package dta.sfmflow.screen;

import dta.sfmflow.SFMFlow;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu registration registry managing container types [3].
 */
public class ModMenuTypes
 {
  public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SFMFlow.MODID);
  
  public static final DeferredHolder<MenuType<?>, MenuType<ManagerMenu>> MANAGER_MENU = registerMenuType("manager_menu", ManagerMenu::new);
  
  public static final DeferredHolder<MenuType<?>, MenuType<CableClusterMenu>> CABLE_CLUSTER_MENU = registerMenuType("cable_cluster_menu", CableClusterMenu::new);
  
  private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory)
   {
	return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
   }  
  
  public static void register(IEventBus eventBus)
   {
	MENUS.register(eventBus);  
   }  
 }