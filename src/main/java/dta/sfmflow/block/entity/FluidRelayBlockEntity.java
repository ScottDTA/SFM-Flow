package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Backing BlockEntity for the Fluid Relay block. Exposes proximal entity fluid handlers dynamically.
 */
public class FluidRelayBlockEntity extends BlockEntity {

	public FluidRelayBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_RELAY_BE.get(), pos, state);
	}

	/**
	 * Resolves any non-player entity fluid handler adjacent to this block on the specified side.
	 */
	public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (this.level == null || side == null) {
			return null;
		}

		BlockPos targetPos = this.worldPosition.relative(side);
		AABB aabb = new AABB(targetPos).inflate(0.2D);
		List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, aabb, entity -> !(entity instanceof Player));

		for (Entity entity : entities) {
			// Pass null as the context parameter to satisfy the Void/Direction type signature
			IFluidHandler handler = entity.getCapability(Capabilities.FluidHandler.ENTITY, null);
			if (handler != null) {
				return handler;
			}
		}

		return null;
	}
}