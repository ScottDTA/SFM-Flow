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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
									.forGetter(AdvancedItemFilterVariableComponent::getSelectedTag),
							Codec.BOOL.optionalFieldOf("useComponentFilter", false)
									.forGetter(AdvancedItemFilterVariableComponent::isUseComponentFilter),
							Codec.STRING.listOf().optionalFieldOf("enabledComponentTypes", List.of())
									.forGetter(AdvancedItemFilterVariableComponent::getEnabledComponentTypes))
					.apply(instance, (baseProps, filterStack, useQuantity, quantity, filterColor, useModId, useTag,
							selectedTag, useComponentFilter, enabledComponentTypes) -> {
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
						comp.useComponentFilter = useComponentFilter;
						comp.enabledComponentTypes.clear();
						comp.enabledComponentTypes.addAll(enabledComponentTypes);
						return comp;
					}));

	private ItemStack filterStack = ItemStack.EMPTY;
	private boolean useQuantity = false;
	private int quantity = 1;
	private Color filterColor = Color.WHITE;
	private boolean useModId = false;
	private boolean useTag = false;
	private String selectedTag = "";
	private boolean useComponentFilter = false; // Enabled data component matching [3]
	private final List<String> enabledComponentTypes = new ArrayList<>(); // Enabled data component registry IDs [3]

	public AdvancedItemFilterVariableComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.hasOutputNodes = false;
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

	public boolean isUseComponentFilter() {
		return useComponentFilter;
	}

	public void setUseComponentFilter(boolean useComponentFilter) {
		this.useComponentFilter = useComponentFilter;
	}

	public List<String> getEnabledComponentTypes() {
		return enabledComponentTypes;
	}

	/**
	 * Matches an item stack based on the dynamic card configurations [3]. Performs
	 * deep value-based equality checking on enabled data components [3].
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
				TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
				matchTag = candidate.is(tagKey);
			}
		} else if (!varComp.isUseModId() && !varComp.isUseComponentFilter()) {
			return ItemStack.isSameItem(candidate, filterItem);
		}

		// Perform deep matching on checked/active data components [3]
		if (varComp.isUseComponentFilter()) {
			for (String typeStr : varComp.getEnabledComponentTypes()) {
				ResourceLocation loc = ResourceLocation.tryParse(typeStr);
				if (loc != null) {
					var typeOpt = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(loc);
					if (typeOpt.isPresent()) {
						var type = typeOpt.get();
						boolean filterHas = filterItem.has(type);
						boolean candidateHas = candidate.has(type);
						if (filterHas != candidateHas) {
							return false; // Type mismatch
						}
						if (filterHas) {
							Object filterVal = filterItem.get(type);
							Object candidateVal = candidate.get(type);
							if (!Objects.equals(filterVal, candidateVal)) {
								return false; // Values differ
							}
						}
					}
				}
			}
		}

		return matchModId && matchTag;
	}

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
		tag.putBoolean("UseComponentFilter", this.useComponentFilter);

		ListTag typesList = new ListTag();
		for (String type : this.enabledComponentTypes) {
			typesList.add(StringTag.valueOf(type));
		}
		tag.put("EnabledComponentTypes", typesList);

		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_NAME, this.getName());
		return stack;
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
		compoundTag.putString("filterColor", this.filterColor.getSerializedName());
		compoundTag.putBoolean("useModId", this.useModId);
		compoundTag.putBoolean("useTag", this.useTag);
		compoundTag.putString("selectedTag", this.selectedTag);
		compoundTag.putBoolean("useComponentFilter", this.useComponentFilter);

		ListTag typesList = new ListTag();
		for (String type : this.enabledComponentTypes) {
			typesList.add(StringTag.valueOf(type));
		}
		compoundTag.put("enabledComponentTypes", typesList);
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
					this.setUseComponentFilter(decoded.isUseComponentFilter());
					this.enabledComponentTypes.clear();
					this.enabledComponentTypes.addAll(decoded.getEnabledComponentTypes());
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
		if (compoundTag.contains("useComponentFilter")) {
			this.useComponentFilter = compoundTag.getBoolean("useComponentFilter");
		} else if (compoundTag.contains("UseComponentFilter")) {
			this.useComponentFilter = compoundTag.getBoolean("UseComponentFilter");
		}
		if (compoundTag.contains("enabledComponentTypes") || compoundTag.contains("EnabledComponentTypes")) {
			ListTag list = compoundTag.contains("enabledComponentTypes")
					? compoundTag.getList("enabledComponentTypes", Tag.TAG_STRING)
					: compoundTag.getList("EnabledComponentTypes", Tag.TAG_STRING);
			this.enabledComponentTypes.clear();
			for (int i = 0; i < list.size(); i++) {
				this.enabledComponentTypes.add(list.getString(i));
			}
		}
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
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable("gui.sfmflow.advanced_item_filter_variable");
	}
}