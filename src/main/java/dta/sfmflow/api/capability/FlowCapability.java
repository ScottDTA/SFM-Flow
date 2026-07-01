package dta.sfmflow.api.capability;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Public API class representing an extensible physical network capability [3].
 * Replaces hardcoded capabilities with a registry-based pattern [3].
 */
public final class FlowCapability<T> {
	private final ResourceLocation id;
	private final BlockCapability<T, @Nullable Direction> capability;
	private final String translationKey;

	public FlowCapability(ResourceLocation id, BlockCapability<T, @Nullable Direction> capability, String translationKey) {
		this.id = id;
		this.capability = capability;
		this.translationKey = translationKey;
	}

	public ResourceLocation getId() {
		return id;
	}

	public BlockCapability<T, @Nullable Direction> getCapability() {
		return capability;
	}

	public String getTranslationKey() {
		return translationKey;
	}
}
