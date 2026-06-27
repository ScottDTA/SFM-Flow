package dta.sfmflow.api.component;

import java.util.UUID;
import java.util.function.Function;
import com.mojang.serialization.MapCodec;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.flowcomponents.IntervalTriggerComponent;
import dta.sfmflow.flowcomponents.ItemTransferComponent;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Public API registry definition and factory class for custom flowchart component types [3].
 * Houses registration handlers and manages instantiation logic for flowchart nodes [3].
 */
public class FlowComponentType
 {
  public static final ResourceKey<Registry<FlowComponentType>> REGISTRY_KEY = ResourceKey.createRegistryKey(
      ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "flow_component_type")
  );

  public static final DeferredRegister<FlowComponentType> COMPONENT_TYPES = DeferredRegister.create(REGISTRY_KEY, SFMFlow.MODID);
  
  // Synchronized registry map keeps client-server runtimes aligned [3]
  public static final Registry<FlowComponentType> REGISTRY = COMPONENT_TYPES.makeRegistry(builder -> builder.sync(true));

  /**
   * Registered supplier holder for the specialized INTERVAL_TRIGGER component type [3].
   */
  public static final DeferredHolder<FlowComponentType, FlowComponentType> INTERVAL_TRIGGER = FlowComponentBuilder.create("interval_trigger", IntervalTriggerComponent::new)
      .category(NodeCategory.TRIGGER)
      .icon("textures/gui/menu_buttons/trigger_button.png")
      .displayName("gui.sfmflow.interval_trigger")
      .codec(IntervalTriggerComponent.CODEC)
      .build(COMPONENT_TYPES);

  /**
   * Registered supplier holder for the specialized ITEM_INPUT component type [3].
   * Consolidated: points to the unified ItemTransferComponent parameterized as an input node [3].
   */
  public static final DeferredHolder<FlowComponentType, FlowComponentType> ITEM_INPUT = FlowComponentBuilder.create("item_input", uuid -> new ItemTransferComponent(uuid, true))
      .category(NodeCategory.INPUT)
      .icon("textures/gui/menu_buttons/input_button.png")
      .displayName("gui.sfmflow.item_input")
      .codec(ItemTransferComponent.INPUT_CODEC)
      .build(COMPONENT_TYPES);

  /**
   * Registered supplier holder for the specialized ITEM_OUTPUT component type [3].
   * Consolidated: points to the unified ItemTransferComponent parameterized as an output node [3].
   */
  public static final DeferredHolder<FlowComponentType, FlowComponentType> ITEM_OUTPUT = FlowComponentBuilder.create("item_output", uuid -> new ItemTransferComponent(uuid, false))
      .category(NodeCategory.OUTPUT)
      .icon("textures/gui/menu_buttons/output_button.png")
      .displayName("gui.sfmflow.item_output")
      .codec(ItemTransferComponent.OUTPUT_CODEC)
      .build(COMPONENT_TYPES);

  private final Function<UUID, AbstractFlowComponent> factoryFunction;
  private final MapCodec<? extends AbstractFlowComponent> codec;

  public FlowComponentType(Function<UUID, AbstractFlowComponent> factoryFunction, MapCodec<? extends AbstractFlowComponent> codec)
   {
    this.factoryFunction = factoryFunction;
    this.codec = codec;
   }

  /**
   * Instantiates a new abstract flow component from this registered type [3].
   *
   * @param id the unique UUID for the new component [3]
   * @return the instantiated flow component [3]
   */
  public AbstractFlowComponent createComponent(UUID id)
   {
    return this.factoryFunction.apply(id);
   }

  /**
   * Exposes the MapCodec designed for this specific component type [3].
   *
   * @return the MapCodec instance [3]
   */
  public MapCodec<? extends AbstractFlowComponent> codec()
   {
    return codec;
   }

  /**
   * Registers this custom registry and its holders to the mod event bus.
   *
   * @param eventBus the mod event bus instance
   */
  public static void register(IEventBus eventBus)
   {
    COMPONENT_TYPES.register(eventBus);
   }
 }