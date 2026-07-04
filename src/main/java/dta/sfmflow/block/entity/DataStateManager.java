package dta.sfmflow.block.entity;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.flowchart.Flowchart;
import dta.sfmflow.api.variable.InventoryGroupVariable;
import dta.sfmflow.api.variable.ItemFilterVariable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * High-performance data manager handling asynchronous, thread-safe serialization 
 * and compressed disk saving of flowchart layout configurations and variable states [3]. 
 * Isolates I/O streams onto a single-threaded background daemon pool [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public final class DataStateManager {

	private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "SFM-Flow I/O Worker");
		thread.setDaemon(true);
		return thread;
	});

	private DataStateManager() {}

	/**
	 * Synchronously encodes active flowchart properties to a CompoundTag on the main 
	 * server thread for concurrency safety, then dispatches the compressed write 
	 * task asynchronously onto the background I/O thread pool [3].
	 *
	 * @param server      the active MinecraftServer context [3]
	 * @param managerId   the unique ID of the target manager block entity [3]
	 * @param flowchart   the current flowchart data layout [3]
	 * @param groupVars   the active inventory group variable list [3]
	 * @param filterVars  the active item filter variable list [3]
	 * @param registries  the level registry access provider [3]
	 */
	public static void saveAsync(MinecraftServer server, UUID managerId, Flowchart flowchart,
			List<InventoryGroupVariable> groupVars, List<ItemFilterVariable> filterVars,
			HolderLookup.Provider registries) {
		if (server == null || managerId == null) {
			return;
		}

		try {
			Path worldDir = server.getWorldPath(LevelResource.ROOT);
			Path filePath = worldDir.resolve("sfmflow").resolve("managers").resolve(managerId + ".dat");

			var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
			CompoundTag dataTag = new CompoundTag();

			// Perform serialization synchronously on the main thread to ensure collection snapshot safety [3]
			Flowchart.CODEC.encodeStart(ops, flowchart)
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode flowchart: {}", err))
					.ifPresent(nbt -> dataTag.put("flowchart", nbt));

			InventoryGroupVariable.CODEC.codec().listOf().encodeStart(ops, groupVars)
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode group variables: {}", err))
					.ifPresent(nbt -> dataTag.put("GroupVariables", nbt));

			ItemFilterVariable.CODEC.codec().listOf().encodeStart(ops, filterVars)
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode filter variables: {}", err))
					.ifPresent(nbt -> dataTag.put("FilterVariables", nbt));

			// Offload the disk compression write asynchronously [3]
			IO_EXECUTOR.submit(() -> {
				try {
					Files.createDirectories(filePath.getParent());
					try (OutputStream out = Files.newOutputStream(filePath)) {
						NbtIo.writeCompressed(dataTag, out);
					}
				} catch (Exception e) {
					SFMFlow.LOGGER.error("Failed to asynchronously compress flowchart data for manager: {}", managerId, e);
				}
			});

		} catch (Exception e) {
			SFMFlow.LOGGER.error("Failed to initiate asynchronous flowchart save for manager: {}", managerId, e);
		}
	}

	/**
	 * Synchronously loads compressed flowchart and variables data on block initialization [3].
	 *
	 * @param server     the active MinecraftServer context [3]
	 * @param managerId  the unique ID of the target manager block entity [3]
	 * @param registries the level registry access provider [3]
	 * @return a LoadedData containing the populated flowchart and variables [3]
	 */
	public static LoadedData loadSync(MinecraftServer server, UUID managerId, HolderLookup.Provider registries) {
		if (server == null || managerId == null) {
			return LoadedData.empty();
		}

		try {
			Path worldDir = server.getWorldPath(LevelResource.ROOT);
			Path filePath = worldDir.resolve("sfmflow").resolve("managers").resolve(managerId + ".dat");

			if (Files.exists(filePath)) {
				CompoundTag dataTag;
				try (InputStream in = Files.newInputStream(filePath)) {
					dataTag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
				}

				if (dataTag != null) {
					var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

					Flowchart flowchart = null;
					if (dataTag.contains("flowchart")) {
						flowchart = Flowchart.CODEC.parse(ops, dataTag.get("flowchart"))
								.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode flowchart map: {}", err))
								.orElse(null);
					}

					List<InventoryGroupVariable> groupVars = new ArrayList<>();
					if (dataTag.contains("GroupVariables")) {
						InventoryGroupVariable.CODEC.codec().listOf().parse(ops, dataTag.get("GroupVariables"))
								.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode group variables: {}", err))
								.ifPresent(groupVars::addAll);
					}

					List<ItemFilterVariable> filterVars = new ArrayList<>();
					if (dataTag.contains("FilterVariables")) {
						ItemFilterVariable.CODEC.codec().listOf().parse(ops, dataTag.get("FilterVariables"))
								.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode filter variables: {}", err))
								.ifPresent(filterVars::addAll);
					}

					if (flowchart == null) {
						flowchart = new Flowchart(new HashMap<>(), new ArrayList<>());
					}

					return new LoadedData(flowchart, groupVars, filterVars);
				}
			}
		} catch (Exception e) {
			SFMFlow.LOGGER.error("Failed to synchronously load flowchart data for manager: {}", managerId, e);
		}

		return LoadedData.empty();
	}

	/**
	 * Deletes the on-disk external layout files upon manager block removal [3].
	 *
	 * @param server    the active MinecraftServer context [3]
	 * @param managerId the unique ID of the target manager block entity [3]
	 */
	public static void deleteSync(MinecraftServer server, UUID managerId) {
		if (server == null || managerId == null) {
			return;
		}
		try {
			Path worldDir = server.getWorldPath(LevelResource.ROOT);
			Path filePath = worldDir.resolve("sfmflow").resolve("managers").resolve(managerId + ".dat");
			if (Files.exists(filePath)) {
				Files.delete(filePath);
			}
		} catch (Exception e) {
			SFMFlow.LOGGER.error("Failed to delete flowchart data file for manager: {}", managerId, e);
		}
	}

	/**
	 * Gracefully shuts down the background I/O pool, blocking up to 5 seconds to flush pending tasks [3].
	 */
	public static void shutdown() {
		SFMFlow.LOGGER.info("[SFM-Flow] Shutting down background flowchart saving thread pool...");
		IO_EXECUTOR.shutdown();
		try {
			if (!IO_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
				IO_EXECUTOR.shutdownNow();
			}
		} catch (InterruptedException e) {
			IO_EXECUTOR.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Listens to the server stopping event to guarantee pending save operations write completely [3].
	 */
	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		shutdown();
	}

	/**
	 * Simple data record housing loaded configurations returned by loadSync [3].
	 */
	public record LoadedData(Flowchart flowchart, List<InventoryGroupVariable> groupVariables,
			List<ItemFilterVariable> filterVariables) {
		public static LoadedData empty() {
			return new LoadedData(new Flowchart(new HashMap<>(), new ArrayList<>()), new ArrayList<>(), new ArrayList<>());
		}
	}
}