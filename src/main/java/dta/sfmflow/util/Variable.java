package dta.sfmflow.util;

import java.util.ArrayList;
import java.util.List;

public class Variable implements IContainerSelection
 {
  private int id;
  private List<Integer> containers;
	
  public Variable(int id)
   {
	this.id = id;
	containers = new ArrayList<Integer>();
   }
	
	
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isVariable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

 }