package dta.sfmflow.block.entity;

import java.util.function.Supplier;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry manager managing the instantiation and binding of BlockEntityTypes.
 */
public class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
			.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SFMFlow.MODID);

	public static final Supplier<BlockEntityType<ManagerBlockEntity>> MANAGER_BE = BLOCK_ENTITIES.register("manager_be",
			() -> BlockEntityType.Builder.of(ManagerBlockEntity::new, ModBlocks.MANAGER_BLOCK.get()).build(null));

	public static final Supplier<BlockEntityType<RedstoneEmitterBlockEntity>> REDSTONE_EMITTER_BE = BLOCK_ENTITIES
			.register("redstone_emitter_be", () -> BlockEntityType.Builder
					.of(RedstoneEmitterBlockEntity::new, ModBlocks.REDSTONE_EMITTER_BLOCK.get()).build(null));

	public static final Supplier<BlockEntityType<RedstoneReceiverBlockEntity>> REDSTONE_RECEIVER_BE = BLOCK_ENTITIES
			.register("redstone_receiver_be", () -> BlockEntityType.Builder
					.of(RedstoneReceiverBlockEntity::new, ModBlocks.REDSTONE_RECEIVER_BLOCK.get()).build(null));

	public static final Supplier<BlockEntityType<ItemEjectorValveBlockEntity>> ITEM_EJECTOR_HATCH_BE = BLOCK_ENTITIES
			.register("item_ejector_hatch_be", () -> BlockEntityType.Builder
					.of(ItemEjectorValveBlockEntity::new, ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get()).build(null));

	public static final Supplier<BlockEntityType<ItemVacuumValveBlockEntity>> ITEM_VACUUM_HATCH_BE = BLOCK_ENTITIES
			.register("item_vacuum_hatch_be", () -> BlockEntityType.Builder
					.of(ItemVacuumValveBlockEntity::new, ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get()).build(null));

	public static final Supplier<BlockEntityType<FluidEjectorValveBlockEntity>> FLUID_EJECTOR_VALVE_BE = BLOCK_ENTITIES
			.register("fluid_ejector_valve_be", () -> BlockEntityType.Builder
					.of(FluidEjectorValveBlockEntity::new, ModBlocks.FLUID_EJECTOR_VALVE_BLOCK.get()).build(null));

	public static final Supplier<BlockEntityType<FluidVacuumValveBlockEntity>> FLUID_VACUUM_VALVE_BE = BLOCK_ENTITIES
			.register("fluid_vacuum_valve_be", () -> BlockEntityType.Builder
					.of(FluidVacuumValveBlockEntity::new, ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get()).build(null));

	public static final Supplier<BlockEntityType<CableClusterBlockEntity>> CABLE_CLUSTER_BE = BLOCK_ENTITIES
			.register("cable_cluster_be", () -> BlockEntityType.Builder.of(CableClusterBlockEntity::new,
					ModBlocks.CABLE_CLUSTER_BLOCK.get(), ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get()).build(null));

	public static void register(IEventBus eventBus) {
		BLOCK_ENTITIES.register(eventBus);
	}
}
