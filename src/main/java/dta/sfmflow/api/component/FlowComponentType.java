package dta.sfmflow.api.component;

import java.util.UUID;
import java.util.function.Function;
import com.mojang.serialization.MapCodec;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.plugin.SFMFlowPluginRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Public API registry definition and factory class for custom flowchart
 * component types [3]. Handles mod plugin component hooks dynamically [3].
 */
public class FlowComponentType {
	public static final ResourceKey<Registry<FlowComponentType>> REGISTRY_KEY = ResourceKey
			.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "flow_component_type"));

	public static final DeferredRegister<FlowComponentType> COMPONENT_TYPES = DeferredRegister.create(REGISTRY_KEY,
			SFMFlow.MODID);

	public static final Registry<FlowComponentType> REGISTRY = COMPONENT_TYPES
			.makeRegistry(builder -> builder.sync(true));

	static {
		/* STREAMING_CHUNK:Registering custom plugin components */
		// Sweeps and triggers dynamic registration hooks for vanilla and external plugins [3]
		SFMFlowPluginRegistry.registerAllComponents(COMPONENT_TYPES);
	}

	private final Function<UUID, AbstractFlowComponent> factoryFunction;
	private final MapCodec<? extends AbstractFlowComponent> codec;

	public FlowComponentType(Function<UUID, AbstractFlowComponent> factoryFunction,
			MapCodec<? extends AbstractFlowComponent> codec) {
		this.factoryFunction = factoryFunction;
		this.codec = codec;
	}

	public AbstractFlowComponent createComponent(UUID id) {
		return this.factoryFunction.apply(id);
	}

	public MapCodec<? extends AbstractFlowComponent> codec() {
		return codec;
	}

	public static void register(IEventBus eventBus) {
		COMPONENT_TYPES.register(eventBus);
	}
}
