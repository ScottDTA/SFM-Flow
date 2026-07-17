package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;

import java.util.UUID;

/**
 * Logical flowchart node that joins multiple input execution paths into a single output [3].
 */
public class CollectorComponent extends AbstractFlowComponent {

	public static final MapCodec<CollectorComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(CollectorComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("numInputs", 2).forGetter(CollectorComponent::getNumInputs))
			.apply(instance, (baseProps, inputsCount) -> {
				CollectorComponent comp = new CollectorComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.numInputs = inputsCount;
				return comp;
			}));

	public CollectorComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 2; // Default to 2 inputs [3]
		this.hasOutputNodes = true;
		this.numOutputs = 1; // 1 output [3]
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.COLLECTOR.get();
	}

	public int getNumInputs() {
		return this.numInputs;
	}

	public void setNumInputs(int count) {
		this.numInputs = count;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		for (FlowComponentConnections conn : context.getConnections()) {
			if (conn.getSourceComponentId().equals(this.getId())) {
				context.enqueue(conn.getTargetComponentId());
			}
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("numInputs", this.numInputs);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		CollectorComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse collector component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.numInputs = decoded.getNumInputs();
				});

		if (compoundTag.contains("numInputs")) {
			this.numInputs = compoundTag.getInt("numInputs");
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.collector");
	}

	@Override
	public Component getInputNodeTooltip(int index) {
		return Component.literal("Input Pin #" + (index + 1));
	}

	@Override
	public Component getOutputNodeTooltip(int index) {
		return Component.literal("Execute Output");
	}
}