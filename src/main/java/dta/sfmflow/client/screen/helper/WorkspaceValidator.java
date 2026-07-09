package dta.sfmflow.client.screen.helper;

import javax.annotation.Nullable;

import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.client.WorkspaceValidatorRegistry;
import dta.sfmflow.client.screen.ManagerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Delegating validation director querying registered component validators on the clientbound UI [3].
 */
@OnlyIn(Dist.CLIENT)
public final class WorkspaceValidator {

	private WorkspaceValidator() {
	}

	public static boolean hasUnboundInventoryError(ManagerScreen screen, AbstractFlowComponent component) {
		var validator = WorkspaceValidatorRegistry.getValidator(component.getClass());
		if (validator != null) {
			return validator.hasError(screen, component);
		}
		return false;
	}

	public static @Nullable Component getErrorTooltip(ManagerScreen screen, AbstractFlowComponent component) {
		var validator = WorkspaceValidatorRegistry.getValidator(component.getClass());
		if (validator != null) {
			return validator.getErrorTooltip(screen, component);
		}
		return null;
	}

	public static boolean hasEmptyFilterVariableWarning(ManagerScreen screen, AbstractFlowComponent component) {
		var validator = WorkspaceValidatorRegistry.getValidator(component.getClass());
		if (validator != null) {
			return validator.hasWarning(screen, component);
		}
		return false;
	}

	public static @Nullable Component getWarningTooltip(ManagerScreen screen, AbstractFlowComponent component) {
		var validator = WorkspaceValidatorRegistry.getValidator(component.getClass());
		if (validator != null) {
			return validator.getWarningTooltip(screen, component);
		}
		return null;
	}
}