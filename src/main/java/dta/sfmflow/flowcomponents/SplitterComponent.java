package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Logical flowchart node that splits execution paths based on ALL, ROUND_ROBIN, or RANDOM triggers [3].
 */
public class SplitterComponent extends AbstractFlowComponent {

	public enum SplitterMode implements StringRepresentable {
		ALL("all"),
		ROUND_ROBIN("round_robin"),
		RANDOM("random");

		private final String name;

		SplitterMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final Codec<SplitterMode> MODE_CODEC = StringRepresentable.fromEnum(SplitterMode::values);

	public static final MapCodec<SplitterComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(SplitterComponent::getBaseProperties),
					MODE_CODEC.optionalFieldOf("splitterMode", SplitterMode.ALL).forGetter(SplitterComponent::getSplitterMode),
					Codec.INT.optionalFieldOf("numOutputs", 2).forGetter(SplitterComponent::getNumOutputs),
					Codec.INT.optionalFieldOf("lastOutputIndex", -1).forGetter(SplitterComponent::getLastOutputIndex))
			.apply(instance, (baseProps, mode, outputsCount, lastIdx) -> {
				SplitterComponent comp = new SplitterComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.splitterMode = mode;
				comp.numOutputs = outputsCount;
				comp.lastOutputIndex = lastIdx;
				return comp;
			}));

	private SplitterMode splitterMode = SplitterMode.ALL;
	private int lastOutputIndex = -1;

	public SplitterComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 2; // Default to 2 outputs [3]
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.SPLITTER.get();
	}

	public SplitterMode getSplitterMode() {
		return splitterMode;
	}

	public void setSplitterMode(SplitterMode splitterMode) {
		this.splitterMode = splitterMode;
	}

	public int getNumOutputs() {
		return this.numOutputs;
	}

	public void setNumOutputs(int count) {
		this.numOutputs = count;
	}

	public int getLastOutputIndex() {
		return lastOutputIndex;
	}

	public void setLastOutputIndex(int lastOutputIndex) {
		this.lastOutputIndex = lastOutputIndex;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		// 1. Resolve and increment the splitter chain depth [3]
		ResourceLocation chainDepthKey = ResourceLocation.fromNamespaceAndPath("sfmflow", "splitter_chain_depth");
		Integer currentDepthObj = (Integer) context.getPipelineBuffer(this.getId(), chainDepthKey);
		int currentDepth = currentDepthObj != null ? currentDepthObj : 0;
		int nextDepth = currentDepth + 1;

		// 2. Perform the server-side budget safety check [3]
		if (nextDepth > ServerConfig.MAX_CHAINED_SPLITTERS.get()) {
			FlowLogger.execution("Circuit breaker: Splitter chain depth limit exceeded at %s (%d > %d). Suppressing downstream executions.",
					this.getId(), nextDepth, ServerConfig.MAX_CHAINED_SPLITTERS.get());
			return; // Stop propagating execution downstream [3]
		}

		List<Integer> targets = new ArrayList<>();

		if (this.splitterMode == SplitterMode.ALL) {
			for (int i = 0; i < this.numOutputs; i++) {
				targets.add(i);
			}
		} else if (this.splitterMode == SplitterMode.ROUND_ROBIN) {
			int next = (this.lastOutputIndex + 1) % this.numOutputs;
			this.lastOutputIndex = next; // Update locally on the worker clone [3]
			targets.add(next);

			// Queue a thread-safe deferred state synchronization back to the main server thread [3]
			context.tryWriteTask(
					ResourceLocation.fromNamespaceAndPath("sfmflow", "splitter_sync"),
					BlockPos.ZERO, 0, null, BlockPos.ZERO, 0, null,
					new SplitterSyncParams(this.getId(), this.lastOutputIndex)
			);
		} else if (this.splitterMode == SplitterMode.RANDOM) {
			int randIdx = ThreadLocalRandom.current().nextInt(this.numOutputs);
			targets.add(randIdx);
		}

		// 3. Propagate the chain depth to downstream targets [3]
		for (int outputIdx : targets) {
			for (FlowComponentConnections conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(this.getId()) && conn.getOutputNodeIndex() == outputIdx) {
					UUID targetId = conn.getTargetComponentId();
					context.setPipelineBuffer(targetId, chainDepthKey, nextDepth); // Propagate depth [3]
					context.enqueue(targetId);
				}
			}
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putString("splitterMode", this.splitterMode.name());
		compoundTag.putInt("numOutputs", this.numOutputs);
		compoundTag.putInt("lastOutputIndex", this.lastOutputIndex);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		SplitterComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse splitter component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.splitterMode = decoded.getSplitterMode();
					this.numOutputs = decoded.getNumOutputs();
					this.lastOutputIndex = decoded.getLastOutputIndex();
				});

		if (compoundTag.contains("splitterMode")) {
			try {
				this.splitterMode = SplitterMode.valueOf(compoundTag.getString("splitterMode"));
			} catch (IllegalArgumentException ignored) {}
		}
		if (compoundTag.contains("numOutputs")) {
			this.numOutputs = compoundTag.getInt("numOutputs");
		}
		if (compoundTag.contains("lastOutputIndex")) {
			this.lastOutputIndex = compoundTag.getInt("lastOutputIndex");
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.splitter");
	}

	@Override
	public Component getInputNodeTooltip(int index) {
		return Component.literal("Execute Input");
	}

	@Override
	public Component getOutputNodeTooltip(int index) {
		return Component.literal("Output Pin #" + (index + 1));
	}

	/**
	 * Packet parameters payload record for off-thread state synchronization [3].
	 */
	public record SplitterSyncParams(UUID componentId, int nextIndex) {}
}