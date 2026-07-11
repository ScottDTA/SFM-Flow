package dta.sfmflow.block;

import java.util.function.Supplier;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry manager managing the instantiation and binding of blocks and block
 * items [3].
 */
public class ModBlocks {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SFMFlow.MODID);

	public static final DeferredBlock<Block> MANAGER_BLOCK = registerBlock("manager_block",
			() -> new ManagerBlock(BlockBehaviour.Properties.of().strength(5.0f, 1200.0f).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> CABLE_BLOCK = registerBlock("cable_block",
			() -> new CableBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> HARDENED_CABLE_BLOCK = registerBlock("hardened_cable_block",
			() -> new HardenedCableBlock(BlockBehaviour.Properties.of().destroyTime(15.0F).explosionResistance(1200.0F)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> REDSTONE_EMITTER_BLOCK = registerBlock("redstone_emitter_block",
			() -> new RedstoneEmitterBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)
					.isRedstoneConductor((state, level, pos) -> false))); // Configured as a non-conductor [3]

	public static final DeferredBlock<Block> REDSTONE_RECEIVER_BLOCK = registerBlock("redstone_receiver_block",
			() -> new RedstoneReceiverBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)
					.isRedstoneConductor((state, level, pos) -> false))); // Configured as a non-conductor [3]

	public static final DeferredBlock<Block> OBSERVER_CABLE_BLOCK = registerBlock("observer_cable_block",
			() -> new ObserverCableBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> ITEM_EJECTOR_HATCH_BLOCK = registerBlock("item_ejector_hatch_block",
			() -> new ItemEjectorHatchBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> ITEM_VACUUM_HATCH_BLOCK = registerBlock("item_vacuum_hatch_block",
			() -> new ItemVacuumHatchBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> FLUID_HATCH_CABLE_BLOCK = registerBlock("fluid_hatch_cable_block",
			() -> new FluidHatchCableBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> CABLE_CLUSTER_BLOCK = registerBlock("cable_cluster_block",
			() -> new CableClusterBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	public static final DeferredBlock<Block> ADVANCED_CABLE_CLUSTER_BLOCK = registerBlock(
			"advanced_cable_cluster_block",
			() -> new CableClusterBlock(BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.METAL)
					.requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

	private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
		DeferredBlock<T> toReturn = BLOCKS.register(name, block);
		registerBlockItem(name, toReturn);
		return toReturn;
	}

	private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
		ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
	}

	public static void register(IEventBus eventBus) {
		BLOCKS.register(eventBus);
	}
}