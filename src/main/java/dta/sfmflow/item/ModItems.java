package dta.sfmflow.item;

import dta.sfmflow.SFMFlow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems
 {
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SFMFlow.MODID);

  //public static final DeferredItem<Item> TEST_ITEM = ITEMS.register("test_item",
  //		  															() -> new Item(new Item.Properties()));
  
  public static void register(IEventBus eventBus)
   {
	ITEMS.register(eventBus);
   }
  
 }