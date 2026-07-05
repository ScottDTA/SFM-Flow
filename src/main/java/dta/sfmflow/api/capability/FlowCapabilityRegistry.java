package dta.sfmflow.api.capability;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Side-safe public registry managing dynamic network capabilities and their
 * task executors [3].
 */
public final class FlowCapabilityRegistry {
	private static final Map<ResourceLocation, FlowCapability<?>> CAPABILITIES = new HashMap<>();
	private static final Map<ResourceLocation, FlowCapabilityTransfer> TRANSFERS = new HashMap<>();

	private FlowCapabilityRegistry() {
	}

	public static <T> void register(FlowCapability<T> capability) {
		if (capability != null) {
			CAPABILITIES.put(capability.getId(), capability);
		}
	}

	public static void registerTransfer(ResourceLocation id, FlowCapabilityTransfer transfer) {
		if (id != null && transfer != null) {
			TRANSFERS.put(id, transfer);
		}
	}

	@Nullable
	public static FlowCapability<?> get(ResourceLocation id) {
		return CAPABILITIES.get(id);
	}

	@Nullable
	public static FlowCapabilityTransfer getTransfer(ResourceLocation id) {
		return TRANSFERS.get(id);
	}

	public static Map<ResourceLocation, FlowCapability<?>> getRegisteredCapabilities() {
		return CAPABILITIES;
	}
}