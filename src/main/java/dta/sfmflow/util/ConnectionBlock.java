package dta.sfmflow.util;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;

public class ConnectionBlock implements IContainerSelection
 {
  private BlockPos blockPos;
  private int cableDistance;
  private EnumSet<ConnectionBlockType> types;
  private int id;


  public ConnectionBlock(BlockPos blockPos, int cableDistance)
   {
	this.blockPos = blockPos;
	this.cableDistance = cableDistance;
	types = EnumSet.noneOf(ConnectionBlockType.class);
	
   }
  
  public BlockPos getBlockPos()
   {
	return blockPos;
   }

  public void setTypes(EnumSet<ConnectionBlockType> caps)
   {
	types = caps;
   }
  
  public void setId(int id)
   {
	this.id = id;  
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