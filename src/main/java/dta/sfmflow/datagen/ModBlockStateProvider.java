package dta.sfmflow.datagen;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Automates the creation of blockstate json and block/item model files.
 */
public class ModBlockStateProvider extends BlockStateProvider {
	public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, SFMFlow.MODID, exFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		BlockModelBuilder managerModel = models().cubeBottomTop(ModBlocks.MANAGER_BLOCK.getRegisteredName(),
				modLoc("block/manager_side"), modLoc("block/manager_bot"), modLoc("block/manager_top"));
		simpleBlock(ModBlocks.MANAGER_BLOCK.get(), managerModel.texture("particle", "#top"));
		simpleBlockItem(ModBlocks.MANAGER_BLOCK.get(), managerModel);

		simpleBlock(ModBlocks.CABLE_BLOCK.get());
		simpleBlockItem(ModBlocks.CABLE_BLOCK.get(), models().cubeAll("cable_block", modLoc("block/cable_block")));

		simpleBlock(ModBlocks.HARDENED_CABLE_BLOCK.get());
		simpleBlockItem(ModBlocks.HARDENED_CABLE_BLOCK.get(),
				models().cubeAll("hardened_cable_block", modLoc("block/hardened_cable_block")));

		simpleBlock(ModBlocks.REDSTONE_RECEIVER_BLOCK.get(),
				models().cubeAll("redstone_receiver_block", modLoc("block/redstone_receiver_block")));
		simpleBlockItem(ModBlocks.REDSTONE_RECEIVER_BLOCK.get(),
				models().cubeAll("redstone_receiver_block", modLoc("block/redstone_receiver_block")));

		// Configured the Item Ejector Valve to use its dedicated front-face texture
		// orientably with custom rotations
		BlockModelBuilder ejectorModel = models().orientable("item_ejector_valve_block",
				modLoc("block/item_ejector_valve_block"), modLoc("block/item_ejector_valve_block_front"),
				modLoc("block/item_ejector_valve_block"));
		directionalBlockFacingNorth(ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get(), ejectorModel);
		simpleBlockItem(ModBlocks.ITEM_EJECTOR_VALVE_BLOCK.get(), ejectorModel);

		// Configured Item Vacuum Valve to use its dedicated front-face texture
		// orientably with custom rotations
		BlockModelBuilder vacuumModel = models().orientable("item_vacuum_valve_block",
				modLoc("block/item_vacuum_valve_block"), modLoc("block/item_vacuum_valve_block_front"),
				modLoc("block/item_vacuum_valve_block"));
		directionalBlockFacingNorth(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get(), vacuumModel);
		simpleBlockItem(ModBlocks.ITEM_VACUUM_VALVE_BLOCK.get(), vacuumModel);

		// Configured Fluid Ejector Valve to use its dedicated front-face texture orientably with custom rotations [3]
				BlockModelBuilder fluidEjectorModel = models().orientable("fluid_ejector_valve_block",
						modLoc("block/fluid_ejector_valve_block"),
						modLoc("block/fluid_ejector_valve_block_front"),
						modLoc("block/fluid_ejector_valve_block"));
				directionalBlockFacingNorth(ModBlocks.FLUID_EJECTOR_VALVE_BLOCK.get(), fluidEjectorModel);
				simpleBlockItem(ModBlocks.FLUID_EJECTOR_VALVE_BLOCK.get(), fluidEjectorModel);

				// Configured Fluid Vacuum Valve to use its dedicated front-face texture orientably with custom rotations [3]
				BlockModelBuilder fluidVacuumModel = models().orientable("fluid_vacuum_valve_block",
						modLoc("block/fluid_vacuum_valve_block"),
						modLoc("block/fluid_vacuum_valve_block_front"),
						modLoc("block/fluid_vacuum_valve_block"));
				directionalBlockFacingNorth(ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get(), fluidVacuumModel);
				simpleBlockItem(ModBlocks.FLUID_VACUUM_VALVE_BLOCK.get(), fluidVacuumModel);
				
		// Standard Cable Cluster
		simpleBlock(ModBlocks.CABLE_CLUSTER_BLOCK.get(),
				models().cubeAll("cable_cluster_block", modLoc("block/cable_block")));
		simpleBlockItem(ModBlocks.CABLE_CLUSTER_BLOCK.get(),
				models().cubeAll("cable_cluster_block", modLoc("block/cable_block")));

		// Advanced Cable Cluster
		simpleBlock(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get(),
				models().cubeAll("advanced_cable_cluster_block", modLoc("block/hardened_cable_block")));
		simpleBlockItem(ModBlocks.ADVANCED_CABLE_CLUSTER_BLOCK.get(),
				models().cubeAll("advanced_cable_cluster_block", modLoc("block/hardened_cable_block")));

		BlockModelBuilder faceOffModel = models()
				.withExistingParent("block/redstone_emitter_face_off", mcLoc("block/block"))
				.texture("particle", modLoc("block/redstone_emitter_side_off"))
				.texture("face", modLoc("block/redstone_emitter_side_off"));
		faceOffModel.element().from(0, 0, 0).to(16, 16, 16).face(Direction.UP).uvs(0, 0, 16, 16).texture("#face")
				.cullface(Direction.UP).end();

		BlockModelBuilder faceOnModel = models()
				.withExistingParent("block/redstone_emitter_face_on", mcLoc("block/block"))
				.texture("particle", modLoc("block/redstone_emitter_side_on"))
				.texture("face", modLoc("block/redstone_emitter_side_on"));
		faceOnModel.element().from(0, 0, 0).to(16, 16, 16).face(Direction.UP).uvs(0, 0, 16, 16).texture("#face")
				.cullface(Direction.UP).end();

		BlockModelBuilder inventoryModel = models().cubeAll("block/redstone_emitter_inventory",
				modLoc("block/redstone_emitter_side_off"));
		simpleBlockItem(ModBlocks.REDSTONE_EMITTER_BLOCK.get(), inventoryModel);

		var emitterBuilder = getMultipartBuilder(ModBlocks.REDSTONE_EMITTER_BLOCK.get());

		emitterBuilder.part().modelFile(faceOffModel).rotationX(180).addModel().condition(BlockStateProperties.DOWN,
				false);
		emitterBuilder.part().modelFile(faceOnModel).rotationX(180).addModel().condition(BlockStateProperties.DOWN,
				true);

		emitterBuilder.part().modelFile(faceOffModel).addModel().condition(BlockStateProperties.UP, false);
		emitterBuilder.part().modelFile(faceOnModel).addModel().condition(BlockStateProperties.UP, true);

		emitterBuilder.part().modelFile(faceOffModel).rotationX(90).addModel().condition(BlockStateProperties.NORTH,
				false);
		emitterBuilder.part().modelFile(faceOnModel).rotationX(90).addModel().condition(BlockStateProperties.NORTH,
				true);

		emitterBuilder.part().modelFile(faceOffModel).rotationX(270).addModel().condition(BlockStateProperties.SOUTH,
				false);
		emitterBuilder.part().modelFile(faceOnModel).rotationX(270).addModel().condition(BlockStateProperties.SOUTH,
				true);

		emitterBuilder.part().modelFile(faceOffModel).rotationX(90).rotationY(270).addModel()
				.condition(BlockStateProperties.WEST, false);
		emitterBuilder.part().modelFile(faceOnModel).rotationX(90).rotationY(270).addModel()
				.condition(BlockStateProperties.WEST, true);

		emitterBuilder.part().modelFile(faceOffModel).rotationX(90).rotationY(90).addModel()
				.condition(BlockStateProperties.EAST, false);
		emitterBuilder.part().modelFile(faceOnModel).rotationX(90).rotationY(90).addModel()
				.condition(BlockStateProperties.EAST, true);

		var observerModel = models().cube("block/observer_cable_block", modLoc("block/observer_cable_top"), // down
																											// (bottom)
				modLoc("block/observer_cable_top2"), // up (top)
				modLoc("block/observer_cable_front"), // north (front face)
				modLoc("block/observer_cable_back"), // south (back port)
				modLoc("block/observer_cable_side2"), // west (side)
				modLoc("block/observer_cable_side") // east (side)
		).texture("particle", modLoc("block/cable_block"));

		getVariantBuilder(ModBlocks.OBSERVER_CABLE_BLOCK.get()).forAllStates(state -> {
			Direction dir = state.getValue(BlockStateProperties.FACING);
			int rotX = 0;
			int rotY = 0;
			switch (dir) {
			case DOWN -> rotX = 90;
			case UP -> rotX = 270;
			case NORTH -> {
				rotX = 0;
				rotY = 0;
			}
			case SOUTH -> rotY = 180;
			case EAST -> rotY = 90;
			case WEST -> rotY = 270;
			}
			return ConfiguredModel.builder().modelFile(observerModel).rotationX(rotX).rotationY(rotY).uvLock(false)
					.build();
		});

		simpleBlockItem(ModBlocks.OBSERVER_CABLE_BLOCK.get(), observerModel);
	}

	/**
	 * Generates a blockstate with correct rotations for blocks whose default front
	 * texture faces NORTH.
	 */
	public void directionalBlockFacingNorth(Block block, ModelFile model) {
		getVariantBuilder(block).forAllStates(state -> {
			Direction dir = state.getValue(BlockStateProperties.FACING);
			int x = 0;
			int y = 0;
			switch (dir) {
			case DOWN -> x = 90;
			case UP -> x = 270;
			case NORTH -> {
				x = 0;
				y = 0;
			}
			case SOUTH -> y = 180;
			case EAST -> y = 90;
			case WEST -> y = 270;
			}
			return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
		});
	}
}