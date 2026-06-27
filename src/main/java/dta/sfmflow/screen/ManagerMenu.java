package dta.sfmflow.screen;

import dta.sfmflow.block.ModBlocks;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The logical container menu for the Manager block interface.
 * Coordinates inventory transfer mappings and synchronizes active workspace command counts.
 */
public class ManagerMenu extends AbstractContainerMenu
 {
  private final ManagerBlockEntity blockEntity;
  private final Level level;
  private final ContainerData data;

  public ManagerMenu(int pContainerId, Inventory pInv, FriendlyByteBuf pExtradata)
   {
    this(pContainerId, pInv, pInv.player.level().getBlockEntity(pExtradata.readBlockPos()), new SimpleContainerData(1));
   }
  
  /**
   * Initializes a new manager container menu instance on the client or server.
   * Aligns Player inventory slots strictly to the center column of the bottom panel [3].
   *
   * @param pContainerId the container ID
   * @param pInv the player's inventory
   * @param pEntity the backing BlockEntity
   * @param pData the container data synchronization slots
   */
  public ManagerMenu(int pContainerId, Inventory pInv, BlockEntity pEntity, ContainerData pData)
   {
    super(ModMenuTypes.MANAGER_MENU.get() , pContainerId);
    if (pEntity instanceof ManagerBlockEntity manager)
     {
      this.blockEntity = manager;
     }
    else
     {
      throw new IllegalStateException("Block entity is not a Manager!");
     }
    this.level = pInv.player.level();
    this.data = pData;
    
    // Player Inventory Rows 1-3 starting centrally at X = 175, Y = 266 [3]
    for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(pInv, c + r * 9 + 9, 175 + c * 18, 266 + r * 18));
        }
    }

    // Player Hotbar Row starting centrally at X = 175, Y = 324 [3]
    for (int c = 0; c < 9; c++) {
        this.addSlot(new Slot(pInv, c, 175 + c * 18, 324));
    }

    addDataSlots(data);
   }

  @Override
  public ItemStack quickMoveStack(Player player, int index)
   {
	return null;
   }

  @Override
  public boolean stillValid(Player pPlayer)
   {
    return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, ModBlocks.MANAGER_BLOCK.get());
   }

  public int getCommandCount()
   {
	return this.data.get(0);  
   }
  
  public ManagerBlockEntity getManagerBlockEntity()
   {
	return blockEntity;  
   }
 }