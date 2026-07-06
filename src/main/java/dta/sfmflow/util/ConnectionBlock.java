package dta.sfmflow.util;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;

/**
 * Declares scannable target inventory indices mapped to physical coordinates
 * [3].
 */
public class ConnectionBlock implements IContainerSelection {
	private Level level;
	private BlockPos blockPos;
	private int cableDistance;
	private Set<ResourceLocation> types;
	private int id;
	private boolean sleeping = false;

	private BlockCapabilityCache<IItemHandler, Direction> itemCache;
	private BlockCapabilityCache<IFluidHandler, Direction> fluidCache;

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

	public void setItemCache(BlockCapabilityCache<IItemHandler, Direction> itemCache) {
		this.itemCache = itemCache;
	}

	public void setFluidCache(BlockCapabilityCache<IFluidHandler, Direction> fluidCache) {
		this.fluidCache = fluidCache;
	}

	/**
	 * Resolves the localized display name of this block coordinate safely and
	 * appends coordinates [3].
	 *
	 * @param level level context [3]
	 * @return localized display text with block position info [3]
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

	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		if (this.sleeping) {
			return null;
		}
		IItemHandler handler = null;
		if (this.itemCache != null) {
			try {
				handler = this.itemCache.getCapability();
			} catch (IllegalStateException e) {
			}
		}
		if (handler == null && this.level != null) {
			// Consult the capability bridge fallback [3]
			handler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.ItemHandler.BLOCK, this.level,
					this.blockPos, this.level.getBlockState(this.blockPos), side);
		}
		return handler;
	}

	public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (this.sleeping) {
			return null;
		}
		IFluidHandler handler = null;
		if (this.fluidCache != null) {
			try {
				handler = this.fluidCache.getCapability();
			} catch (IllegalStateException e) {
			}
		}
		if (handler == null && this.level != null) {
			// Consult the capability bridge fallback [3]
			handler = SpecialBlockCapabilityRegistry.getCapability(Capabilities.FluidHandler.BLOCK, this.level,
					this.blockPos, this.level.getBlockState(this.blockPos), side);
		}
		return handler;
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