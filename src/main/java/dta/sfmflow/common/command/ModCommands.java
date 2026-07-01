package dta.sfmflow.common.command;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Serverbound diagnostic command suite allowing administrators to audit active blocks [3].
 */
@EventBusSubscriber(modid = SFMFlow.MODID)
public final class ModCommands {

	private ModCommands() {}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("sfmflow")
			.requires(source -> source.hasPermission(2)) // Require administrator access level
			.then(Commands.literal("profile")
				.executes(context -> {
					var source = context.getSource();

					source.sendSystemMessage(Component.literal("=== SFM-Flow Active Profiler ===")
							.withStyle(ChatFormatting.GOLD));

					int count = 0;
					/* STREAMING_CHUNK:Scanning tracked manager instances */
					// Query our side-safe high-performance tracking list directly [3]
					for (ManagerBlockEntity manager : ManagerBlockEntity.getActiveManagers()) {
						Level level = manager.getLevel();
						if (level == null) {
							continue;
						}

						count++;
						String dimensionKey = level.dimension().location().toString();
						double avgTime = manager.getAverageExecutionTimeUs();
						double avgTasks = manager.getAverageTasksPerTick();
						int backlog = manager.getBufferBacklog();
						int trips = manager.getBreakerTrips();
						BlockPos pos = manager.getBlockPos();

						MutableComponent statsText = Component.literal(String.format(
								" %-12s @ [%d, %d, %d] -> Avg: %-6sµs | %-5s tasks | Backlog: %-4d | Trips: %-3d ",
								level.dimension().location().getPath(),
								pos.getX(), pos.getY(), pos.getZ(),
								String.format("%.2f", avgTime),
								String.format("%.2f", avgTasks),
								backlog,
								trips
						)).withStyle(ChatFormatting.GRAY);

						/* STREAMING_CHUNK:Stylized teleport action */
						// Stylized, clickable link teleporting administrators across dimensions instantly [3]
						MutableComponent tpLink = Component.literal("[Teleport]")
								.withStyle(style -> style
										.withColor(ChatFormatting.AQUA)
										.withUnderlined(true)
										.withClickEvent(new ClickEvent(
												ClickEvent.Action.RUN_COMMAND,
												String.format("/execute in %s run tp @s %d %d %d", dimensionKey, pos.getX(), pos.getY(), pos.getZ())
												))
										.withHoverEvent(new HoverEvent(
												HoverEvent.Action.SHOW_TEXT,
												Component.literal("Click to teleport directly to this Manager Block Entity [3]")
												)));

						source.sendSystemMessage(statsText.append(tpLink));
					}

					if (count == 0) {
						source.sendSystemMessage(Component.literal("No active Manager Block Entities loaded in the world.")
								.withStyle(ChatFormatting.RED));
					}
					return 1;
				})
			)
		);
	}
}
