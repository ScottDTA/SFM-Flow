package dta.sfmflow.flowcomponents;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Visual boundary input pin inside a sub-canvas.
 */
public class GroupInputComponent extends AbstractFlowComponent {

	public static final MapCodec<GroupInputComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(GroupInputComponent::getBaseProperties))
			.apply(instance, baseProps -> {
				GroupInputComponent comp = new GroupInputComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				return comp;
			}));

	public GroupInputComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.hasOutputNodes = true;
		this.numOutputs = 1;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.GROUP_INPUT.get();
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
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.literal("Group Input");
	}
}