package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.AbstractFilterableConditionalComponent;
import dta.sfmflow.api.component.FlowComponentType;
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
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Conditional logic node that checks the contents of targeted fluid tanks off-thread [3].
 */
public class FluidConditionalComponent extends AbstractFilterableConditionalComponent {

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

	private MatchMode matchMode = MatchMode.MATCH_ANY;
	private ConditionOperator operator = ConditionOperator.GREATER_OR_EQUAL;

	public FluidConditionalComponent(UUID uuid) {
		super(uuid);
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.FLUID_CONDITIONAL.get();
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
	public boolean renderAsFluid() {
		return true; // Decoupled fluid rendering hook [3]
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		boolean isMet = evaluateCondition(context);
		int targetOutputIdx = isMet ? 0 : 1; // Output 0 = True, Output 1 = False [3]

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

		int allowedSlot = targetBlock.getSlotIndex();

		List<FluidStack> inventoryFluids = new ArrayList<>();
		for (Direction side : activeSides) {
			var inv = context.getSnapshot().getFluidInventory(targetPos, side);
			if (inv != null) {
				for (var entry : inv.tanks().entrySet()) {
					int tankIdx = entry.getKey();
					if (allowedSlot != -1 && tankIdx != allowedSlot) {
						continue;
					}
					if (isSlotEnabled(side, tankIdx)) {
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
		compoundTag.putString("matchMode", this.matchMode.getSerializedName()); // Case-safety fix [3]
		compoundTag.putString("operator", this.operator.getSerializedName());   // Case-safety fix [3]
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

		super.loadData(compoundTag); // Load parent class fields [3]

		if (compoundTag.contains("matchMode")) {
			String val = compoundTag.getString("matchMode");
			for (MatchMode m : MatchMode.values()) {
				if (m.name().equalsIgnoreCase(val) || m.getSerializedName().equalsIgnoreCase(val)) {
					this.matchMode = m;
					break;
				}
			}
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
}