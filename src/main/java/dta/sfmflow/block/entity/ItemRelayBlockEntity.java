package dta.sfmflow.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Backing BlockEntity for the Item Relay block. Exposes proximal entity item handlers dynamically.
 */
public class ItemRelayBlockEntity extends BlockEntity {

	public ItemRelayBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ITEM_RELAY_BE.get(), pos, state);
	}

	/**
	 * Resolves any non-player entity item handler adjacent to this block on the specified side.
	 */
	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		if (this.level == null || side == null) {
			return null;
		}

		BlockPos targetPos = this.worldPosition.relative(side);
		AABB aabb = new AABB(targetPos).inflate(0.2D);
		List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, aabb, entity -> !(entity instanceof Player));

		for (Entity entity : entities) {
			// Pass null as the context parameter to satisfy the Void type signature
			IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, null);
			if (handler == null) {
				handler = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
			}
			
			if (handler != null) {
				return handler;
			}
		}

		return null;
	}
}