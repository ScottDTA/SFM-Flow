package dta.sfmflow.registry;

import dta.sfmflow.SFMFlow;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Common tag registry containing block and item tag keys for network cable
 * checks.
 */
public final class ModTags {
	private ModTags() {
	}

	/**
	 * Block tag identifying all registered cable block types.
	 */
	public static final TagKey<Block> CABLES = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "cables"));

	/**
	 * Block tag identifying specialized cable blocks that double as
	 * redstone/network targets. Allows addon developers to register custom
	 * redstone, sensor, and signal cables cleanly.
	 */
	public static final TagKey<Block> REDSTONE_CABLES = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "redstone_cables"));

	/**
	 * Item tag defining valid hardware and sensor cards capable of insertion into
	 * Cable Clusters. Excludes cluster blocks programmatically to avoid infinite
	 * capability recursive loops.
	 */
	public static final TagKey<Item> CLUSTER_COMPATIBLE = TagKey.create(Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "cluster_compatible"));

	/**
	 * Item tag defining all directional cluster cards (e.g., valves, observers).
	 * Governs the maximum capacity of 6 and restricts matching duplicates to unique
	 * directions.
	 */
	public static final TagKey<Item> DIRECTIONAL_CLUSTER_CARDS = TagKey.create(Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "directional_cluster_cards"));

	/**
	 * Item tag defining all omni-directional cluster cards (e.g., redstone signal
	 * sensors). Governs the strict limit of 1 per card type in the cluster.
	 */
	public static final TagKey<Item> OMNI_DIRECTIONAL_CLUSTER_CARDS = TagKey.create(Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "omni_directional_cluster_cards"));

	/**
	 * Block tag identifying blocks that require specialized 3D item rendering
	 * fallback to prevent model/texture bugs in the UI scene.
	 */
	public static final TagKey<Block> SPECIAL_3D_RENDERS = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "special_3d_renders"));
	
	/**
	 * Block tag identifying all cables that physically conduct/extend the network search.
	 * Addon developers can register custom cables here to support network extensions [3].
	 */
	public static final TagKey<Block> CONDUCTIVE_CABLES = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(SFMFlow.MODID, "conductive_cables"));
}