package dta.sfmflow.registry;

import dta.sfmflow.SFMFlow;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Common tag registry containing block and item tag keys for network cable checks [3].
 */
public final class ModTags
 {
  private ModTags()
   {
   }

  /**
   * Block tag identifying all registered cable block types [3].
   */
  public static final TagKey<Block> CABLES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "cables"));

  /**
   * Block tag identifying specialized cable blocks that double as redstone/network targets [3].
   * Allows addon developers to register custom redstone, sensor, and signal cables cleanly [3].
   */
  public static final TagKey<Block> REDSTONE_CABLES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "redstone_cables"));

  /**
   * Item tag defining valid hardware and sensor cards capable of insertion into Cable Clusters [3].
   * Excludes cluster blocks programmatically to avoid infinite capability recursive loops [3].
   */
  public static final TagKey<Item> CLUSTER_COMPATIBLE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "cluster_compatible"));
 }