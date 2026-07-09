package dta.sfmflow.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;

/**
 * Declares scannable target inventory indices mapped to physical coordinates [3].
 */
public class ConnectionBlock implements IContainerSelection {
	private Level level;
	private BlockPos blockPos;
	private int cableDistance;
	private Set<ResourceLocation> types;
	private int id;
	private boolean sleeping = false;

	public record SidedCacheKey(ResourceLocation capId, @Nullable Direction side) {}

	// Sided capability cache matrix to track and invalidate face settings independently [3]
	private final Map<SidedCacheKey, BlockCapabilityCache<?, Direction>> capabilityCaches = new HashMap<>();

	public ConnectionBlock(BlockPos blockPos, int cableDistance) {
		this.blockPos = blockPos;
		this.cableDistance = cableDistance;
		types = new HashSet<>();
	}

	public ConnectionBlock(Level level, BlockPos blockPos, int cableDistance) {
		this(blockPos, cableDistance);
		this.level = level;
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public void setTypes(Set<ResourceLocation> caps) {
		types = caps;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isSleeping() {
		return this.sleeping;
	}

	public void setSleeping(boolean sleeping) {
		this.sleeping = sleeping;
	}

	public int getCableDistance() {
		return this.cableDistance;
	}

	public Set<ResourceLocation> getTypes() {
		return this.types;
	}

	/**
	 * Registers a dynamically allocated BlockCapabilityCache under its capability
	 * registry ID and side context [3].
	 */
	public void registerCache(ResourceLocation capId, @Nullable Direction side, BlockCapabilityCache<?, Direction> cache) {
		this.capabilityCaches.put(new SidedCacheKey(capId, side), cache);
	}

	/**
	 * Safely retrieves an active capability handler, utilizing the dynamic caches
	 * on demand with automatic single-threaded level lookups as an invalidation
	 * fallback [3].
	 */
	@SuppressWarnings("unchecked")
	public @Nullable <T> T getHandler(ResourceLocation capId, Class<T> clazz, @Nullable Direction side) {
		if (this.sleeping) {
			return null;
		}

		BlockCapabilityCache<?, Direction> cache = this.capabilityCaches.get(new SidedCacheKey(capId, side));
		if (cache != null) {
			try {
				Object handler = cache.getCapability();
				if (clazz.isInstance(handler)) {
					return clazz.cast(handler);
				}
			} catch (IllegalStateException e) {
				// Fallback dynamically if the cache is temporarily invalidated [3]
			}
		}

		// Live lookup fallback for dynamically loaded components and special capability
		// bridges [3]
		var flowCap = FlowCapabilityRegistry.get(capId);
		if (flowCap != null && flowCap.getCapability() != null && this.level != null) {
			Object handler = this.level.getCapability((BlockCapability<Object, Direction>) flowCap.getCapability(),
					this.blockPos, side);
			if (handler == null) {
				handler = SpecialBlockCapabilityRegistry.getCapability(
						(BlockCapability<Object, Direction>) flowCap.getCapability(), this.level, this.blockPos,
						this.level.getBlockState(this.blockPos), side);
			}
			if (clazz.isInstance(handler)) {
				return clazz.cast(handler);
			}
		}
		return null;
	}

	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		return getHandler(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"), IItemHandler.class, side);
	}

	public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
		return getHandler(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"), IFluidHandler.class, side);
	}

	/**
	 * Resolves the localized display name of this block coordinate safely and
	 * appends coordinates [3].
	 */
	public Component getDisplayName(Level level) {
		Component baseName;
		if (level != null) {
			var state = level.getBlockState(this.blockPos);
			if (!state.isAir()) {
				baseName = state.getBlock().getName();
			} else {
				baseName = Component.literal("Inventory #" + this.id);
			}
		} else {
			baseName = Component.literal("Inventory #" + this.id);
		}
		return Component.literal(baseName.getString() + " at (" + this.blockPos.getX() + ", " + this.blockPos.getY()
				+ ", " + this.blockPos.getZ() + ")");
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public boolean isVariable() {
		return false;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}
}