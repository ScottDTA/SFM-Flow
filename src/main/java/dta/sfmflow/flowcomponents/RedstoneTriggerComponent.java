package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.IRedstoneSidedConfigurable;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.block.entity.RedstoneReceiverBlockEntity;
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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Persists and evaluates custom, side-specific analog threshold conditions across four outputs.
 */
public class RedstoneTriggerComponent extends AbstractTriggerComponent implements IInventoryTarget, ISideConfigurable, IRedstoneSidedConfigurable {

	public enum Operator implements StringRepresentable {
		GREATER_THAN("greater_than", ">"),
		LESS_THAN("less_than", "<"),
		EQUAL_TO("equal_to", "="),
		GREATER_OR_EQUAL("greater_or_equal", ">="),
		LESS_OR_EQUAL("less_or_equal", "<=");

		private final String name;
		private final String symbol;

		Operator(String name, String symbol) {
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
				case GREATER_THAN -> current > threshold;
				case LESS_THAN -> current < threshold;
				case EQUAL_TO -> current == threshold;
				case GREATER_OR_EQUAL -> current >= threshold;
				case LESS_OR_EQUAL -> current <= threshold;
			};
		}
	}

	public static final Codec<Operator> OPERATOR_CODEC = StringRepresentable.fromEnum(Operator::values);

	public static final MapCodec<RedstoneTriggerComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(RedstoneTriggerComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(RedstoneTriggerComponent::getInventoryId),
					Codec.INT.optionalFieldOf("activeSidesMask", 0).forGetter(RedstoneTriggerComponent::getActiveSidesMask),
					Codec.BOOL.optionalFieldOf("requiresAll", false).forGetter(RedstoneTriggerComponent::isRequiresAll),
					Codec.INT.listOf().optionalFieldOf("thresholds", List.of(0, 0, 0, 0, 0, 0)).forGetter(RedstoneTriggerComponent::getThresholdsList),
					OPERATOR_CODEC.listOf().optionalFieldOf("operators", List.of(Operator.GREATER_THAN, Operator.GREATER_THAN, Operator.GREATER_THAN, Operator.GREATER_THAN, Operator.GREATER_THAN, Operator.GREATER_THAN)).forGetter(RedstoneTriggerComponent::getOperatorsList),
					Codec.INT.optionalFieldOf("highIntervalValue", 10).forGetter(RedstoneTriggerComponent::getHighIntervalValue),
					IntervalTriggerComponent.TIME_UNIT_CODEC.optionalFieldOf("highTimeUnit", IntervalTriggerComponent.TimeUnit.TICKS).forGetter(RedstoneTriggerComponent::getHighTimeUnit),
					Codec.INT.optionalFieldOf("lowIntervalValue", 10).forGetter(RedstoneTriggerComponent::getLowIntervalValue),
					IntervalTriggerComponent.TIME_UNIT_CODEC.optionalFieldOf("lowTimeUnit", IntervalTriggerComponent.TimeUnit.TICKS).forGetter(RedstoneTriggerComponent::getLowTimeUnit),
					Codec.INT.listOf().optionalFieldOf("previousSignals", List.of(0, 0, 0, 0, 0, 0)).forGetter(RedstoneTriggerComponent::getPreviousSignalsList),
					Codec.BOOL.listOf().optionalFieldOf("activeOutputs", List.of(false, false, false, false)).forGetter(RedstoneTriggerComponent::getActiveOutputsList))
			.apply(instance, (baseProps, invId, sidesMask, reqAll, threshs, ops, hVal, hUnit, lVal, lUnit, prevList, actList) -> {
				RedstoneTriggerComponent comp = new RedstoneTriggerComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.activeSidesMask = sidesMask;
				comp.requiresAll = reqAll;
				comp.highIntervalValue = hVal;
				comp.highTimeUnit = hUnit;
				comp.lowIntervalValue = lVal;
				comp.lowTimeUnit = lUnit;
				for (int i = 0; i < 6; i++) {
					if (i < threshs.size()) comp.thresholds[i] = threshs.get(i);
					if (i < ops.size()) comp.operators[i] = ops.get(i);
					if (i < prevList.size()) comp.previousSignals[i] = prevList.get(i);
				}
				for (int i = 0; i < 4; i++) {
					if (i < actList.size()) {
						comp.activeOutputs[i] = actList.get(i);
					}
				}
				return comp;
			}));

	private int inventoryId = -1;
	private int activeSidesMask = 0;
	private boolean requiresAll = false;

	// Sided conditional arrays
	private final int[] thresholds = new int[6];
	private final Operator[] operators = {
			Operator.GREATER_THAN, Operator.GREATER_THAN, Operator.GREATER_THAN,
			Operator.GREATER_THAN, Operator.GREATER_THAN, Operator.GREATER_THAN
	};

	// Level Trigger Cooldown intervals
	private int highIntervalValue = 10;
	private IntervalTriggerComponent.TimeUnit highTimeUnit = IntervalTriggerComponent.TimeUnit.TICKS;
	private int lowIntervalValue = 10;
	private IntervalTriggerComponent.TimeUnit lowTimeUnit = IntervalTriggerComponent.TimeUnit.TICKS;

	// Transient level execution timing track
	private transient long lastHighExecutedTick = 0L;
	private transient long lastLowExecutedTick = 0L;

	private final int[] previousSignals = new int[6];
	private final boolean[] activeOutputs = new boolean[4];

	public RedstoneTriggerComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.hasOutputNodes = true;
		this.numOutputs = 4; 
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.REDSTONE_TRIGGER.get();
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

	public Operator getOperator(Direction side) {
		return operators[side.ordinal()];
	}

	public void setOperator(Direction side, Operator op) {
		this.operators[side.ordinal()] = op;
	}

	public List<Integer> getThresholdsList() {
		List<Integer> list = new ArrayList<>();
		for (int val : thresholds) {
			list.add(val);
		}
		return list;
	}

	public List<Operator> getOperatorsList() {
		return List.of(operators);
	}

	public int getHighIntervalValue() {
		return highIntervalValue;
	}

	public void setHighIntervalValue(int val) {
		this.highIntervalValue = val;
	}

	public IntervalTriggerComponent.TimeUnit getHighTimeUnit() {
		return highTimeUnit;
	}

	public void setHighTimeUnit(IntervalTriggerComponent.TimeUnit unit) {
		this.highTimeUnit = unit;
	}

	public int getLowIntervalValue() {
		return lowIntervalValue;
	}

	public void setLowIntervalValue(int val) {
		this.lowIntervalValue = val;
	}

	public IntervalTriggerComponent.TimeUnit getLowTimeUnit() {
		return lowTimeUnit;
	}

	public void setLowTimeUnit(IntervalTriggerComponent.TimeUnit unit) {
		this.lowTimeUnit = unit;
	}

	public int getHighTotalTicks() {
		return highIntervalValue * highTimeUnit.getFactor();
	}

	public int getLowTotalTicks() {
		return lowIntervalValue * lowTimeUnit.getFactor();
	}

	public List<Integer> getPreviousSignalsList() {
		List<Integer> list = new ArrayList<>();
		for (int sig : previousSignals) {
			list.add(sig);
		}
		return list;
	}

	public List<Boolean> getActiveOutputsList() {
		List<Boolean> list = new ArrayList<>();
		for (boolean b : activeOutputs) {
			list.add(b);
		}
		return list;
	}

	@Override
	public boolean evaluateTrigger(Level level, BlockPos pos, long gameTime) {
		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof ManagerBlockEntity manager)) {
			return false;
		}

		ConnectionBlock targetBlock = null;
		for (ConnectionBlock inv : manager.getInventories()) {
			if (inv.getId() == this.inventoryId && !inv.isSleeping()) {
				targetBlock = inv;
				break;
			}
		}

		if (targetBlock == null) {
			return false;
		}

		BlockPos targetPos = targetBlock.getBlockPos();
		BlockEntity targetBe = level.getBlockEntity(targetPos);
		int[] currentSignals = new int[6];
		if (targetBe instanceof RedstoneReceiverBlockEntity receiver) {
			for (Direction dir : Direction.values()) {
				currentSignals[dir.ordinal()] = receiver.getPowerForSide(dir);
			}
		} else {
			for (Direction dir : Direction.values()) {
				currentSignals[dir.ordinal()] = level.getSignal(targetPos, dir);
			}
		}

		List<Direction> checkedSides = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			if (isSideActive(dir)) {
				checkedSides.add(dir);
			}
		}
		if (checkedSides.isEmpty()) {
			Collections.addAll(checkedSides, Direction.values());
		}

		boolean currentMet = isConditionMet(currentSignals, checkedSides);
		boolean previousMet = isConditionMet(previousSignals, checkedSides);

		// Throttling Level-triggered conditions based on individual timing configurations 
		boolean highTriggered = currentMet && (gameTime - lastHighExecutedTick >= getHighTotalTicks() || lastHighExecutedTick == 0L);
		boolean lowTriggered = !currentMet && (gameTime - lastLowExecutedTick >= getLowTotalTicks() || lastLowExecutedTick == 0L);
		boolean highPulse = currentMet && !previousMet;
		boolean lowPulse = !currentMet && previousMet;

		this.activeOutputs[0] = highTriggered;
		this.activeOutputs[1] = lowTriggered;
		this.activeOutputs[2] = highPulse;
		this.activeOutputs[3] = lowPulse;

		if (highTriggered) {
			this.lastHighExecutedTick = gameTime;
		}
		if (lowTriggered) {
			this.lastLowExecutedTick = gameTime;
		}

		System.arraycopy(currentSignals, 0, previousSignals, 0, 6);

		return highTriggered || lowTriggered || highPulse || lowPulse;
	}

	private boolean isConditionMet(int[] signals, List<Direction> checkedSides) {
		if (requiresAll) {
			for (Direction dir : checkedSides) {
				int idx = dir.ordinal();
				if (!operators[idx].compare(signals[idx], thresholds[idx])) {
					return false;
				}
			}
			return true;
		} else {
			for (Direction dir : checkedSides) {
				int idx = dir.ordinal();
				if (operators[idx].compare(signals[idx], thresholds[idx])) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		for (FlowComponentConnections conn : context.getConnections()) {
			if (conn.getSourceComponentId().equals(this.getId())) {
				int outputIdx = conn.getOutputNodeIndex();
				// Selectively enqueue target logic nodes depending on the active evaluated outputs
				if (outputIdx >= 0 && outputIdx < 4 && activeOutputs[outputIdx]) {
					context.enqueue(conn.getTargetComponentId());
				}
			}
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);
		compoundTag.putBoolean("requiresAll", this.requiresAll);
		compoundTag.putInt("highIntervalValue", this.highIntervalValue);
		compoundTag.putString("highTimeUnit", this.highTimeUnit.name());
		compoundTag.putInt("lowIntervalValue", this.lowIntervalValue);
		compoundTag.putString("lowTimeUnit", this.lowTimeUnit.name());

		ListTag threshsList = new ListTag();
		for (int val : thresholds) {
			threshsList.add(IntTag.valueOf(val));
		}
		compoundTag.put("thresholds", threshsList);

		ListTag opsList = new ListTag();
		for (Operator op : operators) {
			opsList.add(StringTag.valueOf(op.getSerializedName())); // FIX: Use getSerializedName()
		}
		compoundTag.put("operators", opsList);

		ListTag prevList = new ListTag();
		for (int sig : previousSignals) {
			prevList.add(IntTag.valueOf(sig));
		}
		compoundTag.put("previousSignals", prevList);

		ListTag actList = new ListTag();
		for (boolean b : activeOutputs) {
			actList.add(net.minecraft.nbt.ByteTag.valueOf(b));
		}
		compoundTag.put("activeOutputs", actList);

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		RedstoneTriggerComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse redstone trigger component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.activeSidesMask = decoded.activeSidesMask;
					this.requiresAll = decoded.isRequiresAll();
					this.highIntervalValue = decoded.getHighIntervalValue();
					this.highTimeUnit = decoded.getHighTimeUnit();
					this.lowIntervalValue = decoded.getLowIntervalValue();
					this.lowTimeUnit = decoded.getLowTimeUnit();
					for (int i = 0; i < 6; i++) {
						this.thresholds[i] = decoded.thresholds[i];
						this.operators[i] = decoded.operators[i];
						this.previousSignals[i] = decoded.previousSignals[i];
					}
					for (int i = 0; i < 4; i++) {
						this.activeOutputs[i] = decoded.activeOutputs[i];
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
		if (compoundTag.contains("highIntervalValue")) {
			this.highIntervalValue = compoundTag.getInt("highIntervalValue");
		}
		if (compoundTag.contains("highTimeUnit")) {
			try {
				this.highTimeUnit = IntervalTriggerComponent.TimeUnit.valueOf(compoundTag.getString("highTimeUnit"));
			} catch (IllegalArgumentException ignored) {}
		}
		if (compoundTag.contains("lowIntervalValue")) {
			this.lowIntervalValue = compoundTag.getInt("lowIntervalValue");
		}
		if (compoundTag.contains("lowTimeUnit")) {
			try {
				this.lowTimeUnit = IntervalTriggerComponent.TimeUnit.valueOf(compoundTag.getString("lowTimeUnit"));
			} catch (IllegalArgumentException ignored) {}
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
					String val = list.getString(i);
					Operator resolved = null;
					for (Operator op : Operator.values()) {
						if (op.name().equalsIgnoreCase(val) || op.getSerializedName().equalsIgnoreCase(val)) {
							resolved = op;
							break;
						}
					}
					if (resolved != null) {
						this.operators[i] = resolved;
					}
				}
			}
		}
		if (compoundTag.contains("previousSignals")) {
			ListTag list = compoundTag.getList("previousSignals", Tag.TAG_INT);
			for (int i = 0; i < 6; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag num) {
					this.previousSignals[i] = num.getAsInt();
				}
			}
		}
		if (compoundTag.contains("activeOutputs")) {
			ListTag list = compoundTag.getList("activeOutputs", Tag.TAG_BYTE);
			for (int i = 0; i < 4; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag num) {
					this.activeOutputs[i] = num.getAsByte() != 0;
				} else {
					this.activeOutputs[i] = false;
				}
			}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.redstone_trigger");
	}
}