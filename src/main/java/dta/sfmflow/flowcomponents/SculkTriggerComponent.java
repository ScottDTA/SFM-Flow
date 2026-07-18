package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.VanillaGameEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Acoustic sculk trigger node that fires flowchart branches based on
 * environmental vibrations. Targeted and bound specifically to a Sculk Trigger
 * Cable block on the network. Employs zero-garbage in-memory Set lookups for
 * high-performance tick rates. Supports configurable cooldown delay timers to
 * protect network performance.
 */
public class SculkTriggerComponent extends AbstractTriggerComponent implements IInventoryTarget, ISideConfigurable {

	public static final Codec<Map<Direction, List<ResourceLocation>>> SIDE_FILTERS_MAP_CODEC = Codec
			.unboundedMap(Direction.CODEC, ResourceLocation.CODEC.listOf());

	public static final MapCodec<SculkTriggerComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(SculkTriggerComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(SculkTriggerComponent::getInventoryId),
					Codec.INT.optionalFieldOf("activeSidesMask", 63)
							.forGetter(SculkTriggerComponent::getActiveSidesMask),
					SIDE_FILTERS_MAP_CODEC.optionalFieldOf("sideFilters", Map.of())
							.forGetter(SculkTriggerComponent::getSideFiltersMap),
					Codec.INT.optionalFieldOf("radius", 8).forGetter(SculkTriggerComponent::getRadius),
					Codec.INT.optionalFieldOf("cooldownTicks", 20).forGetter(SculkTriggerComponent::getCooldownTicks))
			.apply(instance, (baseProps, invId, sidesMask, filtersMap, rad, cooldown) -> {
				SculkTriggerComponent comp = new SculkTriggerComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.activeSidesMask = sidesMask;
				for (var entry : filtersMap.entrySet()) {
					Direction dir = entry.getKey();
					comp.sideFilters[dir.ordinal()].addAll(entry.getValue());
				}
				comp.radius = rad;
				comp.cooldownTicks = cooldown;
				return comp;
			}));

	private int inventoryId = -1;
	private int activeSidesMask = 63;

	@SuppressWarnings("unchecked")
	private final Set<ResourceLocation>[] sideFilters = new Set[6];
	private int radius = 8;
	private int cooldownTicks = 20;

	private transient volatile boolean triggeredThisTick = false;
	private transient long lastTriggeredTime = 0L;

	public SculkTriggerComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.numInputs = 0;
		this.hasOutputNodes = true;
		this.numOutputs = 1;
		for (int i = 0; i < 6; i++) {
			this.sideFilters[i] = new HashSet<>();
		}
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.SCULK_TRIGGER.get();
	}

	@Override
	public int getInventoryId() {
		return this.inventoryId;
	}

	@Override
	public void setInventoryId(int id) {
		this.inventoryId = id;
	}

	public int getActiveSidesMask() {
		return activeSidesMask;
	}

	public void setActiveSidesMask(int mask) {
		this.activeSidesMask = mask;
	}

	public boolean hasEventFilter(Direction side, ResourceLocation loc) {
		return this.sideFilters[side.ordinal()].contains(loc);
	}

	public void toggleEventFilter(Direction side, ResourceLocation loc) {
		Set<ResourceLocation> set = this.sideFilters[side.ordinal()];
		if (set.contains(loc)) {
			set.remove(loc);
		} else {
			set.add(loc);
		}
	}

	public boolean isFilterEmpty(Direction side) {
		return this.sideFilters[side.ordinal()].isEmpty();
	}

	public Map<Direction, List<ResourceLocation>> getSideFiltersMap() {
		Map<Direction, List<ResourceLocation>> map = new HashMap<>();
		for (Direction dir : Direction.values()) {
			Set<ResourceLocation> set = sideFilters[dir.ordinal()];
			if (!set.isEmpty()) {
				map.put(dir, new ArrayList<>(set));
			}
		}
		return map;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public int getCooldownTicks() {
		return cooldownTicks;
	}

	public void setCooldownTicks(int val) {
		this.cooldownTicks = val;
	}

	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	public void toggleSide(Direction dir) {
		activeSidesMask ^= (1 << dir.ordinal());
	}

	public void onGameEvent(BlockPos targetPos, VanillaGameEvent event, BlockPos eventPos, Direction hitSide) {

		double distSq = targetPos.distSqr(eventPos);
		if (distSq > this.radius * this.radius) {
			return;
		}

		if (!isSideActive(hitSide)) {
			return;
		}

		ResourceLocation eventLoc = event.getVanillaEvent().unwrapKey().map(ResourceKey::location).orElse(null);
		if (eventLoc == null) {
			return;
		}

		Set<ResourceLocation> filters = this.sideFilters[hitSide.ordinal()];

		if (filters.isEmpty() || filters.contains(eventLoc)) {
			this.triggeredThisTick = true;
		}
	}

	@Override
	public boolean evaluateTrigger(Level level, BlockPos pos, long gameTime) {
		if (gameTime - lastTriggeredTime < cooldownTicks && lastTriggeredTime != 0L) {
			this.triggeredThisTick = false;
			return false;
		}

		if (this.triggeredThisTick) {
			this.triggeredThisTick = false;
			this.lastTriggeredTime = gameTime;
			return true;
		}

		return false;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		for (FlowComponentConnections conn : context.getConnections()) {
			if (conn.getSourceComponentId().equals(this.getId())) {
				context.enqueue(conn.getTargetComponentId());
			}
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putInt("activeSidesMask", this.activeSidesMask);
		compoundTag.putInt("radius", this.radius);
		compoundTag.putInt("cooldownTicks", this.cooldownTicks);

		CompoundTag filtersTag = new CompoundTag();
		for (Direction dir : Direction.values()) {
			ListTag sideList = new ListTag();
			for (ResourceLocation loc : this.sideFilters[dir.ordinal()]) {
				sideList.add(StringTag.valueOf(loc.toString()));
			}
			filtersTag.put(dir.getSerializedName(), sideList);
		}
		compoundTag.put("sideFilters", filtersTag);

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		SculkTriggerComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse sculk trigger component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.activeSidesMask = decoded.activeSidesMask;
					this.radius = decoded.getRadius();
					this.cooldownTicks = decoded.getCooldownTicks();
					for (int i = 0; i < 6; i++) {
						this.sideFilters[i].clear();
						this.sideFilters[i].addAll(decoded.sideFilters[i]);
					}
				});

		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("activeSidesMask")) {
			this.activeSidesMask = compoundTag.getInt("activeSidesMask");
		}
		if (compoundTag.contains("radius")) {
			this.radius = compoundTag.getInt("radius");
		}
		if (compoundTag.contains("cooldownTicks")) {
			this.cooldownTicks = compoundTag.getInt("cooldownTicks");
		}
		if (compoundTag.contains("sideFilters")) {
			CompoundTag filtersTag = compoundTag.getCompound("sideFilters");
			for (Direction dir : Direction.values()) {
				Set<ResourceLocation> set = this.sideFilters[dir.ordinal()];
				set.clear();
				String key = dir.getSerializedName();
				if (filtersTag.contains(key)) {
					ListTag list = filtersTag.getList(key, Tag.TAG_STRING);
					for (int i = 0; i < list.size(); i++) {
						ResourceLocation loc = ResourceLocation.tryParse(list.getString(i));
						if (loc != null) {
							set.add(loc);
						}
					}
				}
			}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.sculk_trigger");
	}
}