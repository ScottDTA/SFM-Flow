package dta.sfmflow.api.component;

import dta.sfmflow.flowcomponents.RedstoneTriggerComponent;
import net.minecraft.core.Direction;

/**
 * Common public API interface representing any component that supports
 * sided analog threshold configurations for Redstone signals.
 */
public interface IRedstoneSidedConfigurable extends ISideConfigurable {
	
	int getThreshold(Direction side);

	void setThreshold(Direction side, int val);

	RedstoneTriggerComponent.Operator getOperator(Direction side);

	void setOperator(Direction side, RedstoneTriggerComponent.Operator op);
}