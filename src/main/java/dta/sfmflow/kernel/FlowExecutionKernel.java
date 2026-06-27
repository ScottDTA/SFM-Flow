package dta.sfmflow.kernel;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.concurrent.*;

/**
 * Background execution coordinator managing non-blocking asynchronous planning
 * tasks [3]. Limits workers to daemon threads to guarantee server shutdown
 * cleanliness [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public final class FlowExecutionKernel {
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "SFM-Flow Execution Worker");
		thread.setDaemon(true); // Daemon state prevents thread hangs on server shutdown [3]
		return thread;
	});

	private FlowExecutionKernel() {
	}

	/**
	 * Submits a new planning task to execute asynchronously on the background
	 * daemon thread [3]. Enforces a 1ms execution budget limit for the evaluation
	 * slice to protect CPU performance [3].
	 *
	 * @param manager  the managing block entity [3]
	 * @param snapshot deep-copied snapshot of connected inventories [3]
	 */
	public static void submitTask(ManagerBlockEntity manager, ThreadSafeInventorySnapshot snapshot) {
		EXECUTOR.submit(() -> {
			try {
				FlowchartPlanningTask task = new FlowchartPlanningTask(manager, snapshot);
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
	public static void shutdown() {
		SFMFlow.LOGGER.info("[SFM-Flow] Shutting down asynchronous planning executor daemon thread...");
		EXECUTOR.shutdown();
		try {
			if (!EXECUTOR.awaitTermination(3, TimeUnit.SECONDS)) {
				EXECUTOR.shutdownNow();
			}
		} catch (InterruptedException e) {
			EXECUTOR.shutdownNow();
			Thread.currentThread().interrupt();
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