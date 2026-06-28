package dta.sfmflow.client.render;

import dta.sfmflow.SFMFlow;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import static net.minecraft.commands.Commands.literal;

/**
 * Client-only visual highlighting manager outlining targeted inventories
 * in-world. Outlines bypass depth buffers to be clearly visible through solid
 * terrain walls.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = SFMFlow.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class HighlightManager {
	private static final Set<BlockPos> ACTIVE_HIGHLIGHTS = ConcurrentHashMap.newKeySet();

	public static final KeyMapping CLEAR_HIGHLIGHTS_KEY = new KeyMapping("key.sfmflow.clear_highlights",
			InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, // Default key bind is H [3]
			"key.categories.sfmflow");

	/**
	 * Custom RenderType built explicitly to disable depth testing. This forces
	 * Minecraft's rendering pipeline to draw our wireframe lines on top of all
	 * solid geometry.
	 */
	private static final RenderType THRU_WALLS_LINES = RenderType.create("thru_walls_lines",
			DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
			RenderType.CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader))
					.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.5D)))
					.setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519))
					.setCullState(new RenderStateShard.CullStateShard(false)).createCompositeState(false));

	private HighlightManager() {
	}

	public static void addHighlight(BlockPos pos) {
		if (pos != null) {
			ACTIVE_HIGHLIGHTS.add(pos);
		}
	}

	public static void removeHighlight(BlockPos pos) {
		if (pos != null) {
			ACTIVE_HIGHLIGHTS.remove(pos);
		}
	}

	public static void clearHighlights() {
		ACTIVE_HIGHLIGHTS.clear();
	}

	public static boolean isHighlighted(BlockPos pos) {
		return pos != null && ACTIVE_HIGHLIGHTS.contains(pos);
	}

	/**
	 * Listens to client setup on the mod bus to register our custom keybind.
	 */
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(CLEAR_HIGHLIGHTS_KEY);
	}

	/**
	 * Listens to client ticks on the game event bus to detect keypress clear events
	 */
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		while (CLEAR_HIGHLIGHTS_KEY.consumeClick()) {
			clearHighlights();
			if (Minecraft.getInstance().player != null) {
				Minecraft.getInstance().player
						.displayClientMessage(Component.literal("All in-world highlights cleared."), true);
			}
		}
	}

	/**
	 * Registers client-side slash commands allowing players to disable active
	 * outlines.
	 */
	@SubscribeEvent
	public static void registerClientCommands(RegisterClientCommandsEvent event) {
		event.getDispatcher()
				.register(literal("sfmflow").then(literal("highlight").then(literal("off").executes(context -> {
					clearHighlights();
					context.getSource().sendSystemMessage(Component.literal("All in-world highlights cleared."));
					return 1;
				})).then(literal("clear").executes(context -> {
					clearHighlights();
					context.getSource().sendSystemMessage(Component.literal("All in-world highlights cleared."));
					return 1;
				}))));
	}

	/**
	 * Intercepts in-world level rendering stages to draw wireframe outlines through
	 * terrain.
	 */
	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			if (ACTIVE_HIGHLIGHTS.isEmpty()) {
				return;
			}

			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) {
				return;
			}

			PoseStack poseStack = event.getPoseStack();

			OutlineBufferSource outlineBuffer = mc.renderBuffers().outlineBufferSource();

			outlineBuffer.setColor(216, 175, 55, 255);

			Camera camera = event.getCamera();
			Vec3 camPos = camera.getPosition();

			RenderSystem.depthMask(false);

			for (BlockPos pos : ACTIVE_HIGHLIGHTS) {
				BlockState state = mc.level.getBlockState(pos);
				if (state.isAir()) {
					continue;
				}

				VoxelShape shape = state.getShape(mc.level, pos);
				if (shape.isEmpty()) {
					continue;
				}

				poseStack.pushPose();

				VertexConsumer buffer = outlineBuffer.getBuffer(THRU_WALLS_LINES);

				double dx = (double) pos.getX() - camPos.x;
				double dy = (double) pos.getY() - camPos.y;
				double dz = (double) pos.getZ() - camPos.z;

				LevelRenderer.renderVoxelShape(poseStack, buffer, shape, dx, dy, dz, 0.85F, 0.7F, 0.2F, 1.0F, false);

				poseStack.popPose();
			}

			outlineBuffer.endOutlineBatch();

			RenderSystem.depthMask(true);
		}
	}
}