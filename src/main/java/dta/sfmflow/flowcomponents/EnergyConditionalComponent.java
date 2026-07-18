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
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Conditional logic node that checks the Forge Energy (FE) levels of targeted blocks off-thread [3].
 */
public class EnergyConditionalComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable {

	public enum ConditionOperator implements StringRepresentable {
		GREATER_OR_EQUAL("greater_or_equal", ">="),
		LESS_OR_EQUAL("less_or_equal", "<="),
		EQUAL("equal_to", "=="),
		NOT_EQUAL("not_equal", "!="),
		GREATER("greater_than", ">"),
		LESS("less_than", "<");

		private final String name;
		private final String symbol;

		ConditionOperator(String name, String symbol) {
			this.name = name;
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		public boolean compare(int current, int threshold) {
			return switch (this) {
				case GREATER_OR_EQUAL -> current >= threshold;
				case LESS_OR_EQUAL -> current <= threshold;
				case EQUAL -> current == threshold;
				case NOT_EQUAL -> current != threshold;
				case GREATER -> current > threshold;
				case LESS -> current < threshold;
			};
		}
	}

	public static final Codec<ConditionOperator> OPERATOR_CODEC = StringRepresentable.fromEnum(ConditionOperator::values);

	public static final MapCodec<EnergyConditionalComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(EnergyConditionalComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(EnergyConditionalComponent::getInventoryId),
					Codec.INT.optionalFieldOf("activeSidesMask", 0).forGetter(EnergyConditionalComponent::getActiveSidesMask),
					OPERATOR_CODEC.optionalFieldOf("operator", ConditionOperator.GREATER_OR_EQUAL).forGetter(EnergyConditionalComponent::getOperator),
					Codec.INT.optionalFieldOf("threshold", 0).forGetter(EnergyConditionalComponent::getThreshold))
			.apply(instance, (baseProps, invId, sidesMask, op, thresh) -> {
				EnergyConditionalComponent comp = new EnergyConditionalComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.activeSidesMask = sidesMask;
				comp.operator = op;
				comp.threshold = thresh;
				return comp;
			}));

	private int inventoryId = -1;
	private int activeSidesMask = 0;

	private ConditionOperator operator = ConditionOperator.GREATER_OR_EQUAL;
	private int threshold = 0;

	public EnergyConditionalComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 2; // Output 0 = True, Output 1 = False [3]
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.ENERGY_CONDITIONAL.get();
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

	public ConditionOperator getOperator() {
		return operator;
	}

	public void setOperator(ConditionOperator operator) {
		this.operator = operator;
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		boolean isMet = evaluateCondition(context);
		int targetOutputIdx = isMet ? 0 : 1; // True = Output 0, False = Output 1 [3]

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
			activeSides.add(null);
		}

		// Since block energy is typically shared, matching any valid face's storage is sufficient
		for (Direction side : activeSides) {
			var snap = context.getSnapshot().getEnergy(targetPos, side);
			if (snap != null) {
				if (this.operator.compare(snap.energyStored(), this.threshold)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);
		compoundTag.putString("operator", this.operator.getSerializedName()); // FIX [3]
		compoundTag.putInt("threshold", this.threshold);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		EnergyConditionalComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse energy conditional component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.activeSidesMask = decoded.getActiveSidesMask();
					this.operator = decoded.getOperator();
					this.threshold = decoded.getThreshold();
				});

		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
		if (compoundTag.contains("operator")) {
			String val = compoundTag.getString("operator");
			for (ConditionOperator op : ConditionOperator.values()) {
				if (op.name().equalsIgnoreCase(val) || op.getSerializedName().equalsIgnoreCase(val)) {
					this.operator = op;
					break;
				}
			}
		}
		if (compoundTag.contains("threshold")) {
			this.threshold = compoundTag.getInt("threshold");
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.energy_conditional");
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