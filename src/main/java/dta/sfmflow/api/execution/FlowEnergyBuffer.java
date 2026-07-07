package dta.sfmflow.api.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Public API container representing energy flowing along a specific connection wire,
 * preserving source block coordinate metadata for downstream evaluation [3].
 */
public final class FlowEnergyBuffer {
	public record BufferedEnergy(BlockPos srcPos, @Nullable Direction srcSide, int amount) {}

	private final List<BufferedEnergy> energies = new ArrayList<>();

	public List<BufferedEnergy> getEnergies() {
		return this.energies;
	}

	public void add(BlockPos srcPos, @Nullable Direction srcSide, int amount) {
		if (amount <= 0) return;
		for (int i = 0; i < energies.size(); i++) {
			BufferedEnergy existing = energies.get(i);
			if (existing.srcPos().equals(srcPos) && existing.srcSide() == srcSide) {
				energies.set(i, new BufferedEnergy(srcPos, srcSide, existing.amount() + amount));
				return;
			}
		}
		energies.add(new BufferedEnergy(srcPos, srcSide, amount));
	}

	public boolean isEmpty() {
		return energies.isEmpty() || energies.stream().allMatch(e -> e.amount() <= 0);
	}
}