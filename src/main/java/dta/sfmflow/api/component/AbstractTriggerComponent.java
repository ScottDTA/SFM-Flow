package dta.sfmflow.api.component;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Public API base class representing a trigger component in the flowchart logic
 * workspace [3]. Configures common single-output visual and logic behavior by
 * default [3].
 */
public abstract class AbstractTriggerComponent extends AbstractFlowComponent {
	public AbstractTriggerComponent(UUID uuid) {
		super(uuid);
		this.hasOutputNodes = true;
		this.numOutputs = 1;
	}

	/**
	 * Evaluates if this trigger is active and should fire on the current server tick [3].
	 * Encapsulates the trigger's internal timing, checks, and state mutations [3].
	 *
	 * @param level    the level context [3]
	 * @param pos      the position of the managing block entity [3]
	 * @param gameTime the current game time in ticks [3]
	 * @return true if the trigger conditions are met and the flowchart path should be enqueued [3]
	 */
	public abstract boolean evaluateTrigger(Level level, BlockPos pos, long gameTime);
}