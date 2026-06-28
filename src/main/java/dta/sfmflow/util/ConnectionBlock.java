package dta.sfmflow.util;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Declares scannable target inventory indices mapped to physical coordinates
 * [3].
 */
public class ConnectionBlock implements IContainerSelection {
	private BlockPos blockPos;
	private int cableDistance;
	private EnumSet<ConnectionBlockType> types;
	private int id;
	private boolean sleeping = false;

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

	public int getCableDistance() {
		return this.cableDistance;
	}

	public EnumSet<ConnectionBlockType> getTypes() {
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
		// Append coordinates directly inside display name to let player identify chest
		// locations [3]
		return Component.literal(baseName.getString() + " at (" + this.blockPos.getX() + ", " + this.blockPos.getY()
				+ ", " + this.blockPos.getZ() + ")");
	}

	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		if (this.sleeping || this.itemCache == null) {
			return null;
		}
		try {
			return this.itemCache.getCapability();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (this.sleeping || this.fluidCache == null) {
			return null;
		}
		try {
			return this.fluidCache.getCapability();
		} catch (IllegalStateException e) {
			return null;
		}
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