# API and Component Registries

This guide details how to register and deploy custom flowchart nodes using the SFM-Flow API.

---

## Registry System Architecture

All flowchart node definitions must be registered with the custom NeoForge registry `sfmflow:flow_component_type`. This registry and its mappings are maintained inside the public package `dta.sfmflow.api.component.FlowComponentType`.

```
  [ Mod Event Bus ] ───► Registers DeferredRegister ───► Populates registry: sfmflow:flow_component_type
                                                                         │
                                                            Contains: sfmflow:interval_trigger
                                                                      addon_id:my_custom_node
```

### Registry Objects Reference

*   **Registry Key**: `FlowComponentType.REGISTRY_KEY` (Points to `sfmflow:flow_component_type`).
*   **Deferred Register Helper**: `FlowComponentType.COMPONENT_TYPES`.
*   **Core Registry Reference**: `FlowComponentType.REGISTRY`.
*   **Built-in Elements**: `FlowComponentType.INTERVAL_TRIGGER` registers the periodic trigger component (`sfmflow:interval_trigger`), replacing any legacy unspecialized trigger entries.

---

## Registering Custom Components

To introduce a custom component type, define a component class extending the API's `AbstractFlowComponent` (or `AbstractTriggerComponent` for trigger nodes) and link it through your addon registry.

### Step 1: Component Instance Class
If you are developing a starting trigger node, extend `dta.sfmflow.api.component.AbstractTriggerComponent` to inherit the single-output node configuration layouts:

```java
package com.example.addon.components;

import java.util.UUID;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class CustomTriggerComponent extends AbstractTriggerComponent {

    public CustomTriggerComponent(UUID uuid) {
        // Automatically configures single logic output connector node behavior
        super(uuid); 
    }

    @Override
    public FlowComponentType getType() {
        return ModComponentRegistries.CUSTOM_TRIGGER.get();
    }

    @Override
    public Component getName() {
        return Component.translatable("gui.addon.custom_trigger");
    }
}
```

### Step 2: Enqueueing Custom Registry Objects
Create a registration class to manage and register your component definition on the mod event bus:

```java
package com.example.addon.components;

import dta.sfmflow.api.component.FlowComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModComponentRegistries {

    public static final DeferredRegister<FlowComponentType> ADDON_COMPONENTS = 
        DeferredRegister.create(FlowComponentType.REGISTRY_KEY, "addon_id");

    public static final DeferredHolder<FlowComponentType, FlowComponentType> CUSTOM_TRIGGER = 
        ADDON_COMPONENTS.register("custom_trigger", () -> new FlowComponentType(CustomTriggerComponent::new));

    public static void init(IEventBus eventBus) {
        ADDON_COMPONENTS.register(eventBus);
    }
}
```

---

## Client Visual Property Bindings

SFM-Flow decouples backend flowchart logic from UI presentation. To display your custom node in the client-side creator sidebar menus, bind its presentation rules to the `FlowClientRegistry` using `INodeClientProperties` during client setup:

### Texture Requirements
The custom texture sheet supplied via `getIconTexture()` must be formatted as a vertically stacked texture sheet of `14x28` pixels:
*   The top `14x14` pixel region represents the default button state.
*   The bottom `14x14` pixel region represents its active hover state.

```java
package com.example.addon.client;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.client.FlowClientRegistry;
import dta.sfmflow.api.client.INodeClientProperties;
import com.example.addon.components.ModComponentRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Supplier;

@EventBusSubscriber(modid = "addon_id", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AddonClientSetup {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            FlowClientRegistry.register(ModComponentRegistries.CUSTOM_TRIGGER.get(), new INodeClientProperties() {
                @Override
                public NodeCategory getCategory() {
                    return NodeCategory.TRIGGER; // Maps this node into the sliding TRIGGER submenu grid
                }

                @Override
                public ResourceLocation getIconTexture() {
                    // Must reference a 14x28 pixel stacked hover sheet
                    return ResourceLocation.fromNamespaceAndPath("addon_id", "textures/gui/menu_buttons/custom_trigger.png");
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.addon.custom_trigger");
                }

                @Override
                public Supplier<Boolean> isEnabled() {
                    return () -> true; // Controls node creator visibility in the submenu
                }
            });
        });
    }
}
```