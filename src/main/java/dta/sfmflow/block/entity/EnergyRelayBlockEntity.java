package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Backing BlockEntity for the Energy Relay block. Exposes proximal entity energy storage dynamically.
 */
public class EnergyRelayBlockEntity extends BlockEntity {

	public EnergyRelayBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ENERGY_RELAY_BE.get(), pos, state);
	}

	/**
	 * Resolves any non-player entity energy handler adjacent to this block on the specified side.
	 */
	public @Nullable IEnergyStorage getEnergyHandler(@Nullable Direction side) {
		if (this.level == null || side == null) {
			return null;
		}

		BlockPos targetPos = this.worldPosition.relative(side);
		AABB aabb = new AABB(targetPos).inflate(0.2D);
		List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, aabb, entity -> !(entity instanceof Player));

		for (Entity entity : entities) {
			// Pass null as the context parameter to satisfy the Void/Direction type signature
			IEnergyStorage handler = entity.getCapability(Capabilities.EnergyStorage.ENTITY, null);
			if (handler != null) {
				return handler;
			}
		}

		return null;
	}
}