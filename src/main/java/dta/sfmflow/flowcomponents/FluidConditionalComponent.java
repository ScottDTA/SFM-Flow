package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.component.ISlotConfigurable;
import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Conditional logic node that checks the contents of targeted fluid tanks off-thread.
 * Routes execution along either True (Output 0) or False (Output 1) paths.
 */
public class FluidConditionalComponent extends AbstractFlowComponent
		implements IFilterable, IInventoryTarget, ISideConfigurable, IGhostSlotAware, ISlotConfigurable {

	public enum MatchMode implements StringRepresentable {
		MATCH_ALL("match_all"),
		MATCH_ANY("match_any");

		private final String name;

		MatchMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

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

	public static final Codec<MatchMode> MATCH_MODE_CODEC = StringRepresentable.fromEnum(MatchMode::values);
	public static final Codec<ConditionOperator> OPERATOR_CODEC = StringRepresentable.fromEnum(ConditionOperator::values);

	public static final MapCodec<FluidConditionalComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(FluidConditionalComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(FluidConditionalComponent::getInventoryId),
					Codec.INT.optionalFieldOf("activeSidesMask", 0).forGetter(FluidConditionalComponent::getActiveSidesMask),
					Codec.LONG.listOf().optionalFieldOf("enabledSlotsMasks", List.of(-1L, -1L, -1L, -1L, -1L, -1L))
							.forGetter(FluidConditionalComponent::getEnabledSlotsMasks),
					UUIDUtil.CODEC.optionalFieldOf("boundGroupVariableId")
							.forGetter(comp -> Optional.ofNullable(comp.getBoundGroupVariableId())),
					UUIDUtil.CODEC.optionalFieldOf("boundFilterVariableId")
							.forGetter(comp -> Optional.ofNullable(comp.getBoundFilterVariableId())),
					Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(FluidConditionalComponent::isWhitelist),
					ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filterItems", List.of())
							.forGetter(FluidConditionalComponent::getFilterItems),
					Codec.INT.listOf().optionalFieldOf("filterLimits", List.of(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1))
							.forGetter(FluidConditionalComponent::getFilterLimits),
					MATCH_MODE_CODEC.optionalFieldOf("matchMode", MatchMode.MATCH_ANY).forGetter(FluidConditionalComponent::getMatchMode),
					OPERATOR_CODEC.optionalFieldOf("operator", ConditionOperator.GREATER_OR_EQUAL).forGetter(FluidConditionalComponent::getOperator))
			.apply(instance, (baseProps, invId, sidesMask, masksList, groupVar, filterVar, whitelistVal, filtersList, limitsList, mMode, op) -> {
				FluidConditionalComponent comp = new FluidConditionalComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.activeSidesMask = sidesMask;
				comp.enabledSlotsMasks.clear();
				comp.enabledSlotsMasks.addAll(masksList);
				comp.boundGroupVariableId = groupVar.orElse(null);
				comp.boundFilterVariableId = filterVar.orElse(null);
				comp.whitelist = whitelistVal;
				comp.filterItems.clear();
				comp.filterItems.addAll(filtersList);
				while (comp.filterItems.size() < 12) {
					comp.filterItems.add(ItemStack.EMPTY);
				}
				comp.filterLimits.clear();
				comp.filterLimits.addAll(limitsList);
				while (comp.filterLimits.size() < 12) {
					comp.filterLimits.add(-1);
				}
				comp.matchMode = mMode;
				comp.operator = op;
				return comp;
			}));

	private int inventoryId = -1;
	private int activeSidesMask = 0;
	private final List<Long> enabledSlotsMasks = new ArrayList<>(List.of(-1L, -1L, -1L, -1L, -1L, -1L));

	private UUID boundGroupVariableId = null;
	private UUID boundFilterVariableId = null;

	private boolean whitelist = true;
	private final List<ItemStack> filterItems = new ArrayList<>();
	private final List<Integer> filterLimits = new ArrayList<>();

	private MatchMode matchMode = MatchMode.MATCH_ANY;
	private ConditionOperator operator = ConditionOperator.GREATER_OR_EQUAL;

	public FluidConditionalComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 2; // 0 = True, 1 = False
		for (int i = 0; i < 12; i++) {
			this.filterItems.add(ItemStack.EMPTY);
			this.filterLimits.add(-1);
		}
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.FLUID_CONDITIONAL.get();
	}

	public List<Long> getEnabledSlotsMasks() {
		return this.enabledSlotsMasks;
	}

	public long getEnabledSlotsMask(Direction dir) {
		if (dir == null) return -1L;
		int idx = dir.ordinal();
		if (idx >= 0 && idx < enabledSlotsMasks.size()) {
			return enabledSlotsMasks.get(idx);
		}
		return -1L;
	}

	public void setEnabledSlotsMask(Direction dir, long mask) {
		if (dir == null) return;
		int idx = dir.ordinal();
		while (enabledSlotsMasks.size() <= idx) {
			enabledSlotsMasks.add(-1L);
		}
		enabledSlotsMasks.set(idx, mask);
	}

	public boolean isSlotEnabled(Direction dir, int slot) {
		if (dir == null) return true;
		if (slot < 0 || slot >= 64) return true;
		long mask = getEnabledSlotsMask(dir);
		return (mask & (1L << slot)) != 0;
	}

	public void toggleSlot(Direction dir, int slot) {
		if (dir == null) return;
		if (slot < 0 || slot >= 64) return;
		long mask = getEnabledSlotsMask(dir);
		mask ^= (1L << slot);
		setEnabledSlotsMask(dir, mask);
	}

	public int getInventoryId() {
		return inventoryId;
	}

	public void setInventoryId(int inventoryId) {
		this.inventoryId = inventoryId;
	}

	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	public void toggleSide(Direction dir) {
		activeSidesMask ^= (1 << dir.ordinal());
	}

	public int getActiveSidesMask() {
		return activeSidesMask;
	}

	public void setActiveSidesMask(int mask) {
		this.activeSidesMask = mask;
	}

	public @Nullable UUID getBoundGroupVariableId() {
		return boundGroupVariableId;
	}

	public void setBoundGroupVariableId(@Nullable UUID id) {
		this.boundGroupVariableId = id;
	}

	public @Nullable UUID getBoundFilterVariableId() {
		return boundFilterVariableId;
	}

	public void setBoundFilterVariableId(@Nullable UUID id) {
		this.boundFilterVariableId = id;
	}

	public boolean isWhitelist() {
		return whitelist;
	}

	public void setWhitelist(boolean whitelist) {
		this.whitelist = whitelist;
	}

	public List<ItemStack> getFilterItems() {
		return filterItems;
	}

	@Override
	public List<Integer> getFilterLimits() {
		return filterLimits;
	}

	public MatchMode getMatchMode() {
		return matchMode;
	}

	public void setMatchMode(MatchMode matchMode) {
		this.matchMode = matchMode;
	}

	public ConditionOperator getOperator() {
		return operator;
	}

	public void setOperator(ConditionOperator operator) {
		this.operator = operator;
	}

	@Override
	public ItemStack getGhostStack(int index) {
		if (index >= 0 && index < this.filterItems.size()) {
			return this.filterItems.get(index);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setGhostStack(int index, ItemStack stack) {
		if (index >= 0 && index < this.filterItems.size()) {
			this.filterItems.set(index, stack == null ? ItemStack.EMPTY : stack);
		}
	}

	@Override
	public int getGhostSlotCount() {
		return 12;
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
			activeSides.add(null);
		}

		// Pull current fluids safely from the thread-safe snapshot
		List<FluidStack> inventoryFluids = new ArrayList<>();
		for (Direction side : activeSides) {
			var inv = context.getSnapshot().getFluidInventory(targetPos, side);
			if (inv != null) {
				for (var entry : inv.tanks().entrySet()) {
					if (isSlotEnabled(side, entry.getKey())) {
						inventoryFluids.add(entry.getValue().stack());
					}
				}
			}
		}

		boolean hasFilters = false;
		List<FilterRule> rules = new ArrayList<>();

		if (this.boundFilterVariableId != null) {
			AbstractFlowComponent boundComp = context.getComponents().get(this.boundFilterVariableId);
			if (boundComp instanceof AdvancedFluidFilterVariableComponent varComp) {
				if (!varComp.isFilterEmpty()) {
					hasFilters = true;
					int targetQty = varComp.isUseQuantity() ? varComp.getQuantity() : 1000;
					rules.add(new FilterRule(varComp, targetQty));
				}
			}
		} else {
			for (int i = 0; i < this.filterItems.size(); i++) {
				ItemStack filter = this.filterItems.get(i);
				if (filter != null && !filter.isEmpty()) {
					hasFilters = true;
					int targetQty = i < this.filterLimits.size() ? this.filterLimits.get(i) : 1000;
					if (targetQty <= 0) {
						FluidStack filterFluid = FluidTransferPlanner.getFluidFromItem(filter);
						targetQty = !filterFluid.isEmpty() ? filterFluid.getAmount() : 1000;
					}

					// If this filter item is a Variable Card, resolve its actual custom quantity
					if (filter.getItem() == ModItems.VARIABLE_CARD.get()) {
						CompoundTag tag = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
						if (tag.contains("VariableId")) {
							UUID varId = tag.getUUID("VariableId");
							AbstractFlowComponent comp = context.getComponents().get(varId);
							if (comp instanceof AdvancedFluidFilterVariableComponent advancedVar) {
								if (advancedVar.isUseQuantity()) {
									targetQty = advancedVar.getQuantity();
								}
							}
						}
					}

					rules.add(new FilterRule(filter, targetQty));
				}
			}
		}

		// Wildcard Check (Empty Filters / Empty Whitelist)
		if (!hasFilters) {
			int totalCount = 0;
			for (FluidStack stack : inventoryFluids) {
				if (!stack.isEmpty()) {
					totalCount += stack.getAmount();
				}
			}
			int targetQty = (this.filterLimits.size() > 0 && this.filterLimits.get(0) > 0) ? this.filterLimits.get(0) : 1000;
			return this.operator.compare(totalCount, targetQty);
		}

		// Whitelist Evaluation
		if (this.matchMode == MatchMode.MATCH_ALL) {
			for (FilterRule rule : rules) {
				int count = rule.countMatches(context, inventoryFluids);
				if (!this.operator.compare(count, rule.targetQty)) {
					return false;
				}
			}
			return true;
		} else {
			for (FilterRule rule : rules) {
				int count = rule.countMatches(context, inventoryFluids);
				if (this.operator.compare(count, rule.targetQty)) {
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

		ListTag enabledMasksList = new ListTag();
		for (long mask : this.enabledSlotsMasks) {
			enabledMasksList.add(LongTag.valueOf(mask));
		}
		compoundTag.put("enabledSlotsMasks", enabledMasksList);

		if (this.boundGroupVariableId != null) {
			compoundTag.putUUID("boundGroupVariableId", this.boundGroupVariableId);
		}
		if (this.boundFilterVariableId != null) {
			compoundTag.putUUID("boundFilterVariableId", this.boundFilterVariableId);
		}
		compoundTag.putBoolean("whitelist", this.whitelist);

		var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		ListTag itemsList = new ListTag();
		for (ItemStack stack : this.filterItems) {
			if (stack != null && !stack.isEmpty()) {
				itemsList.add(stack.save(registries));
			} else {
				itemsList.add(new CompoundTag());
			}
		}
		compoundTag.put("filterItems", itemsList);

		ListTag limitsList = new ListTag();
		for (int limit : this.filterLimits) {
			limitsList.add(IntTag.valueOf(limit));
		}
		compoundTag.put("filterLimits", limitsList);

		compoundTag.putString("matchMode", this.matchMode.name());
		compoundTag.putString("operator", this.operator.name());

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		FluidConditionalComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse fluid conditional component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.activeSidesMask = decoded.activeSidesMask;
					this.enabledSlotsMasks.clear();
					this.enabledSlotsMasks.addAll(decoded.getEnabledSlotsMasks());
					this.boundGroupVariableId = decoded.getBoundGroupVariableId();
					this.boundFilterVariableId = decoded.getBoundFilterVariableId();
					this.whitelist = decoded.isWhitelist();
					this.filterItems.clear();
					this.filterItems.addAll(decoded.getFilterItems());
					while (this.filterItems.size() < 12) {
						this.filterItems.add(ItemStack.EMPTY);
					}
					this.filterLimits.clear();
					this.filterLimits.addAll(decoded.getFilterLimits());
					while (this.filterLimits.size() < 12) {
						this.filterLimits.add(-1);
					}
					this.matchMode = decoded.getMatchMode();
					this.operator = decoded.getOperator();
				});

		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
		if (compoundTag.contains("enabledSlotsMasks")) {
			ListTag list = compoundTag.getList("enabledSlotsMasks", Tag.TAG_LONG);
			this.enabledSlotsMasks.clear();
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof NumericTag numericTag) {
					this.enabledSlotsMasks.add(numericTag.getAsLong());
				} else {
					this.enabledSlotsMasks.add(-1L);
				}
			}
		}
		if (compoundTag.contains("boundGroupVariableId")) {
			this.boundGroupVariableId = compoundTag.getUUID("boundGroupVariableId");
		} else {
			this.boundGroupVariableId = null;
		}
		if (compoundTag.contains("boundFilterVariableId")) {
			this.boundFilterVariableId = compoundTag.getUUID("boundFilterVariableId");
		} else {
			this.boundFilterVariableId = null;
		}
		if (compoundTag.contains("whitelist")) {
			this.whitelist = compoundTag.getBoolean("whitelist");
		}
		if (compoundTag.contains("filterItems")) {
			ListTag list = compoundTag.getList("filterItems", Tag.TAG_COMPOUND);
			this.filterItems.clear();
			for (int i = 0; i < list.size(); i++) {
				CompoundTag itemTag = list.getCompound(i);
				if (itemTag.isEmpty() || !itemTag.contains("id")) {
					this.filterItems.add(ItemStack.EMPTY);
				} else {
					this.filterItems.add(ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY));
				}
			}
			while (this.filterItems.size() < 12) {
				this.filterItems.add(ItemStack.EMPTY);
			}
		}
		if (compoundTag.contains("filterLimits")) {
			ListTag list = compoundTag.getList("filterLimits", Tag.TAG_INT);
			this.filterLimits.clear();
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof NumericTag numericTag) {
					this.filterLimits.add(numericTag.getAsInt());
				} else {
					this.filterLimits.add(-1);
				}
			}
			while (this.filterLimits.size() < 12) {
				this.filterLimits.add(-1);
			}
		}
		if (compoundTag.contains("matchMode")) {
			try {
				this.matchMode = MatchMode.valueOf(compoundTag.getString("matchMode"));
			} catch (IllegalArgumentException ignored) {}
		}
		if (compoundTag.contains("operator")) {
			try {
				this.operator = ConditionOperator.valueOf(compoundTag.getString("operator"));
			} catch (IllegalArgumentException ignored) {}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.fluid_conditional");
	}

	@Override
	public Component getInputNodeTooltip(int index) {
		return Component.literal("Execute Input");
	}

	@Override
	public Component getOutputNodeTooltip(int index) {
		return index == 0 ? Component.literal("True Output") : Component.literal("False Output");
	}

	private static class FilterRule {
		private final ItemStack filterStack;
		private final AdvancedFluidFilterVariableComponent varComp;
		private final int targetQty;

		public FilterRule(ItemStack filterStack, int targetQty) {
			this.filterStack = filterStack;
			this.varComp = null;
			this.targetQty = targetQty;
		}

		public FilterRule(AdvancedFluidFilterVariableComponent varComp, int targetQty) {
			this.filterStack = ItemStack.EMPTY;
			this.varComp = varComp;
			this.targetQty = targetQty;
		}

		public int countMatches(FlowchartPlanningContext context, List<FluidStack> inventoryFluids) {
			int total = 0;
			for (FluidStack stack : inventoryFluids) {
				if (stack.isEmpty()) continue;
				if (varComp != null) {
					if (AdvancedFluidFilterVariableComponent.matchesVariableFilter(varComp, stack)) {
						total += stack.getAmount();
					}
				} else {
					if (filterStack.getItem() == ModItems.VARIABLE_CARD.get()) {
						CompoundTag tag = filterStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
						if (tag.contains("VariableId")) {
							UUID varId = tag.getUUID("VariableId");
							AbstractFlowComponent comp = context.getComponents().get(varId);
							if (comp instanceof AdvancedFluidFilterVariableComponent advancedVar) {
								if (AdvancedFluidFilterVariableComponent.matchesVariableFilter(advancedVar, stack)) {
									total += stack.getAmount();
								}
							}
						}
					} else {
						FluidStack filterFluid = FluidTransferPlanner.getFluidFromItem(filterStack);
						if (!filterFluid.isEmpty() && FluidStack.isSameFluid(stack, filterFluid)) {
							total += stack.getAmount();
						}
					}
				}
			}
			return total;
		}
	}
	
	@Override
	public boolean renderAsFluid() {
		return true;
	}
}