package dta.sfmflow;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.plugin.SFMFlowClientPluginRegistry;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.render.HighlightManager;
import dta.sfmflow.client.screen.CableClusterScreen;
import dta.sfmflow.client.screen.helper.SlotLayoutManager;
import dta.sfmflow.screen.ModMenuTypes;
import dta.sfmflow.util.Color;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.NeoForge;
import java.util.function.Supplier;

/**
 * Client-only event subscriber and bootstrappery driver [3].
 */
@Mod(value = SFMFlow.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SFMFlow.MODID, value = Dist.CLIENT)
public class SFMFlowClient {
	public SFMFlowClient(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
	}

	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(ModMenuTypes.MANAGER_MENU.get(), ManagerScreen::new);
		event.register(ModMenuTypes.CABLE_CLUSTER_MENU.get(), CableClusterScreen::new);
	}

	@SubscribeEvent
	public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new SlotLayoutManager());
	}

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		NeoForge.EVENT_BUS.register(HighlightManager.class);

		event.enqueueWork(() -> {
			/* STREAMING_CHUNK:Invoking client plugin registrations */
			// Sweeps and triggers visual layout setup across all client plugins safely [3]
			SFMFlowClientPluginRegistry.initAllClientProperties();

			Color.setResolver((color, isText) -> {
				if (isText) {
					var specValue = ClientConfig.TEXT_CONFIGS.get(color);
					return specValue != null
							? ClientConfig.parseHexColor(specValue.get(), color.getDefaultHexTextColor())
							: color.getDefaultHexTextColor();
				} else {
					var specValue = ClientConfig.BG_CONFIGS.get(color);
					return specValue != null ? ClientConfig.parseHexColor(specValue.get(), color.getDefaultHexColor())
							: color.getDefaultHexColor();
				}
			});

			for (FlowComponentBuilder builder : FlowComponentBuilder.getRegisteredBuilders()) {
				FlowClientRegistry.register(builder.getHolder().get(), new INodeClientProperties() {
					@Override
					public NodeCategory getCategory() {
						return builder.getCategory();
					}

					@Override
					public ResourceLocation getIconTexture() {
						return ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, builder.getIconPath());
					}

					@Override
					public Component getDisplayName() {
						return Component.translatable(builder.getDisplayNameKey());
					}

					@Override
					public Supplier<Boolean> isEnabled() {
						return () -> true;
					}
					// Cleaned up: Legacy expanded inline settings widget factory delegation removed
					// [3]
				});
			}
		});
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		HighlightManager.registerKeyMappings(event);
	}
}