package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractTransferComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Unified logic component handling both Forge Energy (FE) inputs (extractions) and energy
 * outputs (depositions).
 */
public class EnergyTransferComponent extends AbstractTransferComponent {
	private int maxTransferAmount = 1000;

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
		super(uuid, isInput);
	}

	@Override
	public FlowComponentType getType() {
		return isInput ? VanillaSFMFlowPlugin.ENERGY_INPUT.get() : VanillaSFMFlowPlugin.ENERGY_OUTPUT.get();
	}

	public int getMaxTransferAmount() {
		return maxTransferAmount;
	}

	public void setMaxTransferAmount(int amount) {
		this.maxTransferAmount = amount;
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("maxTransferAmount", this.maxTransferAmount);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		super.loadData(compoundTag);
		if (compoundTag.contains("maxTransferAmount")) {
			this.maxTransferAmount = compoundTag.getInt("maxTransferAmount");
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