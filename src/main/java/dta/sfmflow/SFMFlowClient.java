package dta.sfmflow;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.client.plugin.SFMFlowClientPluginRegistry;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.render.HighlightManager;
import dta.sfmflow.client.render.VariableCardRenderer;
import dta.sfmflow.client.screen.CableClusterScreen;
import dta.sfmflow.client.screen.helper.SlotLayoutManager;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.item.VariableCardItem;
import dta.sfmflow.screen.ModMenuTypes;
import dta.sfmflow.util.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client-only event subscriber and bootstrappery driver [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID, value = Dist.CLIENT)
public class SFMFlowClient {
	public SFMFlowClient(ModContainer container) {
		// Empty constructor - configuration factory hook is safely mapped inside main constructor [3]
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
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			if (tintIndex == 0) {
				UUID varId = VariableCardRenderer.getVariableId(stack);
				if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
						// Pull the live independent card filterColor rather than canvas background mask [3]
						return 0xFF000000 | advancedVar.getFilterColor().getHexColor();
					}
				}

				DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
				return dyedColor != null ? (0xFF000000 | dyedColor.rgb()) : 0xFFFFFFFF;
			}
			return -1;
		}, ModItems.VARIABLE_CARD.get());
	}

	// Register our hidden flat model using 1.21's standalone variant structural layouts [3]
	@SubscribeEvent
	public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
		event.register(ModelResourceLocation
				.standalone(ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item/variable_card_flat")));
	}

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		NeoForge.EVENT_BUS.register(HighlightManager.class);

		event.enqueueWork(() -> {
			/* STREAMING_CHUNK:Invoking client plugin registrations */
			// Sweeps and triggers visual layout setup across all client plugins safely [3]
			SFMFlowClientPluginRegistry.initAllClientProperties();

			// Inject the client-only tooltip resolver safely to avoid dedicated server crashes [3]
			VariableCardItem.setTooltipDataResolver(stack -> {
				UUID varId = VariableCardRenderer.getVariableId(stack);
				if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
						return new Object[] { advancedVar.getFilterStack(), advancedVar.isUseQuantity(),
								advancedVar.getQuantity(), advancedVar.getFilterColor() };
					}
				}
				return null;
			});

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
					// Cleaned up: Legacy expanded inline settings widget factory delegation removed [3]
				});
			}
		});
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		HighlightManager.registerKeyMappings(event);
	}
}