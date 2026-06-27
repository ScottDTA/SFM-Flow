package dta.sfmflow.block;

import net.minecraft.util.StringRepresentable;

/**
 * Enumeration defining standard operational modes for vacuum and ejection
 * hatches [3].
 */
public enum HatchMode implements StringRepresentable {
	VACUUM("vacuum"), EJECT("eject");

	private final String name;

	HatchMode(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}