# Network Protocol and UI Rendering

This guide outlines SFM-Flow's asynchronous execution kernel, ring buffer pipelines, clientbound delta strategy synchronization, and advanced UI rendering systems.

---

## Asynchronous Execution Kernel & Ring Buffer

SFM-Flow runs flowchart evaluations on an asynchronous daemon thread pool to prevent main-thread ticks from dropping. 

```
  [ Trigger Tick ] ───► ThreadSafeInventorySnapshot ───► FlowchartPlanningTask (Off-Thread)
                                                                    │
                                                         ExecutionRingBuffer (O(1))
                                                                    │
                                                       pollAndExecuteThrottled (Main-Thread)
```

1.  **Inventory Snapshot**: Evaluated strictly on the main server thread to prevent concurrency collisions, creating a deep copy snapshot (`ThreadSafeInventorySnapshot`) of connected inventories, including side-specific slot mappings.
2.  **Off-Thread Planning**: `FlowExecutionKernel` submits the planning task to run asynchronously. It processes nodes through a 1ms cooperative time-budget slice.
3.  **Circuit Breaker Protection**: If a flowchart exceeds a `1000-node` traversal threshold (e.g. from an infinite recursion loop), the circuit breaker trips, incrementing `planningBreakerTrips` and canceling the planning task to safeguard server stability.
4.  **Recyclable Ring Buffer**: Evaluated commands write to a Power-of-Two `ExecutionRingBuffer`. Standard bitwise masks are utilized instead of modulo operations to guarantee $O(1)$ garbage-free writes.
5.  **Main Thread Handoff**: Main server thread polls the ring buffer via `pollAndExecuteThrottled` to dispatch capability tasks within a microsecond budget (`maxExecutionBudgetUs`).

---

## Network Synchronization & Client Delta Strategy

SFM-Flow uses clientbound delta packets to surgically sync specific visual adjustments rather than re-transmitting entire canvas hierarchies.

### Polymorphic Delta Types (`SyncComponentDeltaPacket`)
Sent S2C when node parameters or layouts are altered:
*   `MOVE`: Synchronizes relative canvas coordinates and sorting layers.
*   `NAME_COLOR`: Updates custom name strings and gradient color masks.
*   `SETTINGS`: Saves and applies serialized component codecs configurations.
*   `ADD`: Dynamically instantiates a newly spawned node on the client interface.
*   `REMOVE`: Deletes a node and any of its associated wires on the client.

### Client Delta Strategy Routing (`IDeltaStrategy`)
Clientbound delta packets are routed through pre-registered strategy handlers (`ClientDeltaRegistry`), preventing redundant screen rebuilds:

```java
// Client-side strategy handler for MOVE events
STRATEGIES.put(DeltaType.MOVE, (screen, packet, localComponent) -> {
    if (localComponent != null && packet.data() != null) {
        localComponent.setX(packet.data().getInt("x"));
        localComponent.setY(packet.data().getInt("y"));
        localComponent.setZ(packet.data().getInt("z"));
        // Surgically updates container widget positions instantly
    }
});
```

---

## Advanced UI Rendering Systems

### 1. Vertex-Colored Gradient Blitting (`GradientBlitUtil`)
Visual compact cards use vertex-colored blitting to render subtle, blended gradients directly over node backgrounds (`component_min_bg.png`):

```java
// Mixes 75% of mask color with 25% white to keep text readable
float mixedR = (r * 0.75F) + 0.25F;
float topR = Math.min(1.0F, mixedR * 1.15F); // Shaded top gradient
float botR = Math.min(1.0F, mixedR * 0.75F); // Shaded bottom gradient

// Draw quad vertices with vertical color transitions
bufferBuilder.addVertex(matrix, x, y, 0.0F).setUv(minU, minV).setColor(topR, topG, topB, 1.0F);
bufferBuilder.addVertex(matrix, x, y + height, 0.0F).setUv(minU, maxV).setColor(botR, botG, botB, 1.0F);
```

### 2. Bezier Wire Rendering (`VectorWireRenderer`)
Connection wires between top input pins and bottom output pins are drawn as smooth, orthogonal cubic Bezier curves (S-curves) using flat $0.0\text{F}$ matrix translations to render directly on the background canvas:

$$\mathbf{B}(t) = (1-t)^3\mathbf{P}_0 + 3(1-t)^2t\mathbf{P}_1 + 3(1-t)t^2\mathbf{P}_2 + t^3\mathbf{P}_3, \quad t \in [0, 1]$$

Where:
*   $\mathbf{P}_0$: Source Output Pin coordinate.
*   $\mathbf{P}_1$: Outbound control anchor point ($\mathbf{P}_0$ translated down along the Y-axis by a calculated deltaY).
*   $\mathbf{P}_2$: Inbound control anchor point ($\mathbf{P}_3$ translated up along the Y-axis by deltaY).
*   $\mathbf{P}_3$: Destination Input Pin coordinate.

### 3. OpenGL Hardware Scissor Masking
To prevent scrolling card selections or submenus from rendering outside their panels, hardware scissors crop rendering operations:

```java
// Clamp rendering within list coordinates boundaries
guiGraphics.enableScissor(listX, listY, listX + 260, listY + 18);
// Render items...
guiGraphics.flush(); // Flush draws before disabling scissor mask
guiGraphics.disableScissor();
```

### 4. Hardware Depth Testing & Layering
To render overlapping nodes cleanly, the rendering loop translates the projection matrix by `getZ() * 1.0F` and flushes the buffer source:

```java
guiGraphics.pose().pushPose();
guiGraphics.pose().translate(0.0F, 0.0F, this.getZ() * 1.0F);

super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
guiGraphics.flush(); // Flushes batch immediately to write to hardware depth buffer

guiGraphics.pose().popPose();
```