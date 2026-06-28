package dta.sfmflow.kernel;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.List;
import java.util.UUID;
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

	/**
	 * Lazily retrieves or instantiates a fresh planning executor thread pool if the
	 * previous one is terminated [3].
	 *
	 * @return active ExecutorService instance [3]
	 */
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
	 * Submits a new planning task to execute asynchronously on the background
	 * daemon thread [3]. Enforces a 1ms execution budget limit for the evaluation
	 * slice to protect CPU performance [3].
	 *
	 * @param manager        the managing block entity [3]
	 * @param snapshot       deep-copied snapshot of connected inventories [3]
	 * @param activeTriggers list of UUIDs representing elapsed triggers [3]
	 */
	public static void submitTask(ManagerBlockEntity manager, ThreadSafeInventorySnapshot snapshot,
			List<UUID> activeTriggers) {
		getExecutor().submit(() -> {
			try {
				FlowchartPlanningTask task = new FlowchartPlanningTask(manager, snapshot, activeTriggers);
				// 1ms budget = 1,000,000 nanoseconds per cycle [3]
				long budgetNs = 1_000_000L;
				boolean done = false;
				while (!done) {
					done = task.evaluateSlice(budgetNs);
					if (!done) {
						// Cooperative yield: wait 1ms before executing the next slice [3]
						Thread.sleep(1);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				SFMFlow.LOGGER.error("Exception occurred inside background flowchart planning executor", e);
			}
		});
	}

	/**
	 * Cleans up and shuts down the executor service cleanly on server shutdown [3].
	 */
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
			executor = null; // Reset reference so a fresh executor can instantiate on reload [3]
		}
	}

	/**
	 * Hooks a cleanup handler to the Game event bus on server stopping [3].
	 *
	 * @param event server stopping lifecycle event [3]
	 */
	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		shutdown();
	}
}