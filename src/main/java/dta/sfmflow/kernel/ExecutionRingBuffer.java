package dta.sfmflow.kernel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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
	 * Retrieves the current write sequence index [3].
	 *
	 * @return write index atomic long value [3]
	 */
	public long getWriteSequence() {
		return this.writeSequence.get();
	}

	/**
	 * Retrieves the current read sequence index [3].
	 *
	 * @return read index atomic long value [3]
	 */
	public long getReadSequence() {
		return this.readSequence.get();
	}

	/**
	 * Attempts to claim a buffer slot and populate a task frame safely [3].
	 */
	public boolean tryWrite(ResourceLocation capabilityId, BlockPos src, int srcSlot, @Nullable Direction srcSide, BlockPos dest, int destSlot, @Nullable Direction destSide, Object params) {
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

		task.set(capabilityId, src, srcSlot, srcSide, dest, destSlot, destSide, params);
		writeSequence.incrementAndGet();
		return true;
	}

	/**
	 * Polls the ring buffer sequentially up to a given time budget (in nanoseconds) [3].
	 * Un-executed tasks naturally remain in the circular buffer to be processed on subsequent ticks [3].
	 */
	public void pollAndExecuteThrottled(Consumer<ExecutionTask> taskConsumer, long maxTimeNs) {
		long currentWrite = writeSequence.get();
		long currentRead = readSequence.get();
		long startTime = System.nanoTime();

		while (currentRead < currentWrite) {
			if (System.nanoTime() - startTime >= maxTimeNs) {
				break; // Budget exhausted; remaining tasks stay in queue until next tick [3]
			}

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
