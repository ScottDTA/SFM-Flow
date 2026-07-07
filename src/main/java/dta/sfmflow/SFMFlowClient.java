package dta.sfmflow;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.INodeClientProperties;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.render.HighlightManager;
import dta.sfmflow.client.render.VariableCardRenderer;
import dta.sfmflow.client.screen.CableClusterScreen;
import dta.sfmflow.client.screen.helper.SlotLayoutManager;
import dta.sfmflow.flowcomponents.AdvancedFluidFilterVariableComponent;
import dta.sfmflow.flowcomponents.AdvancedItemFilterVariableComponent;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.item.VariableCardItem;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowClientPlugin;
import dta.sfmflow.screen.ModMenuTypes;
import dta.sfmflow.util.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client-only event subscriber and bootstrappery driver [3].
 */
@OnlyIn(Dist.CLIENT)
public class SFMFlowClient {

	private SFMFlowClient() {
	}

	/**
	 * Safely registers all client-side event listeners and extension points on the
	 * physical client [3]. Called explicitly inside the main mod constructor
	 * context [3].
	 *
	 * @param modEventBus  the Mod-scoped event bus [3]
	 * @param modContainer the current mod container [3]
	 */
	public static void initialize(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(SFMFlowClient::registerScreens);
		modEventBus.addListener(SFMFlowClient::registerClientReloadListeners);
		modEventBus.addListener(SFMFlowClient::registerItemColors);
		modEventBus.addListener(SFMFlowClient::registerAdditionalModels);
		modEventBus.addListener(SFMFlowClient::clientSetup);
		modEventBus.addListener(SFMFlowClient::registerKeyMappings);

		modContainer.registerExtensionPoint(IConfigScreenFactory.class,
				(container, parent) -> new ConfigurationScreen(container, parent));
	}

	private static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(ModMenuTypes.MANAGER_MENU.get(), ManagerScreen::new);
		event.register(ModMenuTypes.CABLE_CLUSTER_MENU.get(), CableClusterScreen::new);
	}

	private static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new SlotLayoutManager());
	}

	private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			if (tintIndex == 0) {
				UUID varId = VariableCardRenderer.getVariableId(stack);
				if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
						return 0xFF000000 | advancedVar.getFilterColor().getHexColor();
					}
				}

				DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
				return dyedColor != null ? (0xFF000000 | dyedColor.rgb()) : 0xFFFFFFFF;
			}
			return -1;
		}, ModItems.VARIABLE_CARD.get());
	}

	private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
		event.register(ModelResourceLocation
				.standalone(ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "item/variable_card_flat")));
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		NeoForge.EVENT_BUS.register(HighlightManager.class);

		event.enqueueWork(() -> {
			// Register vanilla client properties directly, completely avoiding static
			// plugin lists [3]
			new VanillaSFMFlowClientPlugin().registerClientProperties();

			VariableCardItem.setTooltipDataResolver(stack -> {
				UUID varId = VariableCardRenderer.getVariableId(stack);
				if (varId != null && Minecraft.getInstance().screen instanceof ManagerScreen screen) {
					var comp = screen.getMenu().getManagerBlockEntity().getFlowComponents().get(varId);
					if (comp instanceof AdvancedItemFilterVariableComponent advancedVar) {
						return new Object[] { advancedVar.getFilterStack(), advancedVar.isUseQuantity(),
								advancedVar.getQuantity(), advancedVar.getFilterColor() };
					}
					// Added tooltip support for fluid variables [3]
					if (comp instanceof AdvancedFluidFilterVariableComponent advancedVar) {
						return new Object[] { advancedVar.getFilterFluid(), advancedVar.isUseQuantity(),
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
						ResourceLocation parsed = ResourceLocation.tryParse(builder.getIconPath());
						if (parsed != null && !parsed.getNamespace().equals("minecraft")) {
							return parsed;
						}
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
				});
			}
		});
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		HighlightManager.registerKeyMappings(event);
	}
}