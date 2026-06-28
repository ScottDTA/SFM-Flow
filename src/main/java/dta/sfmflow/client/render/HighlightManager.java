package dta.sfmflow.client.render;

import dta.sfmflow.SFMFlow;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.literal;

/**
 * Client-only visual highlighting manager outlining targeted inventories
 * in-world [3]. Outlines bypass depth buffers to be clearly visible through
 * solid terrain walls [3].
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = SFMFlow.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class HighlightManager {
	private static final Set<BlockPos> ACTIVE_HIGHLIGHTS = ConcurrentHashMap.newKeySet();

	public static final KeyMapping CLEAR_HIGHLIGHTS_KEY = new KeyMapping("key.sfmflow.clear_highlights",
			com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_H, // Default key bind
																									// is H [3]
			"key.categories.sfmflow");

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
	 * Listens to client setup on the mod bus to register our custom keybind [3].
	 */
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(CLEAR_HIGHLIGHTS_KEY);
	}

	/**
	 * Listens to client ticks on the game event bus to detect keypress clear events
	 * [3].
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
	 * outlines [3].
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
	 * terrain [3].
	 */
	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			if (ACTIVE_HIGHLIGHTS.isEmpty()) {
				return;
			}

			com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
			net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance()
					.renderBuffers().bufferSource();
			com.mojang.blaze3d.vertex.VertexConsumer buffer = bufferSource
					.getBuffer(net.minecraft.client.renderer.RenderType.lines());

			net.minecraft.client.Camera camera = event.getCamera();
			Vec3 camPos = camera.getPosition();

			com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
			com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
			com.mojang.blaze3d.systems.RenderSystem.lineWidth(2.5F);

			for (BlockPos pos : ACTIVE_HIGHLIGHTS) {
				poseStack.pushPose();
				poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

				// Draw a gold wireframe outline centered over the block position [3]
				net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, buffer, 0.0, 0.0, 0.0, 1.002,
						1.002, 1.002, 0.85F, 0.7F, 0.2F, 1.0F);

				poseStack.popPose();
			}

			// Immediately flush the specific line renderer batch while depth test is
			// disabled [3]
			bufferSource.endBatch(net.minecraft.client.renderer.RenderType.lines());

			com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
			com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
			com.mojang.blaze3d.systems.RenderSystem.lineWidth(1.0F);
		}
	}
}