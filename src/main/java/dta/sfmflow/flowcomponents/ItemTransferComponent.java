package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * Unified logic component handling both item inputs (extractions) and item
 * outputs (depositions) [3]. Upgraded to serialize optional group and filter
 * variable mappings safely [3].
 */
public class ItemTransferComponent extends AbstractFlowComponent {
	private final boolean isInput;
	private int inventoryId = -1;
	private boolean useAll = true;
	private int targetSlot = -1;
	private int itemCount = 64;

	// Optional bound variables [3]
	private UUID boundGroupVariableId = null;
	private UUID boundFilterVariableId = null;

	public static final MapCodec<ItemTransferComponent> INPUT_CODEC = makeCodec(true);
	public static final MapCodec<ItemTransferComponent> OUTPUT_CODEC = makeCodec(false);

	private static MapCodec<ItemTransferComponent> makeCodec(boolean isInput) {
		return RecordCodecBuilder.mapCodec(instance -> instance
				.group(BaseProperties.CODEC.fieldOf("base").forGetter(ItemTransferComponent::getBaseProperties),
						Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(ItemTransferComponent::getInventoryId),
						Codec.BOOL.optionalFieldOf("useAll", true).forGetter(ItemTransferComponent::isUseAll),
						Codec.INT.optionalFieldOf("targetSlot", -1).forGetter(ItemTransferComponent::getTargetSlot),
						Codec.INT.optionalFieldOf("itemCount", 64).forGetter(ItemTransferComponent::getItemCount),
						UUIDUtil.CODEC.optionalFieldOf("boundGroupVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundGroupVariableId())),
						UUIDUtil.CODEC.optionalFieldOf("boundFilterVariableId")
								.forGetter(comp -> Optional.ofNullable(comp.getBoundFilterVariableId())))
				.apply(instance, (baseProps, invId, useAllVal, slot, count, groupVar, filterVar) -> {
					ItemTransferComponent comp = new ItemTransferComponent(baseProps.id(), isInput);
					comp.setBaseProperties(baseProps);
					comp.inventoryId = invId;
					comp.useAll = useAllVal;
					comp.targetSlot = slot;
					comp.itemCount = count;
					comp.boundGroupVariableId = groupVar.orElse(null);
					comp.boundFilterVariableId = filterVar.orElse(null);
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

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
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
					this.itemCount = decoded.getItemCount();
					this.boundGroupVariableId = decoded.getBoundGroupVariableId();
					this.boundFilterVariableId = decoded.getBoundFilterVariableId();
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