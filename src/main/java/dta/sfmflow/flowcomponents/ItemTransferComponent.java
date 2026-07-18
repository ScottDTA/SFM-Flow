package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFilterableTransferComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Unified logic component handling both item inputs (extractions) and item outputs (depositions).
 */
public class ItemTransferComponent extends AbstractFilterableTransferComponent {

	public static final MapCodec<ItemTransferComponent> INPUT_CODEC = makeCodec(true);
	public static final MapCodec<ItemTransferComponent> OUTPUT_CODEC = makeCodec(false);

	private static MapCodec<ItemTransferComponent> makeCodec(boolean isInput) {
		return RecordCodecBuilder.mapCodec(instance -> instance
				.group(BaseProperties.CODEC.fieldOf("base").forGetter(ItemTransferComponent::getBaseProperties),
						Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(ItemTransferComponent::getInventoryId),
						Codec.BOOL.optionalFieldOf("useAll", true).forGetter(ItemTransferComponent::isUseAll),
						Codec.INT.optionalFieldOf("targetSlot", -1).forGetter(ItemTransferComponent::getTargetSlot),
						Codec.INT.optionalFieldOf("activeSidesMask", 0)
								.forGetter(ItemTransferComponent::getActiveSidesMask),
						Codec.LONG.listOf().optionalFieldOf("enabledSlotsMasks", List.of(-1L, -1L, -1L, -1L, -1L, -1L))
								.forGetter(ItemTransferComponent::getEnabledSlotsMasks),
						UUIDUtil.CODEC.optionalFieldOf("boundGroupVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundGroupVariableId())),
						UUIDUtil.CODEC.optionalFieldOf("boundFilterVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundFilterVariableId())),
						Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(ItemTransferComponent::isWhitelist),
						ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filterItems", List.of())
								.forGetter(ItemTransferComponent::getFilterItems),
						Codec.INT.listOf()
								.optionalFieldOf("filterLimits",
										List.of(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1))
								.forGetter(ItemTransferComponent::getFilterLimits))
				.apply(instance, (baseProps, invId, useAllVal, slot, sidesMask, masksList, groupVar, filterVar,
						whitelistVal, filtersList, limitsList) -> {
					ItemTransferComponent comp = new ItemTransferComponent(baseProps.id(), isInput);
					comp.setBaseProperties(baseProps);
					comp.inventoryId = invId;
					comp.useAll = useAllVal;
					comp.targetSlot = slot;
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
					return comp;
				}));
	}

	public ItemTransferComponent(UUID uuid, boolean isInput) {
		super(uuid, isInput);
	}

	@Override
	public FlowComponentType getType() {
		return isInput ? VanillaSFMFlowPlugin.ITEM_INPUT.get() : VanillaSFMFlowPlugin.ITEM_OUTPUT.get();
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		if (this.isInput()) {
			ItemTransferPlanner.planInput(context, this);
		} else {
			ItemTransferPlanner.planOutput(context, this);
		}
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		var codec = isInput ? ItemTransferComponent.INPUT_CODEC : ItemTransferComponent.OUTPUT_CODEC;
		codec.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse item transfer component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.useAll = decoded.isUseAll();
					this.targetSlot = decoded.getTargetSlot();
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
				});

		super.loadData(compoundTag);
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable(isInput ? "gui.sfmflow.item_input" : "gui.sfmflow.item_output");
	}
}