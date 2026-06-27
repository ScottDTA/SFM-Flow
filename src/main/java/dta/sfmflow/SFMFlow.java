package dta.sfmflow;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.ModBlockEntities;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.item.ModCreativeModeTabs;
import dta.sfmflow.item.ModItems;
import dta.sfmflow.networking.PacketHandlerManager;
import dta.sfmflow.networking.ServerPayloadHandler;
import dta.sfmflow.networking.packets.clientbound.SyncComponentDeltaPacket;
import dta.sfmflow.networking.packets.clientbound.SyncConnectionsPacket;
import dta.sfmflow.networking.packets.serverbound.CanvasActionPacket;
import dta.sfmflow.networking.packets.serverbound.CreateNodePacket;
import dta.sfmflow.networking.packets.serverbound.RemoveConnectionPacket;
import dta.sfmflow.networking.packets.serverbound.ComponentMoved;
import dta.sfmflow.networking.packets.serverbound.CreateConnectionPacket;
import dta.sfmflow.networking.packets.serverbound.SaveComponentSettings;
import dta.sfmflow.networking.packets.serverbound.SyncClusterSlotDirectionPacket;
import dta.sfmflow.screen.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The main mod entry point for SFM-Flow. Handles system initialization,
 * deferred registers, configuration files, and serverbound payload routing [3].
 */
@Mod(SFMFlow.MODID)
public class SFMFlow {
	public static final String MODID = "sfmflow";
	public static final Logger LOGGER = LogUtils.getLogger();

	public SFMFlow(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::registerNetworking);
		ModCreativeModeTabs.register(modEventBus);
		ModItems.register(modEventBus);
		ModBlocks.register(modEventBus);
		FlowComponentType.register(modEventBus);
		ModBlockEntities.register(modEventBus);
		ModMenuTypes.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
		modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "sfmflow-client.toml");
	}

	/**
	 * Registers custom network packet payloads globally for the network protocol
	 * [3]. Routes clientbound payloads safe-side through the PacketHandlerManager
	 * proxy [3].
	 *
	 * @param event the payload registration event [3]
	 */
	public void registerNetworking(RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);

		registrar.playToServer(CanvasActionPacket.TYPE, CanvasActionPacket.STREAM_CODEC,
				(payload, context) -> ServerPayloadHandler.handleCanvasAction(payload, context));
		registrar.playToServer(CreateNodePacket.TYPE, CreateNodePacket.STREAM_CODEC,
				(payload, context) -> ServerPayloadHandler.handleCreateNode(payload, context));
		registrar.playToServer(ComponentMoved.TYPE, ComponentMoved.STREAM_CODEC,
				(payload, context) -> ServerPayloadHandler.handleComponentMoved(payload, context));
		registrar.playToServer(SaveComponentSettings.TYPE, SaveComponentSettings.STREAM_CODEC,
				(payload, context) -> ServerPayloadHandler.handleSaveComponentSettings(payload, context));
		registrar.playToServer(SyncClusterSlotDirectionPacket.TYPE, SyncClusterSlotDirectionPacket.STREAM_CODEC,
				(payload, context) -> ServerPayloadHandler.handleSyncClusterSlotDirection(payload, context));
		registrar.playToServer(CreateConnectionPacket.TYPE, CreateConnectionPacket.STREAM_CODEC,
				(payload, context) -> ServerPayloadHandler.handleCreateConnection(payload, context));
		registrar.playToServer(RemoveConnectionPacket.TYPE, RemoveConnectionPacket.STREAM_CODEC,
				(payload, context) -> ServerPayloadHandler.handleRemoveConnection(payload, context));

		registrar.playToClient(SyncConnectionsPacket.TYPE, SyncConnectionsPacket.STREAM_CODEC,
				PacketHandlerManager::handleSyncConnections);
		registrar.playToClient(SyncComponentDeltaPacket.TYPE, SyncComponentDeltaPacket.STREAM_CODEC,
				PacketHandlerManager::handleSyncComponentDelta);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
	}

}