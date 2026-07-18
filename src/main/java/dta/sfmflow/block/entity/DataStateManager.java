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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * High-performance data manager handling synchronous, compressed disk saving of
 * flowchart layout configurations and variable states.
 */
public final class DataStateManager {

	private DataStateManager() {
	}

	/**
	 * Synchronously encodes and saves the active flowchart properties to a
	 * compressed file.
	 *
	 * @param server     the active MinecraftServer context 
	 * @param managerId  the unique ID of the target manager 
	 * @param flowchart  the current flowchart data layout 
	 * @param groupVars  the active inventory group variable list 
	 * @param filterVars the active item filter variable list 
	 * @param registries the level registry access provider 
	 */
	public static void saveSync(MinecraftServer server, UUID managerId, Flowchart flowchart,
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

			Flowchart.CODEC.encodeStart(ops, flowchart)
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode flowchart: {}", err))
					.ifPresent(nbt -> dataTag.put("flowchart", nbt));

			InventoryGroupVariable.CODEC.listOf().encodeStart(ops, groupVars)
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode group variables: {}", err))
					.ifPresent(nbt -> dataTag.put("GroupVariables", nbt));

			ItemFilterVariable.CODEC.listOf().encodeStart(ops, filterVars)
					.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode filter variables: {}", err))
					.ifPresent(nbt -> dataTag.put("FilterVariables", nbt));

			Files.createDirectories(filePath.getParent());
			try (OutputStream out = Files.newOutputStream(filePath)) {
				NbtIo.writeCompressed(dataTag, out);
			}
		} catch (Exception e) {
			SFMFlow.LOGGER.error("Failed to synchronously compress flowchart data for manager: {}", managerId, e);
		}
	}

	/**
	 * Synchronously loads compressed flowchart and variables data on block
	 * initialization.
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
						InventoryGroupVariable.CODEC.listOf().parse(ops, dataTag.get("GroupVariables"))
								.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode group variables: {}", err))
								.ifPresent(groupVars::addAll);
					}

					List<ItemFilterVariable> filterVars = new ArrayList<>();
					if (dataTag.contains("FilterVariables")) {
						ItemFilterVariable.CODEC.listOf().parse(ops, dataTag.get("FilterVariables"))
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

	public record LoadedData(Flowchart flowchart, List<InventoryGroupVariable> groupVariables,
			List<ItemFilterVariable> filterVariables) {
		public static LoadedData empty() {
			return new LoadedData(new Flowchart(new HashMap<>(), new ArrayList<>()), new ArrayList<>(),
					new ArrayList<>());
		}
	}
}