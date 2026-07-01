package dta.sfmflow.api.capability;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import org.jetbrains.annotations.Nullable;

/**
 * Side-safe public registry managing dynamic network capabilities and their
 * task executors [3].
 */
public final class FlowCapabilityRegistry {
	private static final Map<ResourceLocation, FlowCapability<?>> CAPABILITIES = new HashMap<>();
	private static final Map<ResourceLocation, FlowCapabilityTransfer> TRANSFERS = new HashMap<>();

	static {
		// Register default item capability transfer task
		ResourceLocation itemCapId = ResourceLocation.fromNamespaceAndPath("sfmflow", "item");
		register(new FlowCapability<>(itemCapId, Capabilities.ItemHandler.BLOCK, "gui.sfmflow.type_item"));

		registerTransfer(itemCapId, (level, src, srcSide, dest, destSide, params) -> {
			if (params instanceof ItemTransferParams task) {
				var source = level.getCapability(Capabilities.ItemHandler.BLOCK, src, srcSide);
				var target = level.getCapability(Capabilities.ItemHandler.BLOCK, dest, destSide);

				if (source == null) {
					source = level.getCapability(Capabilities.ItemHandler.BLOCK, src, null);
				}
				if (target == null) {
					target = level.getCapability(Capabilities.ItemHandler.BLOCK, dest, null);
				}

				if (source != null && target != null) {
					ItemStack simExtracted = source.extractItem(task.srcSlot(), task.count(), true);
					if (ItemStack.isSameItemSameComponents(simExtracted, task.item())) {
						ItemStack targetRemaining;
						if (task.destSlot() != -1) {
							targetRemaining = target.insertItem(task.destSlot(), simExtracted, true);
						} else {
							targetRemaining = ItemHandlerHelper.insertItemStacked(target, simExtracted, true);
						}

						int realTransferCount = simExtracted.getCount() - targetRemaining.getCount();

						if (realTransferCount > 0) {
							ItemStack realExtracted = source.extractItem(task.srcSlot(), realTransferCount, false);
							if (task.destSlot() != -1) {
								target.insertItem(task.destSlot(), realExtracted, false);
							} else {
								ItemHandlerHelper.insertItemStacked(target, realExtracted, false);
							}
							return true;
						}
					}
				}
			}
			return false;
		});
	}

	private FlowCapabilityRegistry() {
	}

	public static <T> void register(FlowCapability<T> capability) {
		if (capability != null) {
			CAPABILITIES.put(capability.getId(), capability);
		}
	}

	public static void registerTransfer(ResourceLocation id, FlowCapabilityTransfer transfer) {
		if (id != null && transfer != null) {
			TRANSFERS.put(id, transfer);
		}
	}

	@Nullable
	public static FlowCapability<?> get(ResourceLocation id) {
		return CAPABILITIES.get(id);
	}

	@Nullable
	public static FlowCapabilityTransfer getTransfer(ResourceLocation id) {
		return TRANSFERS.get(id);
	}

	public static Map<ResourceLocation, FlowCapability<?>> getRegisteredCapabilities() {
		return CAPABILITIES;
	}
}
