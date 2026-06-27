package dta.sfmflow.api.component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import com.mojang.serialization.MapCodec;
import dta.sfmflow.api.NodeCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Fluent API builder streamlining the registration of custom flowchart
 * component types [3].
 */
public class FlowComponentBuilder {
	private static final List<FlowComponentBuilder> REGISTERED_BUILDERS = new ArrayList<>();

	private final String name;
	private final Function<UUID, AbstractFlowComponent> factory;
	private NodeCategory category;
	private String iconPath;
	private String displayNameKey;
	private MapCodec<? extends AbstractFlowComponent> codec;
	private DeferredHolder<FlowComponentType, FlowComponentType> holder;

	private FlowComponentBuilder(String name, Function<UUID, AbstractFlowComponent> factory) {
		this.name = name;
		this.factory = factory;
	}

	/**
	 * Instantiates a new builder chain for a designated flowchart node type [3].
	 *
	 * @param name    the unique registry identifier path [3]
	 * @param factory the instantiation factory mapping [3]
	 * @return a new fluent builder instance [3]
	 */
	public static FlowComponentBuilder create(String name, Function<UUID, AbstractFlowComponent> factory) {
		return new FlowComponentBuilder(name, factory);
	}

	/**
	 * Configures the node category for visual grouping in the hover menu [3].
	 *
	 * @param category the node category classification [3]
	 * @return the active builder instance [3]
	 */
	public FlowComponentBuilder category(NodeCategory category) {
		this.category = category;
		return this;
	}

	/**
	 * Configures the relative icon texture asset path for client-side rendering
	 * [3].
	 *
	 * @param iconPath the relative path [3]
	 * @return the active builder instance [3]
	 */
	public FlowComponentBuilder icon(String iconPath) {
		this.iconPath = iconPath;
		return this;
	}

	/**
	 * Configures the localized display translation key [3].
	 *
	 * @param displayNameKey the translation bundle key path [3]
	 * @return the active builder instance [3]
	 */
	public FlowComponentBuilder displayName(String displayNameKey) {
		this.displayNameKey = displayNameKey;
		return this;
	}

	/**
	 * Configures the declarative codec mapped to this component type [3].
	 *
	 * @param codec the subclass map codec [3]
	 * @return the active builder instance [3]
	 */
	public FlowComponentBuilder codec(MapCodec<? extends AbstractFlowComponent> codec) {
		this.codec = codec;
		return this;
	}

	/**
	 * Builds and enqueues the component type into the NeoForge registry system [3].
	 *
	 * @param registry the DeferredRegister manager [3]
	 * @return the registered holder containing the flow component type [3]
	 */
	public DeferredHolder<FlowComponentType, FlowComponentType> build(DeferredRegister<FlowComponentType> registry) {
		this.holder = registry.register(name, () -> new FlowComponentType(factory, codec));
		synchronized (REGISTERED_BUILDERS) {
			REGISTERED_BUILDERS.add(this);
		}
		return this.holder;
	}

	public static List<FlowComponentBuilder> getRegisteredBuilders() {
		return REGISTERED_BUILDERS;
	}

	public NodeCategory getCategory() {
		return category;
	}

	public String getIconPath() {
		return iconPath;
	}

	public String getDisplayNameKey() {
		return displayNameKey;
	}

	public DeferredHolder<FlowComponentType, FlowComponentType> getHolder() {
		return holder;
	}
}