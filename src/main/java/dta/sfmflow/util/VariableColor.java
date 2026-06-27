package dta.sfmflow.util;

public enum VariableColor {
	WHITE("gui.sfmflow.variable_white", Color.WHITE, 1.0F, 1.0F, 1.0F);

	private String name;
	private Color textColor;
	private float red;
	private float green;
	private float blue;

	private VariableColor(String name, Color textColor, float red, float green, float blue) {
		this.name = name;
		this.textColor = textColor;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

}