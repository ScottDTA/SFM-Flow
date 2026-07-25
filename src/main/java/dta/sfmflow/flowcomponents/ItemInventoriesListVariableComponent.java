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
import dta.sfmflow.api.component.ISlotConfigurable;
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
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
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

public class ItemInventoriesListVariableComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable, ISlotConfigurable, IFlowchartVariable {

	public record ItemInventoriesListEntry(int inventoryId, int activeSidesMask, List<Long> enabledSlotsMasks) {
		public static final Codec<ItemInventoriesListEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("inventoryId").forGetter(ItemInventoriesListEntry::inventoryId),
				Codec.INT.fieldOf("activeSidesMask").forGetter(ItemInventoriesListEntry::activeSidesMask),
				Codec.LONG.listOf().optionalFieldOf("enabledSlotsMasks", List.of(-1L, -1L, -1L, -1L, -1L, -1L))
						.forGetter(ItemInventoriesListEntry::enabledSlotsMasks)
		).apply(instance, ItemInventoriesListEntry::new));
	}

	public static final MapCodec<ItemInventoriesListVariableComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(ItemInventoriesListVariableComponent::getBaseProperties),
					ItemInventoriesListEntry.CODEC.listOf().optionalFieldOf("entries", List.of())
							.forGetter(ItemInventoriesListVariableComponent::getEntries),
					Color.CODEC.optionalFieldOf("filterColor", Color.WHITE)
							.forGetter(ItemInventoriesListVariableComponent::getFilterColor))
			.apply(instance, (baseProps, entries, col) -> {
				ItemInventoriesListVariableComponent comp = new ItemInventoriesListVariableComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.entries.clear();
				comp.entries.addAll(entries);
				comp.filterColor = col;
				return comp;
			}));

	private final List<ItemInventoriesListEntry> entries = new ArrayList<>();
	private Color filterColor = Color.WHITE;

	private transient int selectedInventoryId = -1;
	private transient int selectedActiveSidesMask = 0;
	private transient final List<Long> selectedEnabledSlotsMasks = new ArrayList<>(List.of(-1L, -1L, -1L, -1L, -1L, -1L));

	public ItemInventoriesListVariableComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.hasOutputNodes = false;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.ITEM_INVENTORIES_LIST_VARIABLE.get();
	}

	public List<ItemInventoriesListEntry> getEntries() {
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
		for (ItemInventoriesListEntry entry : entries) {
			if (entry.inventoryId() == id) {
				this.selectedActiveSidesMask = entry.activeSidesMask();
				this.selectedEnabledSlotsMasks.clear();
				this.selectedEnabledSlotsMasks.addAll(entry.enabledSlotsMasks());
				found = true;
				break;
			}
		}
		if (!found) {
			this.selectedActiveSidesMask = 0;
			this.selectedEnabledSlotsMasks.clear();
			for (int i = 0; i < 6; i++) {
				this.selectedEnabledSlotsMasks.add(-1L);
			}
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
				setSelectedInList(true); // Re-adds and saves current active sides to the list entry
			}
		}
	}

	@Override
	public boolean isSlotEnabled(Direction dir, int slot) {
		if (dir == null) return true;
		if (slot < 0 || slot >= 64) return true;
		int idx = dir.ordinal();
		if (idx >= 0 && idx < selectedEnabledSlotsMasks.size()) {
			long mask = selectedEnabledSlotsMasks.get(idx);
			return (mask & (1L << slot)) != 0;
		}
		return true;
	}

	@Override
	public void toggleSlot(Direction dir, int slot) {
		if (dir == null) return;
		if (slot < 0 || slot >= 64) return;
		int idx = dir.ordinal();
		while (selectedEnabledSlotsMasks.size() <= idx) {
			selectedEnabledSlotsMasks.add(-1L);
		}
		long mask = selectedEnabledSlotsMasks.get(idx);
		mask ^= (1L << slot);
		selectedEnabledSlotsMasks.set(idx, mask);

		if (isSelectedInList()) {
			setSelectedInList(true); // Re-adds and saves current slot configurations to the list entry
		}
	}

	public boolean isSelectedInList() {
		for (ItemInventoriesListEntry entry : entries) {
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
			entries.add(new ItemInventoriesListEntry(selectedInventoryId, selectedActiveSidesMask, new ArrayList<>(selectedEnabledSlotsMasks)));
		}
	}

	@Override
	public ItemStack toItemStack() {
		ItemStack stack = new ItemStack(ModItems.VARIABLE_CARD.get());
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(this.getFilterColor().getHexColor(), true));

		CompoundTag tag = new CompoundTag();
		tag.putUUID("VariableId", this.getId());
		tag.putString("FilterColor", this.filterColor.name());

		ListTag entriesList = new ListTag();
		for (var entry : this.entries) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putInt("inventoryId", entry.inventoryId());
			entryTag.putInt("activeSidesMask", entry.activeSidesMask());
			
			ListTag maskList = new ListTag();
			for (long mask : entry.enabledSlotsMasks()) {
				maskList.add(LongTag.valueOf(mask));
			}
			entryTag.put("enabledSlotsMasks", maskList);
			entriesList.add(entryTag);
		}
		tag.put("entries", entriesList);

		// Resolve and save block items representing the configured targets for client-side rendering
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
		return "Item Inventories List";
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putString("filterColor", this.filterColor.getSerializedName());

		ListTag list = new ListTag();
		for (ItemInventoriesListEntry entry : entries) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putInt("inventoryId", entry.inventoryId());
			entryTag.putInt("activeSidesMask", entry.activeSidesMask());
			
			ListTag maskList = new ListTag();
			for (long mask : entry.enabledSlotsMasks()) {
				maskList.add(LongTag.valueOf(mask));
			}
			entryTag.put("enabledSlotsMasks", maskList);
			list.add(entryTag);
		}
		compoundTag.put("entries", list);

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		ItemInventoriesListVariableComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse Item Inventories List component data: {}", err))
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
				List<Long> masks = new ArrayList<>();
				if (entryTag.contains("enabledSlotsMasks")) {
					ListTag masksTag = entryTag.getList("enabledSlotsMasks", Tag.TAG_LONG);
					for (int k = 0; k < masksTag.size(); k++) {
						if (masksTag.get(k) instanceof NumericTag num) {
							masks.add(num.getAsLong());
						}
					}
				}
				while (masks.size() < 6) {
					masks.add(-1L);
				}
				this.entries.add(new ItemInventoriesListEntry(entryTag.getInt("inventoryId"), entryTag.getInt("activeSidesMask"), masks));
			}
		}
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable("gui.sfmflow.item_inventories_list_variable");
	}
	
	@Override
	public boolean isInventoryBound(int id) {
		if (id == -1) {
			return false;
		}
		for (ItemInventoriesListEntry entry : entries) {
			if (entry.inventoryId() == id) {
				return true;
			}
		}
		return false;
	}
}