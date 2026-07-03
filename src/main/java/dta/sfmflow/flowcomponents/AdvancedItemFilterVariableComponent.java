package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/**
 * Variable component that stores a single item filter along with an optional
 * quantity limit [3]. Configured with 0 inputs and 0 outputs [3].
 */
public class AdvancedItemFilterVariableComponent extends AbstractFlowComponent implements IGhostSlotAware {

	public static final MapCodec<AdvancedItemFilterVariableComponent> CODEC = RecordCodecBuilder
			.mapCodec(instance -> instance
					.group(BaseProperties.CODEC.fieldOf("base")
							.forGetter(AdvancedItemFilterVariableComponent::getBaseProperties),
							ItemStack.OPTIONAL_CODEC.optionalFieldOf("filterStack", ItemStack.EMPTY)
									.forGetter(AdvancedItemFilterVariableComponent::getFilterStack),
							Codec.BOOL.optionalFieldOf("useQuantity", false)
									.forGetter(AdvancedItemFilterVariableComponent::isUseQuantity),
							Codec.INT.optionalFieldOf("quantity", 1)
									.forGetter(AdvancedItemFilterVariableComponent::getQuantity))
					.apply(instance, (baseProps, filterStack, useQuantity, quantity) -> {
						AdvancedItemFilterVariableComponent comp = new AdvancedItemFilterVariableComponent(
								baseProps.id());
						comp.setBaseProperties(baseProps);
						comp.filterStack = filterStack;
						comp.useQuantity = useQuantity;
						comp.quantity = quantity;
						return comp;
					}));

	private ItemStack filterStack = ItemStack.EMPTY;
	private boolean useQuantity = false;
	private int quantity = 1;

	public AdvancedItemFilterVariableComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false; // 0 inputs
		this.hasOutputNodes = false; // 0 outputs
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.ADVANCED_ITEM_FILTER_VARIABLE.get();
	}

	public ItemStack getFilterStack() {
		return filterStack;
	}

	public void setFilterStack(ItemStack stack) {
		this.filterStack = stack == null ? ItemStack.EMPTY : stack;
	}

	public boolean isUseQuantity() {
		return useQuantity;
	}

	public void setUseQuantity(boolean useQuantity) {
		this.useQuantity = useQuantity;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	/**
	 * Bundles this variable's runtime identity into an ItemStack using standard
	 * Custom Data components [3].
	 */
	public ItemStack toItemStack() {
		ItemStack stack = new ItemStack(ModItems.VARIABLE_CARD.get());
		CompoundTag tag = new CompoundTag();
		tag.putUUID("VariableId", this.getId());

		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_NAME, this.getName());
		return stack;
	}

	@Override
	public ItemStack getGhostStack(int index) {
		return index == 0 ? this.filterStack : ItemStack.EMPTY;
	}

	@Override
	public void setGhostStack(int index, ItemStack stack) {
		if (index == 0) {
			this.setFilterStack(stack);
		}
	}

	@Override
	public int getGhostSlotCount() {
		return 1;
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);

		// Dynamic setup: obtain the static composite HolderLookup.Provider cleanly [3]
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

		// Direct NBT saving fallback to guarantee flawless client-server
		// synchronization [3]
		if (!this.filterStack.isEmpty()) {
			compoundTag.put("filterStack", this.filterStack.save(registries));
		}
		compoundTag.putBoolean("useQuantity", this.useQuantity);
		compoundTag.putInt("quantity", this.quantity);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		// Secure setup: obtain the static composite HolderLookup.Provider cleanly [3]
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		// Attempt standard codec parsing first
		AdvancedItemFilterVariableComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse advanced item variable: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.setFilterStack(decoded.getFilterStack());
					this.setUseQuantity(decoded.isUseQuantity());
					this.setQuantity(decoded.getQuantity());
				});

		// Direct NBT loading fallback as an exploit and desync shield [3]
		if (compoundTag.contains("filterStack")) {
			this.filterStack = ItemStack.parse(registries, compoundTag.getCompound("filterStack"))
					.orElse(ItemStack.EMPTY);
		}
		if (compoundTag.contains("useQuantity")) {
			this.useQuantity = compoundTag.getBoolean("useQuantity");
		}
		if (compoundTag.contains("quantity")) {
			this.quantity = compoundTag.getInt("quantity");
		}
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable("gui.sfmflow.advanced_item_filter_variable");
	}
}
