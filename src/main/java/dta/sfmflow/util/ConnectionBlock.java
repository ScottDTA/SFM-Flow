package dta.sfmflow.util;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Declares scannable target inventory indices mapped to physical coordinates
 * [3]. Upgraded to cache NeoForge BlockCapabilityCache references on the server
 * [3].
 */
public class ConnectionBlock implements IContainerSelection {
	private BlockPos blockPos;
	private int cableDistance;
	private EnumSet<ConnectionBlockType> types;
	private int id;
	private boolean sleeping = false;

	// NeoForge capability caches [3]
	private BlockCapabilityCache<IItemHandler, Direction> itemCache;
	private BlockCapabilityCache<IFluidHandler, Direction> fluidCache;

	public ConnectionBlock(BlockPos blockPos, int cableDistance) {
		this.blockPos = blockPos;
		this.cableDistance = cableDistance;
		types = EnumSet.noneOf(ConnectionBlockType.class);
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public void setTypes(EnumSet<ConnectionBlockType> caps) {
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

	public void setItemCache(BlockCapabilityCache<IItemHandler, Direction> itemCache) {
		this.itemCache = itemCache;
	}

	public void setFluidCache(BlockCapabilityCache<IFluidHandler, Direction> fluidCache) {
		this.fluidCache = fluidCache;
	}

	/**
	 * Retrieves the cached IItemHandler capability [3]. Defensively returns null if
	 * the target chunk unloads or the cache throws [3].
	 *
	 * @param side target block face [3]
	 * @return IItemHandler instance, or null if sleeping [3]
	 */
	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		if (this.sleeping || this.itemCache == null) {
			return null;
		}
		try {
			return this.itemCache.getCapability();
		} catch (IllegalStateException e) {
			return null; // Defensive catch to protect against off-thread unload cycles [3]
		}
	}

	/**
	 * Retrieves the cached IFluidHandler capability [3]. Defensively returns null
	 * if the target chunk unloads or the cache throws [3].
	 *
	 * @param side target block face [3]
	 * @return IFluidHandler instance, or null if sleeping [3]
	 */
	public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (this.sleeping || this.fluidCache == null) {
			return null;
		}
		try {
			return this.fluidCache.getCapability();
		} catch (IllegalStateException e) {
			return null; // Defensive catch to protect against off-thread unload cycles [3]
		}
	}

	@Override
	public int getId() {
		return 0;
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