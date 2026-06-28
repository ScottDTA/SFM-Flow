package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFilterable;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Unified logic component handling both item inputs (extractions) and item
 * outputs (depositions) [3]. Upgraded to serialize optional group, filter
 * variables, and a bitmask representing active directions [3].
 */
public class ItemTransferComponent extends AbstractFlowComponent implements IFilterable, IInventoryTarget, ISideConfigurable {
	private final boolean isInput;
	private int inventoryId = -1;
	private boolean useAll = true;
	private int targetSlot = -1;

	// Bitmask representing active directions (all 6 active by default: 111111 binary = 63) [3]
	private int activeSidesMask = 63;

	private UUID boundGroupVariableId = null;
	private UUID boundFilterVariableId = null;

	// Symmetrical Filter Variables [3]
	private boolean whitelist = true;
	private final List<ItemStack> filterItems = new java.util.ArrayList<>();

	public static final MapCodec<ItemTransferComponent> INPUT_CODEC = makeCodec(true);
	public static final MapCodec<ItemTransferComponent> OUTPUT_CODEC = makeCodec(false);

	private static MapCodec<ItemTransferComponent> makeCodec(boolean isInput) {
		return RecordCodecBuilder.mapCodec(instance -> instance
				.group(BaseProperties.CODEC.fieldOf("base").forGetter(ItemTransferComponent::getBaseProperties),
						Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(ItemTransferComponent::getInventoryId),
						Codec.BOOL.optionalFieldOf("useAll", true).forGetter(ItemTransferComponent::isUseAll),
						Codec.INT.optionalFieldOf("targetSlot", -1).forGetter(ItemTransferComponent::getTargetSlot),
						Codec.INT.optionalFieldOf("activeSidesMask", 63).forGetter(ItemTransferComponent::getActiveSidesMask),
						UUIDUtil.CODEC.optionalFieldOf("boundGroupVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundGroupVariableId())),
						UUIDUtil.CODEC.optionalFieldOf("boundFilterVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundFilterVariableId())),
						Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(ItemTransferComponent::isWhitelist),
						ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filterItems", List.of())
								.forGetter(ItemTransferComponent::getFilterItems))
				.apply(instance,
						(baseProps, invId, useAllVal, slot, sidesMask, groupVar, filterVar, whitelistVal, filtersList) -> {
							ItemTransferComponent comp = new ItemTransferComponent(baseProps.id(), isInput);
							comp.setBaseProperties(baseProps);
							comp.inventoryId = invId;
							comp.useAll = useAllVal;
							comp.targetSlot = slot;
							comp.activeSidesMask = sidesMask;
							comp.boundGroupVariableId = groupVar.orElse(null);
							comp.boundFilterVariableId = filterVar.orElse(null);
							comp.whitelist = whitelistVal;
							comp.filterItems.clear();
							comp.filterItems.addAll(filtersList);
							while (comp.filterItems.size() < 12) {
								comp.filterItems.add(ItemStack.EMPTY);
							}
							return comp;
						}));
	}

	public ItemTransferComponent(UUID uuid, boolean isInput) {
		super(uuid);
		this.isInput = isInput;
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 1;
		for (int i = 0; i < 12; i++) {
			this.filterItems.add(ItemStack.EMPTY);
		}
	}

	public boolean isInput() {
		return isInput;
	}

	public int getInventoryId() {
		return inventoryId;
	}

	public void setInventoryId(int inventoryId) {
		this.inventoryId = inventoryId;
	}

	public boolean isUseAll() {
		return useAll;
	}

	public void setUseAll(boolean useAll) {
		this.useAll = useAll;
	}

	public int getTargetSlot() {
		return targetSlot;
	}

	public void setTargetSlot(int targetSlot) {
		this.targetSlot = targetSlot;
	}

	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	public void toggleSide(Direction dir) {
		activeSidesMask ^= (1 << dir.ordinal());
	}

	public void setSideActive(Direction dir, boolean active) {
		if (active) {
			activeSidesMask |= (1 << dir.ordinal());
		} else {
			activeSidesMask &= ~(1 << dir.ordinal());
		}
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
	public FlowComponentType getType() {
		return isInput ? FlowComponentType.ITEM_INPUT.get() : FlowComponentType.ITEM_OUTPUT.get();
	}

	@Override
	public void loadData(net.minecraft.nbt.CompoundTag compoundTag) {
		var codec = isInput ? ItemTransferComponent.INPUT_CODEC : ItemTransferComponent.OUTPUT_CODEC;
		codec.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag).resultOrPartial(
				err -> dta.sfmflow.SFMFlow.LOGGER.error("Failed to parse item transfer component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.useAll = decoded.isUseAll();
					this.targetSlot = decoded.getTargetSlot();
					this.activeSidesMask = decoded.getActiveSidesMask();
					this.boundGroupVariableId = decoded.getBoundGroupVariableId();
					this.boundFilterVariableId = decoded.getBoundFilterVariableId();
					this.whitelist = decoded.isWhitelist();
					this.filterItems.clear();
					this.filterItems.addAll(decoded.getFilterItems());
					while (this.filterItems.size() < 12) {
						this.filterItems.add(ItemStack.EMPTY);
					}
				});
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable(isInput ? "gui.sfmflow.item_input" : "gui.sfmflow.item_output");
	}
}