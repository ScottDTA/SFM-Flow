package dta.sfmflow.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import dta.sfmflow.registry.ModTags;

/**
 * Public API class representing an extensible physical network capability [3].
 * Replaces hardcoded capabilities with a registry-based pattern [3].
 */
public final class FlowCapability<T> {
	private final ResourceLocation id;
	private final @Nullable BlockCapability<T, @Nullable Direction> capability;
	private final String translationKey;

	public FlowCapability(ResourceLocation id, @Nullable BlockCapability<T, @Nullable Direction> capability,
			String translationKey) {
		this.id = id;
		this.capability = capability;
		this.translationKey = translationKey;
	}

	public ResourceLocation getId() {
		return id;
	}

	public @Nullable BlockCapability<T, @Nullable Direction> getCapability() {
		return capability;
	}

	public String getTranslationKey() {
		return translationKey;
	}

	/**
	 * Dynamic presence check that evaluates if this capability is exposed by the
	 * block state [3].
	 */
	public boolean isPresent(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be,
			@Nullable Direction side) {
		if (id.getPath().equals("redstone")) {
			return state.is(ModTags.REDSTONE_CABLES);
		}
		if (capability == null) {
			return false;
		}
		if (level.getCapability(capability, pos, state, be, side) != null) {
			return true;
		}
		return SpecialBlockCapabilityRegistry.getCapability(capability, level, pos, state, side) != null;
	}

	/**
	 * Symmetrically checks all six directions and the non-directional side context
	 * [3].
	 */
	public boolean isPresentAnywhere(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
		if (this.isPresent(level, pos, state, be, null)) {
			return true;
		}
		for (Direction dir : Direction.values()) {
			if (this.isPresent(level, pos, state, be, dir)) {
				return true;
			}
		}
		return false;
	}
}