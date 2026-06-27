package dta.sfmflow.api.flowchart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.DataResult;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.flowcomponents.FlowComponentConnections;

/**
 * Core MVC data model representing the active flowchart canvas workspace layout
 * [3].
 */
public record Flowchart(Map<UUID, AbstractFlowComponent> components, List<FlowComponentConnections> connections) {
	/**
	 * Canonical constructor designed to intercept deserialized collections [3].
	 * Explicitly wraps immutable collections produced by Mojang Codec parse passes
	 * into mutable java.util.HashMap and java.util.ArrayList implementations to
	 * support real-time canvas editing [3].
	 *
	 * @param components  the components map [3]
	 * @param connections the connections list [3]
	 */
	public Flowchart(Map<UUID, AbstractFlowComponent> components, List<FlowComponentConnections> connections) {
		this.components = components == null ? new HashMap<>() : new HashMap<>(components);
		this.connections = connections == null ? new ArrayList<>() : new ArrayList<>(connections);
	}

//Safe helper record representing our NBT key-value layout compound entry
	record ComponentEntry(UUID id, AbstractFlowComponent component) {
		public static final Codec<ComponentEntry> CODEC = RecordCodecBuilder.create(instance -> instance
				.group(net.minecraft.core.UUIDUtil.CODEC.fieldOf("id").forGetter(ComponentEntry::id),
						AbstractFlowComponent.CODEC.fieldOf("value").forGetter(ComponentEntry::component) // Triggers
																											// partialDispatch
				).apply(instance, ComponentEntry::new));
	}

	/**
	 * Codec for serializing maps with UUID keys as a safe NBT List of compound
	 * entries. Completely circumvents Mojang's UnboundedMapCodec / KeyDispatchCodec
	 * NullPointerException bugs!
	 */
	/**
	 * Codec for serializing maps with UUID keys as an NBT List of compound entries.
	 * Fully type-hinted and insulated with an element firewall to stop Mojang
	 * ListCodec null pointer crashes!
	 */
	public static final Codec<Map<UUID, AbstractFlowComponent>> COMPONENTS_MAP_CODEC = ComponentEntry.CODEC.listOf()
			.flatXmap(rawList -> {
				// THE ELEMENT FIREWALL: Filter out any components that partialDispatch marked
				// as null or failure states
				List<ComponentEntry> cleanList = rawList.stream()
						.filter(entry -> entry != null && entry.id() != null && entry.component() != null).toList();
				return DataResult.success(cleanList);
			}, DataResult::success).xmap(entryList -> {
				Map<UUID, AbstractFlowComponent> uuidMap = new HashMap<>();
				for (ComponentEntry entry : entryList) {
					uuidMap.put(entry.id(), entry.component());
				}
				return uuidMap;
			}, uuidMap -> uuidMap.entrySet().stream().map(e -> new ComponentEntry(e.getKey(), e.getValue())).toList());
	/**
	 * Declarative Codec for encoding/decoding the complete flowchart compound [3].
	 */
	public static final Codec<Flowchart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			COMPONENTS_MAP_CODEC.optionalFieldOf("components", java.util.Map.of()).forGetter(Flowchart::components),

			// THE FIREWALL: Replace the default ListCodec with a resilient element stream
			// filter
			FlowComponentConnections.CODEC.listOf().flatXmap(rawList -> {
				// Drop any connection entries that DFU parsed as null due to hidden polymorphic
				// sub-field validation failures
				java.util.List<FlowComponentConnections> cleanList = rawList.stream().filter(java.util.Objects::nonNull)
						.toList();
				return com.mojang.serialization.DataResult.success(cleanList);
			}, com.mojang.serialization.DataResult::success).optionalFieldOf("connections", java.util.List.of())
					.forGetter(Flowchart::connections))
			.apply(instance, Flowchart::new));
}