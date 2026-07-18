package dta.sfmflow.api.event;

import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.kernel.ExecutionTask;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent; 

/**
 * Fired on the main server thread during task execution ticks.
 */
public class TaskExecutionEvent extends Event {
	private final ManagerBlockEntity manager;
	private final ExecutionTask task;

	public TaskExecutionEvent(ManagerBlockEntity manager, ExecutionTask task) {
		this.manager = manager;
		this.task = task;
	}

	public ManagerBlockEntity getManager() {
		return manager;
	}

	public ExecutionTask getTask() {
		return task;
	}

	/**
	 * Fired right before the capability transfer task is executed.
	 * Implements ICancellableEvent to block the transfer dynamically (e.g. anti-griefing).
	 */
	public static class Pre extends TaskExecutionEvent implements ICancellableEvent {
		private boolean canceled = false;

		public Pre(ManagerBlockEntity manager, ExecutionTask task) {
			super(manager, task);
		}

		@Override
		public boolean isCanceled() {
			return this.canceled;
		}

		@Override
		public void setCanceled(boolean cancel) {
			this.canceled = cancel;
		}
	}

	/**
	 * Fired right after the capability transfer task has executed successfully.
	 */
	public static class Post extends TaskExecutionEvent {
		public Post(ManagerBlockEntity manager, ExecutionTask task) {
			super(manager, task);
		}
	}
}