# API and Component Registries

This guide details how to register and deploy custom flowchart nodes and capabilities using the SFM-Flow API.

---

## Registry System Architecture

All flowchart node definitions must be registered with the custom NeoForge registry `sfmflow:flow_component_type`. This registry and its mappings are maintained inside the public package `dta.sfmflow.api.component.FlowComponentType`.

```
  [ ISFMFlowPlugin ] ───► Registers DeferredRegister ───► Populates registry: sfmflow:flow_component_type
                                                                         │
                                                            Contains: sfmflow:interval_trigger
                                                                      addon_id:my_custom_node
```

### Registry Objects Reference

*   **Registry Key**: `FlowComponentType.REGISTRY_KEY` (Points to `sfmflow:flow_component_type`).
*   **Deferred Register Helper**: `FlowComponentType.COMPONENT_TYPES`.
*   **Core Registry Reference**: `FlowComponentType.REGISTRY`.

---

## Registering Custom Components

SFM-Flow features a side-safe logical plugin system (`ISFMFlowPlugin` and `ISFMFlowClientPlugin`). Common components are registered via your plugin's `registerComponents` hook, and client visual properties are bound during client setup.

### Step 1: Enqueueing Custom Components with FlowComponentBuilder
Use the fluent `FlowComponentBuilder` to define and register your flowchart component. This handles compiling categories, icons, translatable keys, and NBT codecs in one chain:

```java
package com.example.addon.components;

import dta.sfmflow.api.NodeCategory;
import dta.sfmflow.api.component.FlowComponentBuilder;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.plugin.ISFMFlowPlugin;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MyAddonPlugin implements ISFMFlowPlugin {
    public static DeferredHolder<FlowComponentType, FlowComponentType> CUSTOM_TRIGGER;

    @Override
    public String getPluginId() {
        return "my_addon";
    }

    @Override
    public void registerComponents(DeferredRegister<FlowComponentType> registry) {
        CUSTOM_TRIGGER = FlowComponentBuilder.create("custom_trigger", CustomTriggerComponent::new)
            .category(NodeCategory.TRIGGER)
            .icon("textures/gui/menu_buttons/custom_trigger_button.png")
            .displayName("gui.my_addon.custom_trigger")
            .codec(CustomTriggerComponent.CODEC)
            .build(registry);
    }
}
```

### Step 2: Component Instance Class
Extend `dta.sfmflow.api.component.AbstractTriggerComponent` for trigger nodes (which configures single-output pin behavior by default) or `AbstractFlowComponent` for standard nodes:

```java
package com.example.addon.components;

import java.util.UUID;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import net.minecraft.network.chat.Component;

public class CustomTriggerComponent extends AbstractTriggerComponent {

    public CustomTriggerComponent(UUID uuid) {
        super(uuid); 
    }

    @Override
    public FlowComponentType getType() {
        return MyAddonPlugin.CUSTOM_TRIGGER.get();
    }

    @Override
    public void plan(FlowchartPlanningContext context) {
        // Queue downstream components connected to this node
        for (var conn : context.getConnections()) {
            if (conn.getSourceComponentId().equals(this.getId())) {
                context.enqueue(conn.getTargetComponentId());
            }
        }
    }
}
```

---

## Client Visual Property Bindings & Settings Widgets

To display your custom node and link interactive configuration UI panels on the client without dedicated server classloading risks, implement `ISFMFlowClientPlugin`:

```java
package com.example.addon.client;

import dta.sfmflow.api.client.FlowSettingsRegistry;
import dta.sfmflow.api.client.plugin.ISFMFlowClientPlugin;
import com.example.addon.components.MyAddonPlugin;
import com.example.addon.client.gui.CustomSettingsWidget;

public class MyAddonClientPlugin implements ISFMFlowClientPlugin {
    @Override
    public String getPluginId() {
        return "my_addon";
    }

    @Override
    public void registerClientProperties() {
        // Register custom settings widget to the component type
        FlowSettingsRegistry.register(MyAddonPlugin.CUSTOM_TRIGGER.get(), (container, component) -> {
            if (component instanceof CustomTriggerComponent trigger) {
                return new CustomSettingsWidget(container, trigger);
			}
            return null;
        });
    }
}
```

---

## Registering Dynamic Capabilities

SFM-Flow replaces hardcoded capabilities with an extensible capability registry (`FlowCapabilityRegistry`). This allows addons to register physical capability routing networks (e.g. energy, custom chemicals, or items) and execute custom transfers:

```java
package com.example.addon.capabilities;

import dta.sfmflow.api.capability.FlowCapability;
import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.Capabilities;

public class AddonCapabilitySetup {
    public static final ResourceLocation ENERGY_CAP_ID = ResourceLocation.fromNamespaceAndPath("my_addon", "energy");

    public static void register() {
        // Register dynamic capability mapping
        FlowCapabilityRegistry.register(new FlowCapability<>(
            ENERGY_CAP_ID, 
            Capabilities.EnergyStorage.BLOCK, 
            "gui.my_addon.type_energy"
        ));

        // Register custom transfer callback logic executed on the server thread
        FlowCapabilityRegistry.registerTransfer(ENERGY_CAP_ID, (level, src, srcSide, dest, destSide, params) -> {
            var source = level.getCapability(Capabilities.EnergyStorage.BLOCK, src, srcSide);
            var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, dest, destSide);

            if (source != null && target != null) {
                int extracted = source.extractEnergy(1000, true);
                int accepted = target.receiveEnergy(extracted, true);
                if (accepted > 0) {
                    source.extractEnergy(accepted, false);
                    target.receiveEnergy(accepted, false);
                    return true;
                }
            }
            return false;
        });
    }
}
```