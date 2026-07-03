package dta.sfmflow.item;

import dta.sfmflow.SFMFlow;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry manager managing the instantiation and binding of items [3].
 */
public class ModItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SFMFlow.MODID);

	/**
	 * Dynamic variable representation card carrying Custom Data component
	 * parameters [3].
	 */
	public static final DeferredItem<Item> VARIABLE_CARD = ITEMS.register("variable_card",
			() -> new Item(new Item.Properties().stacksTo(1)));

	public static void register(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}
}
