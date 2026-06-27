package dta.sfmflow;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.FlowSettingsRegistry;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ISettingsWidgetProvider;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.CableClusterScreen;
import dta.sfmflow.client.screen.widgets.IntervalTriggerSettingsWidget;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.screen.ModMenuTypes;
import dta.sfmflow.screen.CableClusterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

/**
 * Client-only event subscriber and bootstrappery driver [3].
 * Physical dedicated servers entirely ignore this class, ensuring 100% side-safe operations [3].
 */
@Mod(value = SFMFlow.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SFMFlow.MODID, value = Dist.CLIENT)
public class SFMFlowClient
 {
  public SFMFlowClient(ModContainer container)
   {
    container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
   }

  @SubscribeEvent
  public static void registerScreens(RegisterMenuScreensEvent event)
   {
	event.register(ModMenuTypes.MANAGER_MENU.get(), ManagerScreen::new);
	event.register(ModMenuTypes.CABLE_CLUSTER_MENU.get(), CableClusterScreen::new);
   }

  /**
   * Processes client-side mod bootstrapping lifecycle operations [3].
   * Automatically loops through common-safe builders to populate visual client properties side-safely [3].
   *
   * @param event the client-setup mod bus event [3]
   */
  @SubscribeEvent
  public static void clientSetup(FMLClientSetupEvent event)
   {
    event.enqueueWork(() -> {
        FlowSettingsRegistry.register(FlowComponentType.INTERVAL_TRIGGER.get(), (container, component) -> {
            if (component instanceof IntervalTriggerComponent intervalTrigger)
             {
              return new IntervalTriggerSettingsWidget(container, intervalTrigger);
             }
            return null;
        });

        dta.sfmflow.util.Color.setResolver((color, isText) -> {
            if (isText) {
                var specValue = dta.sfmflow.ClientConfig.TEXT_CONFIGS.get(color);
                return specValue != null ? dta.sfmflow.ClientConfig.parseHexColor(specValue.get(), color.getDefaultHexTextColor()) : color.getDefaultHexTextColor();
            } else {
                var specValue = dta.sfmflow.ClientConfig.BG_CONFIGS.get(color);
                return specValue != null ? dta.sfmflow.ClientConfig.parseHexColor(specValue.get(), color.getDefaultHexColor()) : color.getDefaultHexColor();
            }
        });

        for (dta.sfmflow.api.component.FlowComponentBuilder builder : dta.sfmflow.api.component.FlowComponentBuilder.getRegisteredBuilders())
         {
          FlowClientRegistry.register(builder.getHolder().get(), new INodeClientProperties() {
              @Override
              public NodeCategory getCategory()
               {
                return builder.getCategory();
               }

              @Override
              public ResourceLocation getIconTexture()
               {
                return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, builder.getIconPath());
               }

              @Override
              public Component getDisplayName()
               {
                return Component.translatable(builder.getDisplayNameKey());
               }

              @Override
              public Supplier<Boolean> isEnabled()
               {
                return () -> true;
               }

              @Override
              public AbstractFlowWidget createSettingsWidget(dta.sfmflow.client.screen.widgets.FlowWidgetContainer container, dta.sfmflow.api.component.AbstractFlowComponent component)
               {
                ISettingsWidgetProvider provider = FlowSettingsRegistry.getProvider(component.getType());
                return provider != null ? provider.createSettingsWidget(container, component) : null;
               }
          });
         }
    });
   }
 }