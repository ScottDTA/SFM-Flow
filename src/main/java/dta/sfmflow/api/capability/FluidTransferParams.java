package dta.sfmflow.api.capability;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Standard parameters record used for fluid transfer task frames [3].
 */
public record FluidTransferParams(FluidStack fluid, int maxAmount) {
	public FluidTransferParams {
		fluid = fluid.copy(); // Guarantee isolated copy
	}
}