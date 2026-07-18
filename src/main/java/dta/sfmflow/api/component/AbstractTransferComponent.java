package dta.sfmflow.api.component;

import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Base abstract class consolidating direction, target-binding, and face logic for transfers.
 */
public abstract class AbstractTransferComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable {
	protected final boolean isInput;
	protected int inventoryId = -1;
	protected int activeSidesMask = 0;

	protected AbstractTransferComponent(UUID uuid, boolean isInput) {
		super(uuid);
		this.isInput = isInput;
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 1; 
	}

	public boolean isInput() {
		return isInput;
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

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
	}
}