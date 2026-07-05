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
import dta.sfmflow.registry.ModDataComponents;
import dta.sfmflow.util.Color;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.Locale;
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
									.forGetter(AdvancedItemFilterVariableComponent::getQuantity),
							Color.CODEC.optionalFieldOf("filterColor", Color.WHITE)
									.forGetter(AdvancedItemFilterVariableComponent::getFilterColor),
							Codec.BOOL.optionalFieldOf("useModId", false)
									.forGetter(AdvancedItemFilterVariableComponent::isUseModId),
							Codec.BOOL.optionalFieldOf("useTag", false)
									.forGetter(AdvancedItemFilterVariableComponent::isUseTag),
							Codec.STRING.optionalFieldOf("selectedTag", "")
									.forGetter(AdvancedItemFilterVariableComponent::getSelectedTag))
					.apply(instance, (baseProps, filterStack, useQuantity, quantity, filterColor, useModId, useTag,
							selectedTag) -> {
						AdvancedItemFilterVariableComponent comp = new AdvancedItemFilterVariableComponent(
								baseProps.id());
						comp.setBaseProperties(baseProps);
						comp.filterStack = filterStack;
						comp.useQuantity = useQuantity;
						comp.quantity = quantity;
						comp.filterColor = filterColor;
						comp.useModId = useModId;
						comp.useTag = useTag;
						comp.selectedTag = selectedTag;
						return comp;
					}));

	private ItemStack filterStack = ItemStack.EMPTY;
	private boolean useQuantity = false;
	private int quantity = 1;
	private Color filterColor = Color.WHITE; // Independent card frame dye color [3]
	private boolean useModId = false; // Filter using namespace/modid instead of item [3]
	private boolean useTag = false; // Filter using a registered tag [3]
	private String selectedTag = ""; // The active tag key string selected [3]

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

	public Color getFilterColor() {
		return filterColor;
	}

	public void setFilterColor(Color filterColor) {
		this.filterColor = filterColor == null ? Color.WHITE : filterColor;
	}

	public boolean isUseModId() {
		return useModId;
	}

	public void setUseModId(boolean useModId) {
		this.useModId = useModId;
	}

	public boolean isUseTag() {
		return useTag;
	}

	public void setUseTag(boolean useTag) {
		this.useTag = useTag;
	}

	public String getSelectedTag() {
		return selectedTag;
	}

	public void setSelectedTag(String selectedTag) {
		this.selectedTag = selectedTag == null ? "" : selectedTag;
	}

	/**
	 * Matches an item stack based on the dynamic card configurations [3].
	 */
	public static boolean matchesVariableFilter(AdvancedItemFilterVariableComponent varComp, ItemStack candidate) {
		ItemStack filterItem = varComp.getFilterStack();
		if (filterItem.isEmpty() || candidate.isEmpty()) {
			return false;
		}

		boolean matchModId = true;
		if (varComp.isUseModId()) {
			String filterMod = BuiltInRegistries.ITEM.getKey(filterItem.getItem()).getNamespace();
			String candidateMod = BuiltInRegistries.ITEM.getKey(candidate.getItem()).getNamespace();
			matchModId = filterMod.equals(candidateMod);
		}

		boolean matchTag = true;
		if (varComp.isUseTag() && !varComp.getSelectedTag().isEmpty()) {
			matchTag = false;
			ResourceLocation tagLoc = ResourceLocation.tryParse(varComp.getSelectedTag());
			if (tagLoc != null) {
				// Fix: Correctly pass the Registries.ITEM resource key to TagKey.create [3]
				TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
				matchTag = candidate.is(tagKey);
			}
		} else if (!varComp.isUseModId()) {
			return ItemStack.isSameItem(candidate, filterItem);
		}

		return matchModId && matchTag;
	}

	/**
	 * Bundles this variable's runtime identity into an ItemStack using standard
	 * Custom Data components [3].
	 */
	public ItemStack toItemStack() {
		ItemStack stack = new ItemStack(ModItems.VARIABLE_CARD.get());

		if (!this.filterStack.isEmpty()) {
			stack.set(ModDataComponents.FILTERED_ITEM.get(),
					new ModDataComponents.FilteredItemComponent(this.filterStack.copyWithCount(1)));
		}

		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(this.getFilterColor().getHexColor(), true));

		CompoundTag tag = new CompoundTag();
		tag.putUUID("VariableId", this.getId());
		tag.putBoolean("UseQuantity", this.useQuantity);
		tag.putInt("Quantity", this.quantity);
		tag.putString("FilterColor", this.filterColor.name());
		tag.putBoolean("UseModId", this.useModId);
		tag.putBoolean("UseTag", this.useTag);
		tag.putString("SelectedTag", this.selectedTag);

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

		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

		if (!this.filterStack.isEmpty()) {
			compoundTag.put("filterStack", this.filterStack.save(registries));
		}
		compoundTag.putBoolean("useQuantity", this.useQuantity);
		compoundTag.putInt("quantity", this.quantity);
		compoundTag.putString("filterColor", this.filterColor.getSerializedName()); // Fixed lowercase serialized names
																					// [3]
		compoundTag.putBoolean("useModId", this.useModId);
		compoundTag.putBoolean("useTag", this.useTag);
		compoundTag.putString("selectedTag", this.selectedTag);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		AdvancedItemFilterVariableComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse advanced item variable: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.setFilterStack(decoded.getFilterStack());
					this.setUseQuantity(decoded.isUseQuantity());
					this.setQuantity(decoded.getQuantity());
					this.setFilterColor(decoded.getFilterColor());
					this.setUseModId(decoded.isUseModId());
					this.setUseTag(decoded.isUseTag());
					this.setSelectedTag(decoded.getSelectedTag());
				});

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
		if (compoundTag.contains("filterColor")) {
			// Symmetrical Fix: converted to uppercase to parse existing capitalization
			// formats smoothly [3]
			String nameVal = compoundTag.getString("filterColor").toUpperCase(Locale.ROOT);
			try {
				this.filterColor = Color.valueOf(nameVal);
			} catch (IllegalArgumentException e) {
				this.filterColor = Color.WHITE;
			}
		}
		if (compoundTag.contains("useModId")) {
			this.useModId = compoundTag.getBoolean("useModId");
		} else if (compoundTag.contains("UseModId")) {
			this.useModId = compoundTag.getBoolean("UseModId");
		}
		if (compoundTag.contains("useTag")) {
			this.useTag = compoundTag.getBoolean("useTag");
		} else if (compoundTag.contains("UseTag")) {
			this.useTag = compoundTag.getBoolean("UseTag");
		}
		if (compoundTag.contains("selectedTag")) {
			this.selectedTag = compoundTag.getString("selectedTag");
		} else if (compoundTag.contains("SelectedTag")) {
			this.selectedTag = compoundTag.getString("SelectedTag");
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