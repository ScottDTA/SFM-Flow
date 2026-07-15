package dta.sfmflow.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dta.sfmflow.api.capability.FlowCapabilityRegistry;
import dta.sfmflow.api.capability.SpecialBlockCapabilityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Declares scannable target inventory indices mapped to physical coordinates.
 */
public class ConnectionBlock implements IContainerSelection {
	private Level level;
	private BlockPos blockPos;
	private int cableDistance;
	private Set<ResourceLocation> types;
	private int id;
	private volatile boolean sleeping = false;
	private Direction direction;

	// Virtual slot tracking for Cable Cluster cards 
	private int slotIndex = -1;
	private ItemStack cardStack = ItemStack.EMPTY;

	public record SidedCacheKey(ResourceLocation capId, @Nullable Direction side) {}

	private final Map<SidedCacheKey, BlockCapabilityCache<?, Direction>> capabilityCaches = new HashMap<>();

	public ConnectionBlock(BlockPos blockPos, int cableDistance) {
		this(blockPos, cableDistance, -1);
	}

	public ConnectionBlock(BlockPos blockPos, int cableDistance, int slotIndex) {
		this.blockPos = blockPos;
		this.cableDistance = cableDistance;
		this.slotIndex = slotIndex;
		this.types = new HashSet<>();
		this.cardStack = ItemStack.EMPTY;
	}

	public ConnectionBlock(Level level, BlockPos blockPos, int cableDistance) {
		this(blockPos, cableDistance, -1);
		this.level = level;
	}

	public ConnectionBlock(Level level, BlockPos blockPos, int cableDistance, int slotIndex, ItemStack cardStack) {
		this(blockPos, cableDistance, slotIndex);
		this.level = level;
		this.cardStack = cardStack;
	}
	
	public ConnectionBlock(Level level, BlockPos blockPos, int cableDistance, int slotIndex, ItemStack cardStack, @Nullable Direction direction) {
	    this(blockPos, cableDistance, slotIndex);
	    this.level = level;
	    this.cardStack = cardStack;
	    this.direction = direction; // Store the direction
	}

	/**
	 * Copy constructor to create a thread-safe, isolated snapshot of a connection block.
	 */
	public ConnectionBlock(ConnectionBlock other) {
		this.level = other.level;
		this.blockPos = other.blockPos;
		this.cableDistance = other.cableDistance;
		this.types = other.types != null ? new HashSet<>(other.types) : new HashSet<>();
		this.id = other.id;
		this.sleeping = other.sleeping;
		this.slotIndex = other.slotIndex;
		this.cardStack = other.cardStack != null ? other.cardStack.copy() : ItemStack.EMPTY;
		this.direction = other.direction;
	}

	public @Nullable Direction getDirection() {
	    return direction;
	}
	
	public BlockPos getBlockPos() {
		return blockPos;
	}

	public void setTypes(Set<ResourceLocation> caps) {
		types = caps;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isSleeping() {
		return this.sleeping;
	}

	public void setSleeping(boolean sleeping) {
		this.sleeping = sleeping;
	}

	public int getCableDistance() {
		return this.cableDistance;
	}

	public Set<ResourceLocation> getTypes() {
		return this.types;
	}

	public int getSlotIndex() {
		return slotIndex;
	}

	public ItemStack getCardStack() {
		return cardStack;
	}

	public void registerCache(ResourceLocation capId, @Nullable Direction side, BlockCapabilityCache<?, Direction> cache) {
		this.capabilityCaches.put(new SidedCacheKey(capId, side), cache);
	}

	@SuppressWarnings("unchecked")
	public @Nullable <T> T getHandler(ResourceLocation capId, Class<T> clazz, @Nullable Direction side) {
		if (this.sleeping) {
			return null;
		}

		BlockCapabilityCache<?, Direction> cache = this.capabilityCaches.get(new SidedCacheKey(capId, side));
		if (cache != null) {
			try {
				Object handler = cache.getCapability();
				if (clazz.isInstance(handler)) {
					return clazz.cast(handler);
				}
			} catch (IllegalStateException e) {
				// Fallback dynamically if the cache is temporarily invalidated
			}
		}

		var flowCap = FlowCapabilityRegistry.get(capId);
		if (flowCap != null && flowCap.getCapability() != null && this.level != null) {
			Object handler = this.level.getCapability((BlockCapability<Object, Direction>) flowCap.getCapability(),
					this.blockPos, side);
			if (handler == null) {
				handler = SpecialBlockCapabilityRegistry.getCapability(
						(BlockCapability<Object, Direction>) flowCap.getCapability(), this.level, this.blockPos,
						this.level.getBlockState(this.blockPos), side);
			}
			if (clazz.isInstance(handler)) {
				return clazz.cast(handler);
			}
		}
		return null;
	}

	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		return getHandler(ResourceLocation.fromNamespaceAndPath("sfmflow", "item"), IItemHandler.class, side);
	}

	public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
		return getHandler(ResourceLocation.fromNamespaceAndPath("sfmflow", "fluid"), IFluidHandler.class, side);
	}

	/**
	 * Generates a clean multi-line tooltip display.
	 */
	public List<Component> getMultiLineTooltip(Level level) {
		List<Component> list = new ArrayList<>();

		// Line 1: Block Name or Card Name [3]
		Component blockName = Component.literal("Unknown Block");
		if (this.slotIndex >= 0 && !this.cardStack.isEmpty()) {
			blockName = this.cardStack.getHoverName();
		} else if (level != null) {
			BlockState state = level.getBlockState(this.blockPos);
			if (!state.isAir()) {
				blockName = state.getBlock().getName();
			}
		}
		list.add(blockName.copy().withStyle(ChatFormatting.WHITE));

		// Line 2: Position coordinates [3]
		list.add(Component.literal("Position: " + this.blockPos.getX() + ", " + this.blockPos.getY() + ", " + this.blockPos.getZ())
				.withStyle(ChatFormatting.GRAY));

	    // Line 3: Use the stored direction field directly [3]
	    if (this.direction != null) {
	        list.add(Component.literal("Direction: " + this.direction.name().toUpperCase(Locale.ROOT))
	                .withStyle(ChatFormatting.AQUA));
	    }
	    return list;
	}

	public Component getDisplayName(Level level) {
		Component baseName;
		if (level != null) {
			var state = level.getBlockState(this.blockPos);
			if (!state.isAir()) {
				baseName = state.getBlock().getName();
			} else {
				baseName = Component.literal("Inventory #" + this.id);
			}
		} else {
			baseName = Component.literal("Inventory #" + this.id);
		}
		return Component.literal(baseName.getString() + " at (" + this.blockPos.getX() + ", " + this.blockPos.getY()
				+ ", " + this.blockPos.getZ() + ")");
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public boolean isVariable() {
		return false;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}
}