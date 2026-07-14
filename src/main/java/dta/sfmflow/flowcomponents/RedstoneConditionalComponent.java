package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Conditional logic node that checks Redstone signal levels of targeted blocks off-thread.
 * Supports per-side analog thresholds, logic operations, and ALL/ANY evaluation toggles.
 */
public class RedstoneConditionalComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable {

	public static final MapCodec<RedstoneConditionalComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(RedstoneConditionalComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(RedstoneConditionalComponent::getInventoryId),
					Codec.INT.optionalFieldOf("activeSidesMask", 0).forGetter(RedstoneConditionalComponent::getActiveSidesMask),
					Codec.BOOL.optionalFieldOf("requiresAll", false).forGetter(RedstoneConditionalComponent::isRequiresAll),
					Codec.INT.listOf().optionalFieldOf("thresholds", List.of(0, 0, 0, 0, 0, 0)).forGetter(RedstoneConditionalComponent::getThresholdsList),
					RedstoneTriggerComponent.OPERATOR_CODEC.listOf().optionalFieldOf("operators", List.of(RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN)).forGetter(RedstoneConditionalComponent::getOperatorsList))
			.apply(instance, (baseProps, invId, sidesMask, reqAll, threshs, ops) -> {
				RedstoneConditionalComponent comp = new RedstoneConditionalComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.activeSidesMask = sidesMask;
				comp.requiresAll = reqAll;
				for (int i = 0; i < 6; i++) {
					if (i < threshs.size()) comp.thresholds[i] = threshs.get(i);
					if (i < ops.size()) comp.operators[i] = ops.get(i);
				}
				return comp;
			}));

	private int inventoryId = -1;
	private int activeSidesMask = 0;
	private boolean requiresAll = false;

	private final int[] thresholds = new int[6];
	private final RedstoneTriggerComponent.Operator[] operators = {
			RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN,
			RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN, RedstoneTriggerComponent.Operator.GREATER_THAN
	};

	public RedstoneConditionalComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 2; // Output 0 = True, Output 1 = False
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.REDSTONE_CONDITIONAL.get();
	}

	@Override
	public int getInventoryId() {
		return inventoryId;
	}

	@Override
	public void setInventoryId(int inventoryId) {
		this.inventoryId = inventoryId;
	}

	@Override
	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	@Override
	public void toggleSide(Direction dir) {
		activeSidesMask ^= (1 << dir.ordinal());
	}

	public int getActiveSidesMask() {
		return activeSidesMask;
	}

	public void setActiveSidesMask(int mask) {
		this.activeSidesMask = mask;
	}

	public boolean isRequiresAll() {
		return requiresAll;
	}

	public void setRequiresAll(boolean requiresAll) {
		this.requiresAll = requiresAll;
	}

	public int getThreshold(Direction side) {
		return thresholds[side.ordinal()];
	}

	public void setThreshold(Direction side, int val) {
		this.thresholds[side.ordinal()] = val;
	}

	public RedstoneTriggerComponent.Operator getOperator(Direction side) {
		return operators[side.ordinal()];
	}

	public void setOperator(Direction side, RedstoneTriggerComponent.Operator op) {
		this.operators[side.ordinal()] = op;
	}

	public List<Integer> getThresholdsList() {
		List<Integer> list = new ArrayList<>();
		for (int val : thresholds) {
			list.add(val);
		}
		return list;
	}

	public List<RedstoneTriggerComponent.Operator> getOperatorsList() {
		return List.of(operators);
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		boolean isMet = evaluateCondition(context);
		int targetOutputIdx = isMet ? 0 : 1; // True = Output 0, False = Output 1

		for (FlowComponentConnections conn : context.getConnections()) {
			if (conn.getSourceComponentId().equals(this.getId()) && conn.getOutputNodeIndex() == targetOutputIdx) {
				context.enqueue(conn.getTargetComponentId());
			}
		}
	}

	private boolean evaluateCondition(FlowchartPlanningContext context) {
		List<ConnectionBlock> inventories = context.getConnectedInventories();
		ConnectionBlock targetBlock = null;
		for (ConnectionBlock block : inventories) {
			if (block.getId() == this.inventoryId && !block.isSleeping()) {
				targetBlock = block;
				break;
			}
		}
		if (targetBlock == null) {
			return false;
		}

		BlockPos targetPos = targetBlock.getBlockPos();
		List<Direction> activeSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (isSideActive(dir)) {
				activeSides.add(dir);
			}
		}
		if (activeSides.isEmpty()) {
			Collections.addAll(activeSides, Direction.values());
		}

		ThreadSafeInventorySnapshot.RedstoneSnapshot snap = context.getSnapshot().getRedstone(targetPos);
		if (snap == null) {
			return false;
		}

		if (this.requiresAll) {
			for (Direction side : activeSides) {
				int idx = side.ordinal();
				int signalStrength = snap.power()[idx];
				if (!operators[idx].compare(signalStrength, thresholds[idx])) {
					return false;
				}
			}
			return true;
		} else {
			for (Direction side : activeSides) {
				int idx = side.ordinal();
				int signalStrength = snap.power()[idx];
				if (operators[idx].compare(signalStrength, thresholds[idx])) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);
		compoundTag.putBoolean("requiresAll", this.requiresAll);

		ListTag threshsList = new ListTag();
		for (int val : thresholds) {
			threshsList.add(IntTag.valueOf(val));
		}
		compoundTag.put("thresholds", threshsList);

		ListTag opsList = new ListTag();
		for (RedstoneTriggerComponent.Operator op : operators) {
			opsList.add(StringTag.valueOf(op.name()));
		}
		compoundTag.put("operators", opsList);

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		RedstoneConditionalComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse redstone conditional component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.activeSidesMask = decoded.activeSidesMask;
					this.requiresAll = decoded.isRequiresAll();
					for (int i = 0; i < 6; i++) {
						this.thresholds[i] = decoded.thresholds[i];
						this.operators[i] = decoded.operators[i];
					}
				});

		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
		if (compoundTag.contains("requiresAll")) {
			this.requiresAll = compoundTag.getBoolean("requiresAll");
		}
		if (compoundTag.contains("thresholds")) {
			ListTag list = compoundTag.getList("thresholds", Tag.TAG_INT);
			for (int i = 0; i < 6; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag num) {
					this.thresholds[i] = num.getAsInt();
				}
			}
		}
		if (compoundTag.contains("operators")) {
			ListTag list = compoundTag.getList("operators", Tag.TAG_STRING);
			for (int i = 0; i < 6; i++) {
				if (i < list.size()) {
					try {
						this.operators[i] = RedstoneTriggerComponent.Operator.valueOf(list.getString(i).toUpperCase(Locale.ROOT));
					} catch (IllegalArgumentException ignored) {}
				}
			}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.redstone_conditional");
	}

	@Override
	public Component getInputNodeTooltip(int index) {
		return Component.literal("Execute Input");
	}

	@Override
	public Component getOutputNodeTooltip(int index) {
		return index == 0 ? Component.literal("True Output") : Component.literal("False Output");
	}
}