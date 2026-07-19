package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.client.screen.ManagerScreen;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Standard visual directory node acting as a sub-canvas folder.
 */
public class GroupComponent extends AbstractFlowComponent {

	// Updated codec to serialize the dynamic pin counts
	public static final MapCodec<GroupComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(GroupComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("numInputs", 0).forGetter(GroupComponent::getNumInputs),
					Codec.INT.optionalFieldOf("numOutputs", 0).forGetter(GroupComponent::getNumOutputs))
			.apply(instance, (baseProps, inputs, outputs) -> {
				GroupComponent comp = new GroupComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.setNumInputs(inputs);
				comp.setNumOutputs(outputs);
				return comp;
			}));

	public GroupComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 0; // Starts with 0 inputs
		this.hasOutputNodes = true;
		this.numOutputs = 0; // Starts with 0 outputs
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.GROUP_NODE.get();
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		List<AbstractFlowComponent> sortedInputs = getSortedInnerTerminals(context, true);

		// Map each incoming connection pin index to the corresponding sorted inner GroupInput
		for (FlowComponentConnections conn : context.getConnections()) {
			if (conn.getTargetComponentId().equals(this.getId())) {
				int inputPinIdx = conn.getInputNodeIndex();
				if (inputPinIdx >= 0 && inputPinIdx < sortedInputs.size()) {
					context.enqueue(sortedInputs.get(inputPinIdx).getId());
				}
			}
		}
	}

	@Override
	public @Nullable Component getInputNodeTooltip(int index) {
		List<AbstractFlowComponent> inputs = getSortedInnerTerminals(null, true);
		if (index >= 0 && index < inputs.size()) {
			return Component.literal("Group Input: ").append(inputs.get(index).getName());
		}
		return null;
	}

	@Override
	public @Nullable Component getOutputNodeTooltip(int index) {
		List<AbstractFlowComponent> outputs = getSortedInnerTerminals(null, false);
		if (index >= 0 && index < outputs.size()) {
			return Component.literal("Group Output: ").append(outputs.get(index).getName());
		}
		return null;
	}

	/**
	 * Resolves and sorts all active inner terminals alphabetically by their UUIDs.
	 */
	public List<AbstractFlowComponent> getSortedInnerTerminals(@Nullable FlowchartPlanningContext context, boolean isInput) {
		Collection<AbstractFlowComponent> sourceList;
		if (context != null) {
			sourceList = context.getComponents().values();
		} else {
			sourceList = getClientComponents();
		}

		List<AbstractFlowComponent> terminals = new ArrayList<>();
		for (AbstractFlowComponent comp : sourceList) {
			if (this.getId().equals(comp.getParentGroupId())) {
				if (isInput && comp instanceof GroupInputComponent) {
					terminals.add(comp);
				} else if (!isInput && comp instanceof GroupOutputComponent) {
					terminals.add(comp);
				}
			}
		}
		terminals.sort(Comparator.comparing(AbstractFlowComponent::getId));
		return terminals;
	}

	@OnlyIn(Dist.CLIENT)
	private Collection<AbstractFlowComponent> getClientComponents() {
		var screen = Minecraft.getInstance().screen;
		if (screen instanceof ManagerScreen managerScreen) {
			return managerScreen.getMenu().getManagerBlockEntity().getFlowComponents().values();
		}
		return Collections.emptyList();
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		// Manually serialize pin counts as a fallback double-guard
		compoundTag.putInt("numInputs", this.numInputs);
		compoundTag.putInt("numOutputs", this.numOutputs);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		GroupComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse group component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.numInputs = decoded.getNumInputs();
					this.numOutputs = decoded.getNumOutputs();
				});

		super.loadData(compoundTag);

		if (compoundTag.contains("numInputs")) {
			this.numInputs = compoundTag.getInt("numInputs");
		}
		if (compoundTag.contains("numOutputs")) {
			this.numOutputs = compoundTag.getInt("numOutputs");
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.menu.group_node");
	}
}