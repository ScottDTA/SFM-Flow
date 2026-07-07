package dta.sfmflow.kernel;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.api.flowchart.Flowchart;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Background execution coordinator managing non-blocking asynchronous planning
 * tasks [3]. Limits workers to daemon threads and safely manages thread pools
 * across world reloads [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public final class FlowExecutionKernel {
	private static ExecutorService executor = null;

	private FlowExecutionKernel() {
	}

	private static synchronized ExecutorService getExecutor() {
		if (executor == null || executor.isShutdown() || executor.isTerminated()) {
			executor = Executors.newSingleThreadExecutor(runnable -> {
				Thread thread = new Thread(runnable, "SFM-Flow Execution Worker");
				thread.setDaemon(true); // Daemon state prevents thread hangs on server shutdown [3]
				return thread;
			});
		}
		return executor;
	}

	/**
	 * Submits a new planning task to execute asynchronously on the background daemon thread [3].
	 *
	 * @param executionBuffer the ring buffer to write physical tasks to [3]
	 * @param onBreakerTrip   thread-safe callback running on traversal breaker trips [3]
	 * @param snapshot        deep-copied snapshot of connected inventories [3]
	 * @param flowchartNbt    cloned NBT tag holding isolated component properties [3]
	 * @param registries      registry provider context required for deserialization [3]
	 * @param activeTriggers  active triggers list [3]
	 */
	public static void submitTask(
			ExecutionRingBuffer executionBuffer,
			Runnable onBreakerTrip,
			ThreadSafeInventorySnapshot snapshot,
			Tag flowchartNbt,
			HolderLookup.Provider registries,
			List<UUID> activeTriggers
	) {
		getExecutor().submit(() -> {
			try {
				// 1. Offload the CPU-heavy Codec parsing from the main server tick loop [3]
				var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
				Flowchart flowchart = Flowchart.CODEC.parse(ops, flowchartNbt)
						.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode cloned flowchart: {}", err))
						.orElseGet(() -> new Flowchart(new HashMap<>(), new ArrayList<>()));

				// 2. Setup the planning task using the fully isolated, thread-safe flowchart representation [3]
				FlowchartPlanningTask task = new FlowchartPlanningTask(
						executionBuffer,
						onBreakerTrip,
						snapshot,
						flowchart,
						activeTriggers
				);

				long budgetNs = 1_000_000L; // 1ms budget [3]
				boolean done = false;
				while (!done) {
					done = task.evaluateSlice(budgetNs);
					if (!done) {
						Thread.sleep(1); // Cooperative yield [3]
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				SFMFlow.LOGGER.error("Exception occurred inside background flowchart planning executor", e);
			}
		});
	}

	public static synchronized void shutdown() {
		if (executor != null) {
			SFMFlow.LOGGER.info("[SFM-Flow] Shutting down asynchronous planning executor daemon thread...");
			executor.shutdown();
			try {
				if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
			executor = null;
		}
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		shutdown();
	}
}