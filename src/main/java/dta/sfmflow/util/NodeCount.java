package dta.sfmflow.util;

public enum NodeCount {
	ONE(1, new int[] { 29 }, new int[] { 59 }), TWO(2, new int[] { 15, 43 }, new int[] { 31, 87 }),
	THREE(3, new int[] { 4, 29, 54 }, new int[] { 14, 59, 104 }),
	FOUR(4, new int[] { 5, 21, 37, 53 }, new int[] { 14, 44, 74, 104 }),
	FIVE(5, new int[] { 3, 16, 29, 42, 55 }, new int[] { 12, 35, 59, 83, 106 });

	private final int nodeCount;
	private final int[] closedOffsets;
	private final int[] openOffsets;

	NodeCount(int nodeCount, int[] closedOffsets, int[] openOffsets) {
		this.nodeCount = nodeCount;
		this.closedOffsets = closedOffsets;
		this.openOffsets = openOffsets;
	}

	public int getNodeCount() {
		return this.nodeCount;
	}

	public int[] getOffsets(boolean isOpen) {
		return isOpen ? this.openOffsets : this.closedOffsets;
	}

	public static NodeCount getForCount(int count) {
		return values()[count - 1];
	}

}