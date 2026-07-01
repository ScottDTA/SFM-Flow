# Creating Custom Nodes

Custom nodes are created by extending `dta.sfmflow.api.component.AbstractFlowComponent` or `dta.sfmflow.api.component.AbstractTriggerComponent` and writing declarative Mojang Codecs for data serialization.

---

## Sizing & Layout Constants

All nodes are rendered strictly in compact mode to maintain UI density. Sizing is locked to standard constants defined in `AbstractFlowComponent`:

*   **`BASE_WIDTH`**: `64`
*   **`BASE_HEIGHT`**: `20`
*   **`OUTPUT_EXTENSION`**: `6`
*   **`INPUT_EXTENSION`**: `6`
*   **`CANVAS_MAX_X`**: `508`
*   **`CANVAS_MAX_Y`**: `240`

---

## State & NBT Serialization via Codecs

SFM-Flow uses Mojang Codecs (`Codec` and `MapCodec`) to serialize and deserialize component data instead of raw NBT reads and writes. Custom components must provide a static `MapCodec` that incorporates the superclass `BaseProperties`:

```java
package com.example.addon.components;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class CustomNodeComponent extends AbstractFlowComponent {
    private int customRate = 20;

    public static final MapCodec<CustomNodeComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BaseProperties.CODEC.fieldOf("base").forGetter(CustomNodeComponent::getBaseProperties),
        Codec.INT.optionalFieldOf("customRate", 20).forGetter(CustomNodeComponent::getCustomRate)
    ).apply(instance, (baseProps, rate) -> {
        CustomNodeComponent comp = new CustomNodeComponent(baseProps.id());
        comp.setBaseProperties(baseProps);
        comp.setCustomRate(rate);
        return comp;
    }));

    public CustomNodeComponent(UUID uuid) {
        super(uuid);
    }

    public int getCustomRate() { return this.customRate; }
    public void setCustomRate(int rate) { this.customRate = rate; }

    @Override
    public FlowComponentType getType() {
        return MyAddonPlugin.CUSTOM_NODE.get();
    }
}
```

### Base Properties Serialization
`BaseProperties` automatically manages and serializes these core spatial NBT parameters:

| Field Name | Type | Key | Description |
|---|---|---|---|
| `id` | `UUID` | `id` | Unique identifier for the component instance. |
| `x` | `int` | `x` | Relative X-coordinate on the canvas viewport. |
| `y` | `int` | `y` | Relative Y-coordinate on the canvas viewport. |
| `z` | `int` | `z` | Depth sorting layer (Z-rank). |
| `customName` | `String` | `customName` | Custom nickname assigned to the component. |
| `colorMask` | `Color` | `colorMask` | RGB color-gradient tint mask. |

---

## Creating the Custom Settings UI Panel

Since nodes are permanently compact, configuring settings requires creating a centered **Settings Overlay** (`NodeSettingsOverlay`) that displays when double-clicking the node:

```java
package com.example.addon.client.gui;

import dta.sfmflow.api.client.widget.AbstractFlowWidget;
import dta.sfmflow.api.client.widget.ApiWidgetAdapter;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.client.screen.widgets.NodeSettingsOverlay;
import com.example.addon.components.CustomNodeComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CustomSettingsOverlay extends NodeSettingsOverlay {
    public CustomSettingsOverlay(ManagerScreen parentScreen, CustomNodeComponent component) {
        super(parentScreen, component);
        
        // Define modal dimensions
        this.width = 150;
        this.height = 100;
        
        // Center the modal symmetrically
        this.setX((parentScreen.width - this.width) / 2);
        this.setY((parentScreen.height - this.height) / 2);

        // Standard vanilla UI buttons can be wrapped inside ApiWidgetAdapter
        Button cycleButton = Button.builder(Component.literal("Toggle Mode"), btn -> {
            // Modify local component settings
        }).bounds(getX() + 20, getY() + 30, 110, 20).build();

        this.children.add(new ApiWidgetAdapter<>(cycleButton));
    }
}
```

---

## Network Synchronization Guidelines

To save modified configurations on the server, dispatch a serverbound `SaveComponentSettings` packet payload when a user concludes dragging a slider, clicks a button, or scrolls:

```java
private void sendSettingsUpdate() {
    CompoundTag nbt = new CompoundTag();
    this.component.saveData(nbt); // Serializes current properties through component Codec

    BlockPos managerPos = this.parentScreen.getMenu().getManagerBlockEntity().getBlockPos();
    PacketDistributor.sendToServer(new SaveComponentSettings(managerPos, this.component.getId(), nbt));
}
```

### Slider Optimization
To prevent network congestion from excessive packets during active drag operations:
*   **Do not** send packet payloads inside your slider's `applyValue()` method.
*   **Only** dispatch `SaveComponentSettings` when the user releases the mouse button (`mouseReleased`) or performs a discrete step change (`mouseScrolled`). Disable default click sounds (`playDownSound`) on sliders for a cleaner drag experience.