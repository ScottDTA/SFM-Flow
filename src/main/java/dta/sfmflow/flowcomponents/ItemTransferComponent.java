package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import net.minecraft.network.chat.Component;
import java.util.UUID;

/**
 * Unified logic component handling both item inputs (extractions) and item outputs (depositions) [3].
 * Consolidates old redundant class files into a single parameterized MVC data model [3].
 * Structurally safe: holds no imports or dependencies pointing to client-only packages [3].
 */
public class ItemTransferComponent extends AbstractFlowComponent {
    private final boolean isInput;
    private int inventoryId = -1; // -1 represents "All Connected Inventories"
    private boolean useAll = true;
    private int targetSlot = -1;  // -1 represents "Any slot"
    private int itemCount = 64;   // Default stack extraction amount

    /**
     * Re-bound MapCodec handling the deserialization of item input transactions [3].
     */
    public static final MapCodec<ItemTransferComponent> INPUT_CODEC = makeCodec(true);

    /**
     * Re-bound MapCodec handling the deserialization of item output transactions [3].
     */
    public static final MapCodec<ItemTransferComponent> OUTPUT_CODEC = makeCodec(false);

    /**
     * Parameterized codec factory defining distinct serialization profiles based on component direction [3].
     * Hard-locks the isInput state inside DFU apply loops to prevent logical state-clashes [3].
     *
     * @param isInput if true, registers as an input target; otherwise registers as an output target [3]
     * @return the mapped MapCodec [3]
     */
    private static MapCodec<ItemTransferComponent> makeCodec(boolean isInput) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseProperties.CODEC.fieldOf("base").forGetter(ItemTransferComponent::getBaseProperties),
            Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(ItemTransferComponent::getInventoryId),
            Codec.BOOL.optionalFieldOf("useAll", true).forGetter(ItemTransferComponent::isUseAll),
            Codec.INT.optionalFieldOf("targetSlot", -1).forGetter(ItemTransferComponent::getTargetSlot),
            Codec.INT.optionalFieldOf("itemCount", 64).forGetter(ItemTransferComponent::getItemCount)
        ).apply(instance, (baseProps, invId, useAllVal, slot, count) -> {
            ItemTransferComponent comp = new ItemTransferComponent(baseProps.id(), isInput);
            comp.setBaseProperties(baseProps);
            comp.inventoryId = invId;
            comp.useAll = useAllVal;
            comp.targetSlot = slot;
            comp.itemCount = count;
            return comp;
        }));
    }

    /**
     * Symmetrically instantiates an ItemTransferComponent [3].
     * Pre-populates baseline visual extensions and configures single input/output terminals [3].
     *
     * @param uuid unique component identifier [3]
     * @param isInput direction flag indicating whether this is an input node [3]
     */
    public ItemTransferComponent(UUID uuid, boolean isInput) {
        super(uuid);
        this.isInput = isInput;
        this.hasInputNodes = true;  // Symmetric: both input and output variants expose top pins [3]
        this.numInputs = 1;
        this.hasOutputNodes = true; // Symmetric: both variants expose bottom execution pins [3]
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

    @Override
    public FlowComponentType getType() {
        return isInput ? FlowComponentType.ITEM_INPUT.get() : FlowComponentType.ITEM_OUTPUT.get();
    }

    @Override
    public void loadData(net.minecraft.nbt.CompoundTag compoundTag) {
        var codec = isInput ? ItemTransferComponent.INPUT_CODEC : ItemTransferComponent.OUTPUT_CODEC;
        codec.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag)
            .resultOrPartial(err -> dta.sfmflow.SFMFlow.LOGGER.error("Failed to parse item transfer component data: {}", err))
            .ifPresent(decoded -> {
                this.setBaseProperties(decoded.getBaseProperties());
                this.inventoryId = decoded.getInventoryId();
                this.useAll = decoded.isUseAll();
                this.targetSlot = decoded.getTargetSlot();
                this.itemCount = decoded.getItemCount();
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