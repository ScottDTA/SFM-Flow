package dta.sfmflow.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Public API callback executing a physical capability transfer on the main server thread [3].
 */
@FunctionalInterface
public interface FlowCapabilityTransfer {
	/**
	 * Executes the transfer task [3].
	 *
	 * @param level      the level context [3]
	 * @param src        source position [3]
	 * @param srcSide    source facing context [3]
	 * @param dest       destination position [3]
	 * @param destSide   destination facing context [3]
	 * @param taskParams additional dynamic parameters associated with the task [3]
	 * @return true if the transfer was successful, false otherwise [3]
	 */
	boolean execute(Level level, BlockPos src, @Nullable Direction srcSide, BlockPos dest, @Nullable Direction destSide, Object taskParams);
}
