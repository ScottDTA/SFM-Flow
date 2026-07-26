package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IFlowchartVariable;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.util.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SignUpdaterInventoriesListVariableComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable, IFlowchartVariable {

	public record SignUpdaterInventoriesListEntry(int inventoryId, int activeSidesMask) {
		public static final Codec<SignUpdaterInventoriesListEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("inventoryId").forGetter(SignUpdaterInventoriesListEntry::inventoryId),
				Codec.INT.fieldOf("activeSidesMask").forGetter(SignUpdaterInventoriesListEntry::activeSidesMask)
		).apply(instance, SignUpdaterInventoriesListEntry::new));
	}

	public static final MapCodec<SignUpdaterInventoriesListVariableComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(SignUpdaterInventoriesListVariableComponent::getBaseProperties),
					SignUpdaterInventoriesListEntry.CODEC.listOf().optionalFieldOf("entries", List.of())
							.forGetter(SignUpdaterInventoriesListVariableComponent::getEntries),
					Color.CODEC.optionalFieldOf("filterColor", Color.WHITE)
							.forGetter(SignUpdaterInventoriesListVariableComponent::getFilterColor))
			.apply(instance, (baseProps, entries, col) -> {
				SignUpdaterInventoriesListVariableComponent comp = new SignUpdaterInventoriesListVariableComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.entries.clear();
				comp.entries.addAll(entries);
				comp.filterColor = col;
				return comp;
			}));

	private final List<SignUpdaterInventoriesListEntry> entries = new ArrayList<>();
	private Color filterColor = Color.WHITE;

	private transient int selectedInventoryId = -1;
	private transient int selectedActiveSidesMask = 0;

	public SignUpdaterInventoriesListVariableComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.hasOutputNodes = false;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.SIGN_UPDATER_INVENTORIES_LIST_VARIABLE.get();
	}

	public List<SignUpdaterInventoriesListEntry> getEntries() {
		return entries;
	}

	public Color getFilterColor() {
		return filterColor;
	}

	public void setFilterColor(Color color) {
		this.filterColor = color == null ? Color.WHITE : color;
	}

	@Override
	public int getInventoryId() {
		return this.selectedInventoryId;
	}

	@Override
	public void setInventoryId(int id) {
		this.selectedInventoryId = id;
		boolean found = false;
		for (SignUpdaterInventoriesListEntry entry : entries) {
			if (entry.inventoryId() == id) {
				this.selectedActiveSidesMask = entry.activeSidesMask();
				found = true;
				break;
			}
		}
		if (!found) {
			this.selectedActiveSidesMask = 0;
		}
	}

	@Override
	public boolean isSideActive(Direction dir) {
		return (selectedActiveSidesMask & (1 << dir.ordinal())) != 0;
	}

	@Override
	public void toggleSide(Direction dir) {
		if (dir != null) {
			selectedActiveSidesMask ^= (1 << dir.ordinal());
			if (isSelectedInList()) {
				setSelectedInList(true);
			}
		}
	}

	public boolean isSelectedInList() {
		for (SignUpdaterInventoriesListEntry entry : entries) {
			if (entry.inventoryId() == selectedInventoryId) {
				return true;
			}
		}
		return false;
	}

	public void setSelectedInList(boolean add) {
		if (selectedInventoryId == -1) return;
		entries.removeIf(e -> e.inventoryId() == selectedInventoryId);
		if (add) {
			entries.add(new SignUpdaterInventoriesListEntry(selectedInventoryId, selectedActiveSidesMask));
		}
	}

	@Override
	public ItemStack toItemStack() {
		ItemStack stack = new ItemStack(ModItems.VARIABLE_CARD.get());
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(this.getFilterColor().getHexColor(), true));

		CompoundTag tag = new CompoundTag();
		tag.putUUID("VariableId", this.getId());
		tag.putString("VariableType", "sfmflow:sign_updater_inventories_list_variable");
		tag.putString("FilterColor", this.filterColor.name());

		ListTag entriesList = new ListTag();
		for (var entry : this.entries) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putInt("inventoryId", entry.inventoryId());
			entryTag.putInt("activeSidesMask", entry.activeSidesMask());
			entriesList.add(entryTag);
		}
		tag.put("entries", entriesList);

		ListTag itemsList = new ListTag();
		Level level = Minecraft.getInstance().level;
		if (level != null && !this.entries.isEmpty() && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
			for (var entry : this.entries) {
				BlockPos targetPos = null;
				for (var block : screen.getMenu().getManagerBlockEntity().getInventories()) {
					if (block.getId() == entry.inventoryId()) {
						targetPos = block.getBlockPos();
						break;
					}
				}
				if (targetPos != null) {
					BlockState state = level.getBlockState(targetPos);
					if (!state.isAir()) {
						ItemStack blockItem = new ItemStack(state.getBlock().asItem());
						if (!blockItem.isEmpty()) {
							itemsList.add(blockItem.save(level.registryAccess()));
						}
					}
				}
			}
		}
		tag.put("InventoriesListItems", itemsList);

		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_NAME, this.getName());
		return stack;
	}

	@Override
	public boolean isFilterEmpty() {
		return this.entries.isEmpty();
	}

	@Override
	public String getFilteredContentName() {
		return "Sign Updater Inventories List";
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putString("filterColor", this.filterColor.getSerializedName());

		ListTag list = new ListTag();
		for (SignUpdaterInventoriesListEntry entry : entries) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putInt("inventoryId", entry.inventoryId());
			entryTag.putInt("activeSidesMask", entry.activeSidesMask());
			list.add(entryTag);
		}
		compoundTag.put("entries", list);

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		SignUpdaterInventoriesListVariableComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse Sign Updater Inventories List component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.entries.clear();
					this.entries.addAll(decoded.getEntries());
					this.filterColor = decoded.getFilterColor();
				});

		super.loadData(compoundTag);

		if (compoundTag.contains("filterColor")) {
			try {
				this.filterColor = Color.valueOf(compoundTag.getString("filterColor").toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {}
		}
		if (compoundTag.contains("entries")) {
			ListTag list = compoundTag.getList("entries", Tag.TAG_COMPOUND);
			this.entries.clear();
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entryTag = list.getCompound(i);
				this.entries.add(new SignUpdaterInventoriesListEntry(entryTag.getInt("inventoryId"), entryTag.getInt("activeSidesMask")));
			}
		}
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable("gui.sfmflow.sign_updater_inventories_list_variable");
	}
	
	@Override
	public boolean isInventoryBound(int id) {
		if (id == -1) {
			return false;
		}
		for (SignUpdaterInventoriesListEntry entry : entries) {
			if (entry.inventoryId() == id) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isInventoryList() {
		return true;
	}
}