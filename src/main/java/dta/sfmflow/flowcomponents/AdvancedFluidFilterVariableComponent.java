package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFlowchartVariable;
import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Variable component that stores a single fluid filter along with an optional
 * quantity limit in mB [3]. Configured with 0 inputs and 0 outputs [3].
 */
public class AdvancedFluidFilterVariableComponent extends AbstractFlowComponent
		implements IGhostSlotAware, IFlowchartVariable {

	public static final MapCodec<AdvancedFluidFilterVariableComponent> CODEC = RecordCodecBuilder
			.mapCodec(instance -> instance
					.group(BaseProperties.CODEC.fieldOf("base")
							.forGetter(AdvancedFluidFilterVariableComponent::getBaseProperties),
							FluidStack.CODEC.optionalFieldOf("filterFluid", FluidStack.EMPTY)
									.forGetter(AdvancedFluidFilterVariableComponent::getFilterFluid),
							Codec.BOOL.optionalFieldOf("useQuantity", false)
									.forGetter(AdvancedFluidFilterVariableComponent::isUseQuantity),
							Codec.INT.optionalFieldOf("quantity", 1000)
									.forGetter(AdvancedFluidFilterVariableComponent::getQuantity),
							Color.CODEC.optionalFieldOf("filterColor", Color.WHITE)
									.forGetter(AdvancedFluidFilterVariableComponent::getFilterColor),
							Codec.BOOL.optionalFieldOf("useModId", false)
									.forGetter(AdvancedFluidFilterVariableComponent::isUseModId),
							Codec.BOOL.optionalFieldOf("useTag", false)
									.forGetter(AdvancedFluidFilterVariableComponent::isUseTag),
							Codec.STRING.optionalFieldOf("selectedTag", "")
									.forGetter(AdvancedFluidFilterVariableComponent::getSelectedTag),
							Codec.BOOL.optionalFieldOf("useComponentFilter", false)
									.forGetter(AdvancedFluidFilterVariableComponent::isUseComponentFilter),
							Codec.STRING.listOf().optionalFieldOf("enabledComponentTypes", List.of())
									.forGetter(AdvancedFluidFilterVariableComponent::getEnabledComponentTypes),
							CompoundTag.CODEC.optionalFieldOf("customComponentSettings", new CompoundTag())
									.forGetter(AdvancedFluidFilterVariableComponent::getCustomComponentSettings))
					.apply(instance, (baseProps, filterFluid, useQuantity, quantity, filterColor, useModId, useTag,
							selectedTag, useComponentFilter, enabledComponentTypes, customComponentSettings) -> {
						AdvancedFluidFilterVariableComponent comp = new AdvancedFluidFilterVariableComponent(
								baseProps.id());
						comp.setBaseProperties(baseProps);
						comp.filterFluid = filterFluid;
						comp.useQuantity = useQuantity;
						comp.quantity = quantity;
						comp.filterColor = filterColor;
						comp.useModId = useModId;
						comp.useTag = useTag;
						comp.selectedTag = selectedTag;
						comp.useComponentFilter = useComponentFilter;
						comp.enabledComponentTypes.clear();
						comp.enabledComponentTypes.addAll(enabledComponentTypes);
						comp.customComponentSettings = customComponentSettings;
						return comp;
					}));

	private FluidStack filterFluid = FluidStack.EMPTY;
	private boolean useQuantity = false;
	private int quantity = 1000;
	private Color filterColor = Color.WHITE;
	private boolean useModId = false;
	private boolean useTag = false;
	private String selectedTag = "";
	private boolean useComponentFilter = false;
	private final List<String> enabledComponentTypes = new ArrayList<>();

	private CompoundTag customComponentSettings = new CompoundTag();

	public AdvancedFluidFilterVariableComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.hasOutputNodes = false;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.ADVANCED_FLUID_FILTER_VARIABLE.get();
	}

	public FluidStack getFilterFluid() {
		return filterFluid;
	}

	public void setFilterFluid(FluidStack stack) {
		this.filterFluid = stack == null ? FluidStack.EMPTY : stack;
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

	public CompoundTag getCustomComponentSettings() {
		return customComponentSettings;
	}

	/**
	 * Matches an in-transit fluid stack based on the dynamic card configurations
	 * [3]. Performs deep value-based equality checking on enabled data components
	 * [3].
	 */
	public static boolean matchesVariableFilter(AdvancedFluidFilterVariableComponent varComp, FluidStack candidate) {
		FluidStack filterFluid = varComp.getFilterFluid();
		if (filterFluid.isEmpty() || candidate.isEmpty()) {
			return false;
		}

		boolean matchModId = true;
		if (varComp.isUseModId()) {
			String filterMod = BuiltInRegistries.FLUID.getKey(filterFluid.getFluid()).getNamespace();
			String candidateMod = BuiltInRegistries.FLUID.getKey(candidate.getFluid()).getNamespace();
			matchModId = filterMod.equals(candidateMod);
		}

		boolean matchTag = true;
		if (varComp.isUseTag() && !varComp.getSelectedTag().isEmpty()) {
			matchTag = false;
			ResourceLocation tagLoc = ResourceLocation.tryParse(varComp.getSelectedTag());
			if (tagLoc != null) {
				TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagLoc);
				matchTag = candidate.is(tagKey);
			}
		} else if (!varComp.isUseModId() && !varComp.isUseComponentFilter()) {
			return FluidStack.isSameFluid(candidate, filterFluid);
		}

		if (varComp.isUseComponentFilter()) {
			for (String typeStr : varComp.getEnabledComponentTypes()) {
				ResourceLocation loc = ResourceLocation.tryParse(typeStr);
				if (loc != null) {
					var typeOpt = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(loc);
					if (typeOpt.isPresent()) {
						var type = typeOpt.get();
						boolean filterHas = filterFluid.has(type);
						boolean candidateHas = candidate.has(type);
						if (filterHas != candidateHas) {
							return false; // Type mismatch [3]
						}
						if (filterHas) {
							Object filterVal = filterFluid.get(type);
							Object candidateVal = candidate.get(type);
							if (!Objects.equals(filterVal, candidateVal)) {
								return false; // Values differ [3]
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

		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(this.getFilterColor().getHexColor(), true));

		if (this.useComponentFilter) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}

		CompoundTag tag = new CompoundTag();
		tag.putUUID("VariableId", this.getId());
		tag.putBoolean("UseQuantity", this.useQuantity);
		tag.putInt("Quantity", this.quantity);
		tag.putString("FilterColor", this.filterColor.name());
		tag.putBoolean("UseModId", this.useModId);
		tag.putBoolean("UseTag", this.useTag);
		tag.putString("SelectedTag", this.selectedTag);
		tag.putBoolean("UseComponentFilter", this.useComponentFilter);
		tag.put("CustomComponentSettings", this.customComponentSettings);

		// Serialize the active fluid so the custom BEWLR item renderer can find it [3]
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		if (!this.filterFluid.isEmpty()) {
			tag.put("FilterFluid", this.filterFluid.save(registries));
		}

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
	public ItemStack getGhostStack(int index) {
		if (index == 0 && !filterFluid.isEmpty()) {
			Item bucket = filterFluid.getFluid().getBucket();
			if (bucket != null && bucket != Items.AIR) {
				return new ItemStack(bucket);
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setGhostStack(int index, ItemStack stack) {
		if (index == 0) {
			this.setFilterFluid(FluidTransferPlanner.getFluidFromItem(stack));
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

		if (!this.filterFluid.isEmpty()) {
			compoundTag.put("filterFluid", this.filterFluid.save(registries));
		}
		compoundTag.putBoolean("useQuantity", this.useQuantity);
		compoundTag.putInt("quantity", this.quantity);
		compoundTag.putString("filterColor", this.filterColor.getSerializedName());
		compoundTag.putBoolean("useModId", this.useModId);
		compoundTag.putBoolean("useTag", this.useTag);
		compoundTag.putString("selectedTag", this.selectedTag);
		compoundTag.putBoolean("useComponentFilter", this.useComponentFilter);
		compoundTag.put("customComponentSettings", this.customComponentSettings);

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

		AdvancedFluidFilterVariableComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse advanced fluid variable: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.setFilterFluid(decoded.getFilterFluid());
					this.setUseQuantity(decoded.isUseQuantity());
					this.setQuantity(decoded.getQuantity());
					this.setFilterColor(decoded.getFilterColor());
					this.setUseModId(decoded.isUseModId());
					this.setUseTag(decoded.isUseTag());
					this.setSelectedTag(decoded.getSelectedTag());
					this.setUseComponentFilter(decoded.isUseComponentFilter());
					this.enabledComponentTypes.clear();
					this.enabledComponentTypes.addAll(decoded.getEnabledComponentTypes());
					this.customComponentSettings = decoded.getCustomComponentSettings();
				});

		if (compoundTag.contains("filterFluid")) {
			this.filterFluid = FluidStack.parse(registries, compoundTag.getCompound("filterFluid"))
					.orElse(FluidStack.EMPTY);
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
		}
		if (compoundTag.contains("useTag")) {
			this.useTag = compoundTag.getBoolean("useTag");
		}
		if (compoundTag.contains("selectedTag")) {
			this.selectedTag = compoundTag.getString("selectedTag");
		}
		if (compoundTag.contains("useComponentFilter")) {
			this.useComponentFilter = compoundTag.getBoolean("useComponentFilter");
		}
		if (compoundTag.contains("customComponentSettings")) {
			this.customComponentSettings = compoundTag.getCompound("customComponentSettings");
		}
		if (compoundTag.contains("enabledComponentTypes")) {
			ListTag list = compoundTag.getList("enabledComponentTypes", Tag.TAG_STRING);
			this.enabledComponentTypes.clear();
			for (int i = 0; i < list.size(); i++) {
				this.enabledComponentTypes.add(list.getString(i));
			}
		}
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable("gui.sfmflow.advanced_fluid_filter_variable");
	}

	@Override
	public boolean isFilterEmpty() {
		return this.filterFluid.isEmpty();
	}

	@Override
	public String getFilteredContentName() {
		return this.filterFluid.getHoverName().getString();
	}

}