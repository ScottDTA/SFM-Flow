package dta.sfmflow.util;

public interface IContainerSelection {
	int getId();

	boolean isVariable();

	// The Following are client side only
	String getName();// needs guimanager?

	String getDescription();// needs guimanager

	// void draw();//needs guidmanager, x, y

}