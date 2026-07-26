package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.execution.ThreadSafeInventorySnapshot;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.util.Color;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

public class ForEachComponent extends AbstractFlowComponent {

	public enum IterationMode implements StringRepresentable {
		TOP_TO_BOTTOM("top_to_bottom"), BOTTOM_TO_TOP("bottom_to_top"), RANDOM("random");

		private final String name;

		IterationMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final Codec<IterationMode> MODE_CODEC = StringRepresentable.fromEnum(IterationMode::values);

	public static final MapCodec<ForEachComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(ForEachComponent::getBaseProperties),
					UUIDUtil.CODEC
							.optionalFieldOf("boundListVariableId")
							.forGetter(comp -> Optional.ofNullable(comp.getBoundListVariableId())),
					Codec.STRING.optionalFieldOf("elementName", "Element").forGetter(ForEachComponent::getElementName),
					Color.CODEC.optionalFieldOf("elementColor", Color.WHITE)
							.forGetter(ForEachComponent::getElementColor),
					MODE_CODEC.optionalFieldOf("iterationMode", IterationMode.TOP_TO_BOTTOM)
							.forGetter(ForEachComponent::getIterationMode))
			.apply(instance, (baseProps, boundListVar, elemName, elemColor, mode) -> {
				ForEachComponent comp = new ForEachComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.boundListVariableId = boundListVar.orElse(null);
				comp.elementName = elemName;
				comp.elementColor = elemColor;
				comp.iterationMode = mode;
				return comp;
			}));

	private UUID boundListVariableId = null;
	private String elementName = "Element";
	private Color elementColor = Color.WHITE;
	private IterationMode iterationMode = IterationMode.TOP_TO_BOTTOM;

	public ForEachComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 2; // Output 0 = Completion, Output 1 = Iteration
		this.hasRightOutput = true;
	}

	@Override
	public boolean acceptsInventoryListCards() {
		return true;
	}

	@Override
	public @Nullable UUID getBoundInventoryListVariableId() {
		return this.boundListVariableId;
	}

	@Override
	public void setBoundInventoryListVariableId(@Nullable UUID id) {
		this.setBoundListVariableId(id);
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.FOREACH_LOOP.get();
	}

	public UUID getBoundListVariableId() {
		return boundListVariableId;
	}

	public void setBoundListVariableId(UUID id) {
		this.boundListVariableId = id;
	}

	public String getElementName() {
		return elementName;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName == null ? "Element" : elementName;
	}

	public Color getElementColor() {
		return elementColor;
	}

	public void setElementColor(Color elementColor) {
		this.elementColor = elementColor == null ? Color.WHITE : elementColor;
	}

	public IterationMode getIterationMode() {
		return iterationMode;
	}

	public void setIterationMode(IterationMode mode) {
		this.iterationMode = mode == null ? IterationMode.TOP_TO_BOTTOM : mode;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		FlowLogger.execution("[ForEach] Planning started. BoundListVariableId: %s", this.boundListVariableId);

		// 1. Resolve and increment ForEach chain depth for safety circuit breaker
		ResourceLocation depthKey = ResourceLocation.fromNamespaceAndPath("sfmflow", "foreach_chain_depth");
		Integer depthVal = (Integer) context.getPipelineBuffer(this.getId(), depthKey);
		int currentDepth = depthVal != null ? depthVal : 0;
		int nextDepth = currentDepth + 1;

		if (nextDepth > ServerConfig.MAX_CHAINED_FOREACH.get()) {
			FlowLogger.execution(
					"[ForEach] Circuit breaker: ForEach chain depth limit exceeded at %s (%d > %d). Suppressing downstream executions.",
					this.getId(), nextDepth, ServerConfig.MAX_CHAINED_FOREACH.get());
			return;
		}

		if (this.boundListVariableId == null) {
			FlowLogger.execution("[ForEach] boundListVariableId is NULL. Advancing straight to completion.");
			triggerCompletion(context);
			return;
		}

		AbstractFlowComponent listComp = context.getComponents().get(this.boundListVariableId);
		if (listComp == null) {
			FlowLogger.execution("[ForEach] listComp not found in context components!");
			triggerCompletion(context);
			return;
		}
		FlowLogger.execution("[ForEach] Found listComp: %s of class %s", listComp.getName().getString(),
				listComp.getClass().getSimpleName());

		// Abstractly resolve list elements without hardcoded types
		List<ConnectionBlock> resolvedList = listComp.resolveListElements(context);

		FlowLogger.execution("[ForEach] Total resolved iteration elements: %d", resolvedList.size());

		if (resolvedList.isEmpty()) {
			triggerCompletion(context);
			return;
		}

		// 3. Re-order based on IterationMode
		if (this.iterationMode == IterationMode.BOTTOM_TO_TOP) {
			Collections.reverse(resolvedList);
		} else if (this.iterationMode == IterationMode.RANDOM) {
			Collections.shuffle(resolvedList, ThreadLocalRandom.current());
		}

		// 4. Sequentially execute the loop body synchronous sub-traversal
		ResourceLocation activeBlockKey = ResourceLocation.fromNamespaceAndPath("sfmflow", "active_foreach_block");

		for (ConnectionBlock currentElement : resolvedList) {
			context.setPipelineBuffer(this.getId(), activeBlockKey, currentElement);

			Set<UUID> subVisited = new HashSet<>();
			Queue<UUID> subQueue = new ArrayDeque<>();

			FlowchartPlanningContext subContext = new FlowchartPlanningContext() {
				@Override
				public ThreadSafeInventorySnapshot getSnapshot() {
					return context.getSnapshot();
				}

				@Override
				public Map<UUID, AbstractFlowComponent> getComponents() {
					return context.getComponents();
				}

				@Override
				public List<FlowComponentConnections> getConnections() {
					return context.getConnections();
				}

				@Override
				public List<ConnectionBlock> getConnectedInventories() {
					return context.getConnectedInventories();
				}

				@Override
				public void enqueue(UUID componentId) {
					if (componentId != null && !subVisited.contains(componentId)) {
						setPipelineBuffer(componentId, depthKey, nextDepth);
						subVisited.add(componentId);
						subQueue.add(componentId);
					}
				}

				@Override
				public boolean tryWriteTask(ResourceLocation capabilityId, BlockPos src, int srcSlot, Direction srcSide,
						BlockPos dest, int destSlot, Direction destSide, Object taskParams) {
					return context.tryWriteTask(capabilityId, src, srcSlot, srcSide, dest, destSlot, destSide,
							taskParams);
				}

				@Override
				public Object getPipelineBuffer(UUID componentId, ResourceLocation capabilityId) {
					return context.getPipelineBuffer(componentId, capabilityId);
				}

				@Override
				public void setPipelineBuffer(UUID componentId, ResourceLocation capabilityId, Object buffer) {
					context.setPipelineBuffer(componentId, capabilityId, buffer);
				}

				@Override
				public void copyPipelineBuffers(UUID srcComponentId, UUID destComponentId) {
					context.copyPipelineBuffers(srcComponentId, destComponentId);
				}
			};

			int startNodeConnectionsCount = 0;
			// Seed sub-traversal from right-side iteration output pin (index 1)
			for (FlowComponentConnections conn : context.getConnections()) {
				if (conn.getSourceComponentId().equals(this.getId()) && conn.getOutputNodeIndex() == 1) {
					UUID targetId = conn.getTargetComponentId();

					// Dynamically copy all pipeline buffers (Items, Fluids, Energy, and custom
					// Addon buffers)
					context.copyPipelineBuffers(this.getId(), targetId);

					subQueue.add(targetId);
					subVisited.add(targetId);
					startNodeConnectionsCount++;
				}
			}

			FlowLogger.execution("[ForEach] Iteration Element: %s at %s. Seeding %d sub-traversal starting nodes.",
					currentElement.getDisplayName(null).getString(), currentElement.getBlockPos(),
					startNodeConnectionsCount);

			// Run sub-traversal sequentially
			int subNodesTraversed = 0;
			while (!subQueue.isEmpty()) {
				UUID currId = subQueue.poll();
				AbstractFlowComponent curr = context.getComponents().get(currId);
				if (curr != null) {
					subNodesTraversed++;
					FlowLogger.execution("[ForEach]   Planning loop node: %s (%s)", curr.getName().getString(), currId);
					curr.plan(subContext);
				}
			}
			FlowLogger.execution("[ForEach] Iteration finished. Sub-nodes traversed: %d", subNodesTraversed);
		}

		// 5. Clean up and trigger completion pin (index 0)
		context.setPipelineBuffer(this.getId(), activeBlockKey, null);
		triggerCompletion(context);
	}

	private void triggerCompletion(FlowchartPlanningContext context) {
		for (FlowComponentConnections conn : context.getConnections()) {
			if (conn.getSourceComponentId().equals(this.getId()) && conn.getOutputNodeIndex() == 0) {
				context.enqueue(conn.getTargetComponentId());
			}
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		if (this.boundListVariableId != null) {
			compoundTag.putUUID("boundListVariableId", this.boundListVariableId);
		}
		compoundTag.putString("elementName", this.elementName);
		compoundTag.putString("elementColor", this.elementColor.getSerializedName());
		compoundTag.putString("iterationMode", this.iterationMode.getSerializedName());
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		ForEachComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse ForEach component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.boundListVariableId = decoded.getBoundListVariableId();
					this.elementName = decoded.getElementName();
					this.elementColor = decoded.getElementColor();
					this.iterationMode = decoded.getIterationMode();
				});

		super.loadData(compoundTag);

		if (compoundTag.contains("boundListVariableId")) {
			this.boundListVariableId = compoundTag.getUUID("boundListVariableId");
		} else {
			this.boundListVariableId = null;
		}
		if (compoundTag.contains("elementName")) {
			this.elementName = compoundTag.getString("elementName");
		}
		if (compoundTag.contains("elementColor")) {
			try {
				this.elementColor = Color.valueOf(compoundTag.getString("elementColor").toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				this.elementColor = Color.WHITE;
			}
		}
		if (compoundTag.contains("iterationMode")) {
			String val = compoundTag.getString("iterationMode");
			for (IterationMode m : IterationMode.values()) {
				if (m.name().equalsIgnoreCase(val) || m.getSerializedName().equalsIgnoreCase(val)) {
					this.iterationMode = m;
					break;
				}
			}
		}
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.literal("ForEach Loop");
	}

	@Override
	public Component getInputNodeTooltip(int index) {
		return Component.literal("Execute Input");
	}

	@Override
	public Component getOutputNodeTooltip(int index) {
		return index == 0 ? Component.literal("Completion Output") : Component.literal("Iteration Output");
	}
}