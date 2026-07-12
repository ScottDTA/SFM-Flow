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
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Command component executing math modifications onto targeted Redstone Emitter
 * blocks.
 */
public class RedstoneEmitterComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable {

	public enum RedstoneOp implements StringRepresentable {
		FIXED("fixed"), ADD("add"), SUBTRACT("subtract");

		private final String name;

		RedstoneOp(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		public int apply(int current, int modifier, boolean enableRollover) {
			return switch (this) {
			case FIXED -> Math.max(0, Math.min(15, modifier));
			case ADD -> {
				int target = current + modifier;
				if (enableRollover) {
					yield target & 15;
				}
				yield Math.min(15, target);
			}
			case SUBTRACT -> {
				int target = current - modifier;
				if (enableRollover) {
					yield target & 15;
				}
				yield Math.max(0, target);
			}
			};
		}
	}

	public static final Codec<RedstoneOp> OP_CODEC = StringRepresentable.fromEnum(RedstoneOp::values);

	public static final MapCodec<RedstoneEmitterComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(RedstoneEmitterComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(RedstoneEmitterComponent::getInventoryId),
					Codec.INT.optionalFieldOf("activeSidesMask", 0)
							.forGetter(RedstoneEmitterComponent::getActiveSidesMask),
					Codec.INT.listOf().optionalFieldOf("values", List.of(0, 0, 0, 0, 0, 0))
							.forGetter(RedstoneEmitterComponent::getValuesList),
					OP_CODEC.listOf()
							.optionalFieldOf("operators",
									List.of(RedstoneOp.FIXED, RedstoneOp.FIXED, RedstoneOp.FIXED, RedstoneOp.FIXED,
											RedstoneOp.FIXED, RedstoneOp.FIXED))
							.forGetter(RedstoneEmitterComponent::getOperatorsList),
					Codec.BOOL.listOf().optionalFieldOf("pulses", List.of(false, false, false, false, false, false))
							.forGetter(RedstoneEmitterComponent::getPulsesList),
					Codec.BOOL.listOf().optionalFieldOf("rollovers", List.of(false, false, false, false, false, false))
							.forGetter(RedstoneEmitterComponent::getRolloversList))
			.apply(instance, (baseProps, invId, sidesMask, vals, ops, pls, rolls) -> {
				RedstoneEmitterComponent comp = new RedstoneEmitterComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.activeSidesMask = sidesMask;
				for (int i = 0; i < 6; i++) {
					if (i < vals.size())
						comp.values[i] = vals.get(i);
					if (i < ops.size())
						comp.operators[i] = ops.get(i);
					if (i < pls.size())
						comp.pulses[i] = pls.get(i);
					if (i < rolls.size())
						comp.rollovers[i] = rolls.get(i);
				}
				return comp;
			}));

	private int inventoryId = -1;
	private int activeSidesMask = 0;

	private final int[] values = new int[6];
	private final RedstoneOp[] operators = { RedstoneOp.FIXED, RedstoneOp.FIXED, RedstoneOp.FIXED, RedstoneOp.FIXED,
			RedstoneOp.FIXED, RedstoneOp.FIXED };
	private final boolean[] pulses = new boolean[6];
	private final boolean[] rollovers = new boolean[6];

	public RedstoneEmitterComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 1;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.REDSTONE_EMITTER.get();
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

	public int getValue(Direction side) {
		return values[side.ordinal()];
	}

	public void setValue(Direction side, int val) {
		this.values[side.ordinal()] = val;
	}

	public RedstoneOp getOperator(Direction side) {
		return operators[side.ordinal()];
	}

	public void setOperator(Direction side, RedstoneOp op) {
		this.operators[side.ordinal()] = op;
	}

	public boolean isPulse(Direction side) {
		return pulses[side.ordinal()];
	}

	public void setPulse(Direction side, boolean pulse) {
		this.pulses[side.ordinal()] = pulse;
	}

	public boolean isRollover(Direction side) {
		return rollovers[side.ordinal()];
	}

	public void setRollover(Direction side, boolean rollover) {
		this.rollovers[side.ordinal()] = rollover;
	}

	public List<Integer> getValuesList() {
		List<Integer> list = new ArrayList<>();
		for (int v : values)
			list.add(v);
		return list;
	}

	public List<RedstoneOp> getOperatorsList() {
		return List.of(operators);
	}

	public List<Boolean> getPulsesList() {
		List<Boolean> list = new ArrayList<>();
		for (boolean b : pulses)
			list.add(b);
		return list;
	}

	public List<Boolean> getRolloversList() {
		List<Boolean> list = new ArrayList<>();
		for (boolean b : rollovers)
			list.add(b);
		return list;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		var inventories = context.getConnectedInventories();
		ConnectionBlock tgtBlock = null;

		for (var block : inventories) {
			if (block.getId() == this.inventoryId && !block.isSleeping()) {
				tgtBlock = block;
				break;
			}
		}

		if (tgtBlock != null) {
			BlockPos destPos = tgtBlock.getBlockPos();
			for (Direction dir : Direction.values()) {
				if (isSideActive(dir)) {
					int idx = dir.ordinal();
					context.tryWriteTask(ResourceLocation.fromNamespaceAndPath("sfmflow", "redstone_emitter"),
							context.getSnapshot().getCapturedInventories().get(0).getBlockPos(), 0, null, destPos, idx,
							dir, new RedstoneEmitterParams(this.activeSidesMask, operators[idx], values[idx],
									pulses[idx], rollovers[idx]));
				}
			}
		}

		// Propagate upstream down the output execution pin [3]
		for (var conn : context.getConnections()) {
			if (conn.getSourceComponentId().equals(this.getId())) {
				context.enqueue(conn.getTargetComponentId());
			}
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);

		ListTag valList = new ListTag();
		for (int val : values)
			valList.add(IntTag.valueOf(val));
		compoundTag.put("values", valList);

		ListTag opsList = new ListTag();
		for (RedstoneOp op : operators)
			opsList.add(StringTag.valueOf(op.name()));
		compoundTag.put("operators", opsList);

		ListTag plsList = new ListTag();
		for (boolean b : pulses)
			plsList.add(ByteTag.valueOf(b));
		compoundTag.put("pulses", plsList);

		ListTag rollsList = new ListTag();
		for (boolean b : rollovers)
			rollsList.add(ByteTag.valueOf(b));
		compoundTag.put("rollovers", rollsList);

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		RedstoneEmitterComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse redstone emitter component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.activeSidesMask = decoded.activeSidesMask;
					for (int i = 0; i < 6; i++) {
						this.values[i] = decoded.values[i];
						this.operators[i] = decoded.operators[i];
						this.pulses[i] = decoded.pulses[i];
						this.rollovers[i] = decoded.rollovers[i];
					}
				});

		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
		if (compoundTag.contains("values")) {
			ListTag list = compoundTag.getList("values", Tag.TAG_INT);
			for (int i = 0; i < 6; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag num) {
					this.values[i] = num.getAsInt();
				}
			}
		}
		if (compoundTag.contains("operators")) {
			ListTag list = compoundTag.getList("operators", Tag.TAG_STRING);
			for (int i = 0; i < 6; i++) {
				if (i < list.size()) {
					try {
						this.operators[i] = RedstoneOp.valueOf(list.getString(i).toUpperCase(Locale.ROOT));
					} catch (IllegalArgumentException ignored) {
					}
				}
			}
		}
		if (compoundTag.contains("pulses")) {
			ListTag list = compoundTag.getList("pulses", Tag.TAG_BYTE);
			for (int i = 0; i < 6; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag num) {
					this.pulses[i] = num.getAsByte() != 0;
				}
			}
		}
		if (compoundTag.contains("rollovers")) {
			ListTag list = compoundTag.getList("rollovers", Tag.TAG_BYTE);
			for (int i = 0; i < 6; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag num) {
					this.rollovers[i] = num.getAsByte() != 0;
				}
			}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.redstone_emitter");
	}

	public record RedstoneEmitterParams(int activeSidesMask, RedstoneOp operator, int modifierValue, boolean isPulse,
			boolean rolloverEnabled) {
	}
}