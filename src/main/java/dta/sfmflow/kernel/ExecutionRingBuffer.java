package dta.sfmflow.kernel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance Disruptor-style circular queue for thread-safe pipeline
 * synchronization [3]. Employs power-of-two limits on size to replace slow
 * modulo operations with fast bitwise masking [3].
 */
public final class ExecutionRingBuffer {
	private final int bufferSize;
	private final int mask;
	private final ExecutionTask[] buffer;

	private final AtomicLong writeSequence = new AtomicLong(-1);
	private final AtomicLong readSequence = new AtomicLong(-1);

	/**
	 * Instantiates the buffer and pre-allocates recyclable execution frames [3].
	 *
	 * @param size requested buffer capacity, rounded to the next power of two [3]
	 */
	public ExecutionRingBuffer(int size) {
		this.bufferSize = findNextPowerOfTwo(size);
		this.mask = this.bufferSize - 1;
		this.buffer = new ExecutionTask[this.bufferSize];
		for (int i = 0; i < this.bufferSize; i++) {
			this.buffer[i] = new ExecutionTask();
		}
	}

	/**
	 * Attempts to claim a buffer slot and populate a task frame safely [3]. Returns
	 * false if the queue is full or the reader is still accessing the slot [3].
	 *
	 * @return true if successfully published [3]
	 */
	public boolean tryWrite(BlockPos src, int srcSlot, BlockPos dest, int destSlot, ItemStack stack, int amount) {
		long currentWrite = writeSequence.get();
		long currentRead = readSequence.get();

		if (currentWrite - currentRead >= bufferSize) {
			return false; // Queue is full
		}

		int index = (int) ((currentWrite + 1) & mask);
		ExecutionTask task = buffer[index];

		if (task.isUsed()) {
			return false; // Reader has not completed reading this slot [3]
		}

		task.set(src, srcSlot, dest, destSlot, stack, amount);
		writeSequence.incrementAndGet();
		return true;
	}

	/**
	 * Polls the ring buffer sequentially, executing available task frames and
	 * recycling slots [3].
	 *
	 * @param taskConsumer executing task lambda receiver [3]
	 */
	public void pollAndExecute(java.util.function.Consumer<ExecutionTask> taskConsumer) {
		long currentWrite = writeSequence.get();
		long currentRead = readSequence.get();

		while (currentRead < currentWrite) {
			int index = (int) ((currentRead + 1) & mask);
			ExecutionTask task = buffer[index];

			if (task.isUsed()) {
				taskConsumer.accept(task);
				task.reset(); // Recycle container slot safely [3]
			}
			currentRead = readSequence.incrementAndGet();
		}
	}

	private int findNextPowerOfTwo(int value) {
		int highestOneBit = Integer.highestOneBit(value);
		if (highestOneBit == value) {
			return value;
		}
		return highestOneBit << 1;
	}
}