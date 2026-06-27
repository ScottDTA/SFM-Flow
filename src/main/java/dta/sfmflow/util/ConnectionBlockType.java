package dta.sfmflow.util;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import dta.sfmflow.compat.MekanismCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Declares target capability and interface classifications for scanned network targets [3].
 */
public enum ConnectionBlockType
 {
  ITEM("gui.sfmflow.type_item", () -> Capabilities.ItemHandler.BLOCK, () -> true),
  FLUID("gui.sfmflow.type_fluid", () -> Capabilities.FluidHandler.BLOCK, () -> true),
  ENERGY("gui.sfmflow.type_energy", () -> Capabilities.EnergyStorage.BLOCK, () -> true),	
  CHEMICAL("Chemical (Gas)", MekanismCompat::getChemicalCapability, () -> ModList.get().isLoaded("mekanism")),
  
  /**
   * Analog redstone input and output devices connected as logical targets on the physical network [3].
   */
  REDSTONE("gui.sfmflow.type_redstone", () -> null, () -> true);

  private final String translationKey;
  private final Supplier<BlockCapability<?, Direction>> capabilitySupplier;
  private final Supplier<Boolean> enabledSupplier;
  
  ConnectionBlockType(String translationKey, Supplier<BlockCapability<?, Direction>> capabilitySupplier, Supplier<Boolean> enabledSupplier)
   {
	this.translationKey = translationKey;
	this.capabilitySupplier = capabilitySupplier;
    this.enabledSupplier = enabledSupplier;
   }
  
  public String getName()
   {
	return translationKey;  
   }
  
  public boolean isEnabled()
   {
    return this.enabledSupplier.get();
   }
  
  public <T> @Nullable T getHandler(Class<T> typeClass, Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side)
   {
    if (!this.isEnabled())
     {
      return null;
     }
      
    BlockCapability<?, Direction> cap = this.capabilitySupplier.get();
    if (cap == null)
     {
      return null;
     }

    Object handler = level.getCapability(cap, pos, state, be, side);
    if (typeClass.isInstance(handler))
     {
      return typeClass.cast(handler);
     }
    return null;
   }
  
  public boolean isPresent(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side)
   {
    if (!this.isEnabled())
     {
      return false;
     }
    BlockCapability<?, Direction> cap = this.capabilitySupplier.get();
    return cap != null && level.getCapability(cap, pos, state, be, side) != null;
   }
  
  public boolean isPresentAnywhere(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be)
   {
    if (!this.isEnabled())
     {
      return false;
     }
    if (this.isPresent(level, pos, state, be, null))
     {
      return true;
     }
    for (Direction dir : Direction.values())
     {
      if (this.isPresent(level, pos, state, be, dir))
       {
        return true;
       }
     }
    return false;
   }
  
  @Override
  public String toString()
   {
	return translationKey;  
   }
 }