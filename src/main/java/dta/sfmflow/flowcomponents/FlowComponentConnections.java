package dta.sfmflow.flowcomponents;

import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

public class FlowComponentConnections {
	public static final Codec<FlowComponentConnections> CODEC = RecordCodecBuilder
			.create(instance -> instance
					.group(UUIDUtil.CODEC.fieldOf("sourceId").forGetter(FlowComponentConnections::getSourceComponentId),
							Codec.INT.fieldOf("outputIdx").forGetter(FlowComponentConnections::getOutputNodeIndex),
							UUIDUtil.CODEC.fieldOf("targetId")
									.forGetter(FlowComponentConnections::getTargetComponentId),
							Codec.INT.fieldOf("inputIdx").forGetter(FlowComponentConnections::getInputNodeIndex))
					.apply(instance, FlowComponentConnections::new));

	private final UUID sourceComponentId;
	private final UUID targetComponentId;
	private final int outputNodeIndex;
	private final int inputNodeIndex;

	public FlowComponentConnections(UUID sourceComponentId, int outputNodeIndex, UUID targetComponentId,
			int inputNodeIndex) {
		this.sourceComponentId = sourceComponentId;
		this.outputNodeIndex = outputNodeIndex;
		this.targetComponentId = targetComponentId;
		this.inputNodeIndex = inputNodeIndex;
	}

	public UUID getSourceComponentId() {
		return this.sourceComponentId;
	}

	public int getOutputNodeIndex() {
		return this.outputNodeIndex;
	}

	public UUID getTargetComponentId() {
		return this.targetComponentId;
	}

	public int getInputNodeIndex() {
		return this.inputNodeIndex;
	}

	public CompoundTag save(CompoundTag tag) {
		tag.putUUID("SourceId", this.sourceComponentId);
		tag.putInt("OutputIdx", this.outputNodeIndex);
		tag.putUUID("TargetId", this.targetComponentId);
		tag.putInt("InputIdx", this.inputNodeIndex);
		return tag;
	}

	public static FlowComponentConnections load(CompoundTag tag) {
		UUID sourceId = tag.getUUID("SourceId");
		int outputIdx = tag.getInt("OutputIdx");
		UUID targetId = tag.getUUID("TargetId");
		int inputIdx = tag.getInt("InputIdx");
		return new FlowComponentConnections(sourceId, outputIdx, targetId, inputIdx);
	}
	
	/**
	 * Checks if adding a connection from sourceId to targetId would create a loop/cycle [3].
	 */
	public static boolean wouldCreateCycle(java.util.List<FlowComponentConnections> connections, UUID sourceId, UUID targetId) {
		if (sourceId.equals(targetId)) {
			return true;
		}

		java.util.Queue<UUID> queue = new java.util.ArrayDeque<>();
		java.util.Set<UUID> visited = new java.util.HashSet<>();

		queue.add(targetId);
		visited.add(targetId);

		while (!queue.isEmpty()) {
			UUID current = queue.poll();
			if (current.equals(sourceId)) {
				return true;
			}

			for (FlowComponentConnections conn : connections) {
				if (conn.getSourceComponentId().equals(current)) {
					UUID next = conn.getTargetComponentId();
					if (!visited.contains(next)) {
						visited.add(next);
						queue.add(next);
					}
				}
			}
		}

		return false;
	}

}