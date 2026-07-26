package dta.sfmflow.block.entity;

import dta.sfmflow.api.component.IGhostSlotAware;
import dta.sfmflow.api.component.IFlowchartVariable;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.item.VariableCardItem;
import dta.sfmflow.screen.ManagerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API client-server visual capability slot that prevents item placements
 * and extractions. Handles dynamic capability targets using IGhostSlotAware.
 */
public class FilterGhostSlot extends Slot {
	private final ManagerMenu menu;
	private final int filterIndex;

	// Thread-safe high-performance cache to avoid repeated dummy instance allocations
	private static final Map<FlowComponentType, Boolean> IS_INVENTORY_LIST_CACHE = new ConcurrentHashMap<>();

	public FilterGhostSlot(ManagerMenu menu, int filterIndex, int x, int y) {
		super(new SimpleContainer(12), filterIndex, x, y); // Pass filterIndex cleanly
		this.menu = menu;
		this.filterIndex = filterIndex;
	}

	@Override
	public ItemStack getItem() {
		AbstractFlowComponent comp = menu.getActiveComponent();
		if (comp != null && comp.acceptsInventoryListCards()) {
			UUID varId = comp.getBoundInventoryListVariableId();
			if (varId != null) {
				var listComp = menu.getManagerBlockEntity().getFlowComponents().get(varId);
				if (listComp instanceof IFlowchartVariable flowchartVar) {
					return flowchartVar.toItemStack();
				}
			}
			return ItemStack.EMPTY;
		}
		if (comp instanceof IGhostSlotAware aware && filterIndex < aware.getGhostSlotCount()) {
			return aware.getGhostStack(filterIndex);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void set(ItemStack stack) {
		AbstractFlowComponent comp = menu.getActiveComponent();
		if (comp != null && comp.acceptsInventoryListCards()) {
			UUID varId = VariableCardItem.getVariableId(stack);
			comp.setBoundInventoryListVariableId(varId);
			menu.getManagerBlockEntity().setChanged();
			return;
		}
		if (comp instanceof IGhostSlotAware aware && filterIndex < aware.getGhostSlotCount()) {
			if (isInventoryListCard(stack) && !comp.acceptsInventoryListCards()) {
				return;
			}
			aware.setGhostStack(filterIndex, stack);
			menu.getManagerBlockEntity().setChanged();
		}
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public boolean mayPickup(Player playerIn) {
		return false;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/**
	 * Registry-driven validation helper that checks if an item stack represents an inventory list card.
	 * This allows addon developers to easily declare custom lists without modifying the core mod files.
	 */
	public static boolean isInventoryListCard(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(ModItems.VARIABLE_CARD.get())) {
			return false;
		}

		ResourceLocation typeKey = VariableCardItem.getVariableTypeKey(stack);
		if (typeKey != null) {
			FlowComponentType type = FlowComponentType.REGISTRY.get(typeKey);
			if (type != null) {
				return IS_INVENTORY_LIST_CACHE.computeIfAbsent(type, t -> {
					AbstractFlowComponent dummy = t.createComponent(new UUID(0L, 0L));
					return dummy instanceof IFlowchartVariable flowchartVar && flowchartVar.isInventoryList();
				});
			}
		}
		return false;
	}
}