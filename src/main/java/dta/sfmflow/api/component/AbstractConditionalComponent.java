package dta.sfmflow.api.component;

import java.util.UUID;
import net.minecraft.core.Direction;

/**
 * Base abstract class consolidating target-binding and directional face logic for conditionals.
 */
public abstract class AbstractConditionalComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable {
	protected int inventoryId = -1;
	protected int activeSidesMask = 0;

	protected AbstractConditionalComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 2; 
	}

	@Override
	public int getInventoryId() {
		return inventoryId;
	}

	@Override
	public void setInventoryId(int inventoryId) {
		this.inventoryId = inventoryId;
	}

	@Override
	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	@Override
	public void toggleSide(Direction dir) {
		if (dir != null) {
			activeSidesMask ^= (1 << dir.ordinal());
		}
	}

	public int getActiveSidesMask() {
		return activeSidesMask;
	}

	public void setActiveSidesMask(int mask) {
		this.activeSidesMask = mask;
	}
}