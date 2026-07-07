package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Unified logic component handling both Forge Energy (FE) inputs (extractions) and energy
 * outputs (depositions) [3]. Supports NBT serialization for active side configurations [3].
 */
public class EnergyTransferComponent extends AbstractFlowComponent
		implements IInventoryTarget, ISideConfigurable {
	private final boolean isInput;
	private int inventoryId = -1;
	private int maxTransferAmount = 1000;
	private int activeSidesMask = 0;

	public static final MapCodec<EnergyTransferComponent> INPUT_CODEC = makeCodec(true);
	public static final MapCodec<EnergyTransferComponent> OUTPUT_CODEC = makeCodec(false);

	private static MapCodec<EnergyTransferComponent> makeCodec(boolean isInput) {
		return RecordCodecBuilder.mapCodec(instance -> instance
				.group(BaseProperties.CODEC.fieldOf("base").forGetter(EnergyTransferComponent::getBaseProperties),
						Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(EnergyTransferComponent::getInventoryId),
						Codec.INT.optionalFieldOf("maxTransferAmount", 1000).forGetter(EnergyTransferComponent::getMaxTransferAmount),
						Codec.INT.optionalFieldOf("activeSidesMask", 0).forGetter(EnergyTransferComponent::getActiveSidesMask))
				.apply(instance, (baseProps, invId, limit, sidesMask) -> {
					EnergyTransferComponent comp = new EnergyTransferComponent(baseProps.id(), isInput);
					comp.setBaseProperties(baseProps);
					comp.inventoryId = invId;
					comp.maxTransferAmount = limit;
					comp.activeSidesMask = sidesMask;
					return comp;
				}));
	}

	public EnergyTransferComponent(UUID uuid, boolean isInput) {
		super(uuid);
		this.isInput = isInput;
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 1;
	}

	@Override
	public FlowComponentType getType() {
		return isInput ? VanillaSFMFlowPlugin.ENERGY_INPUT.get() : VanillaSFMFlowPlugin.ENERGY_OUTPUT.get();
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

	public int getMaxTransferAmount() {
		return maxTransferAmount;
	}

	public void setMaxTransferAmount(int amount) {
		this.maxTransferAmount = amount;
	}

	@Override
	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	@Override
	public void toggleSide(Direction dir) {
		activeSidesMask ^= (1 << dir.ordinal());
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
		compoundTag.putInt("maxTransferAmount", this.maxTransferAmount);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("maxTransferAmount")) {
			this.maxTransferAmount = compoundTag.getInt("maxTransferAmount");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		if (this.isInput()) {
			EnergyTransferPlanner.planInput(context, this);
		} else {
			EnergyTransferPlanner.planOutput(context, this);
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable(isInput ? "gui.sfmflow.energy_input" : "gui.sfmflow.energy_output");
	}
}