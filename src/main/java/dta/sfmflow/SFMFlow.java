package dta.sfmflow;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.ModBlockEntities;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.item.ModCreativeModeTabs;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.registry.ModDataComponents;
import dta.sfmflow.networking.ModNetworking; // Link updated networking [3]
import dta.sfmflow.screen.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * The main mod entry point for SFM-Flow. Handles system initialization,
 * deferred registers, configuration files, and serverbound payload routing.
 */
@Mod(SFMFlow.MODID)
public class SFMFlow {
	public static final String MODID = "sfmflow";
	public static final Logger LOGGER = LogUtils.getLogger();

	public SFMFlow(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(ModNetworking::register);

		ModCreativeModeTabs.register(modEventBus);
		ModItems.register(modEventBus);
		ModDataComponents.register(modEventBus);
		ModBlocks.register(modEventBus);
		FlowComponentType.register(modEventBus);
		ModBlockEntities.register(modEventBus);
		ModMenuTypes.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
		modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "sfmflow-client.toml");

		if (FMLEnvironment.dist.isClient()) {
			dta.sfmflow.SFMFlowClient.initialize(modEventBus, modContainer);
		}
	}
}