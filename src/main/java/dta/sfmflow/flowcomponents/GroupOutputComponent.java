package dta.sfmflow.flowcomponents;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/**
 * Visual boundary output pin inside a sub-canvas.
 */
public class GroupOutputComponent extends AbstractFlowComponent {

	public static final MapCodec<GroupOutputComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(GroupOutputComponent::getBaseProperties))
			.apply(instance, baseProps -> {
				GroupOutputComponent comp = new GroupOutputComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				return comp;
			}));

	public GroupOutputComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = false;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.GROUP_OUTPUT.get();
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		UUID parentId = this.getParentGroupId();
		if (parentId != null) {
			AbstractFlowComponent parentGroup = context.getComponents().get(parentId);
			if (parentGroup instanceof GroupComponent group) {
				List<AbstractFlowComponent> innerOutputs = group.getSortedInnerTerminals(context, false);
				int myIndex = innerOutputs.indexOf(this);
				
				if (myIndex != -1) {
					// Propagate execution outside parent GroupComponent's output pin #myIndex
					for (FlowComponentConnections conn : context.getConnections()) {
						if (conn.getSourceComponentId().equals(parentId) && conn.getOutputNodeIndex() == myIndex) {
							context.enqueue(conn.getTargetComponentId());
						}
					}
				}
			}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.literal("Group Output");
	}
}