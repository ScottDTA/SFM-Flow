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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.VanillaGameEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Acoustic sculk trigger node that fires flowchart branches based on environmental vibrations.
 * Targeted and bound specifically to a Sculk Trigger Cable block on the network [3].
 */
public class SculkTriggerComponent extends AbstractTriggerComponent implements IInventoryTarget, ISideConfigurable {

	public static final MapCodec<SculkTriggerComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(SculkTriggerComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(SculkTriggerComponent::getInventoryId),
					Codec.INT.optionalFieldOf("activeSidesMask", 63).forGetter(SculkTriggerComponent::getActiveSidesMask),
					Codec.STRING.listOf().optionalFieldOf("matchedEvents", List.of()).forGetter(SculkTriggerComponent::getMatchedEvents),
					Codec.INT.optionalFieldOf("radius", 8).forGetter(SculkTriggerComponent::getRadius))
			.apply(instance, (baseProps, invId, sidesMask, events, rad) -> {
				SculkTriggerComponent comp = new SculkTriggerComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.activeSidesMask = sidesMask;
				comp.matchedEvents.clear();
				comp.matchedEvents.addAll(events);
				comp.radius = rad;
				return comp;
			}));

	private int inventoryId = -1;
	private int activeSidesMask = 63; // Defaults to 63 (all 6 faces enabled) [3]
	private final List<String> matchedEvents = new ArrayList<>();
	private int radius = 8; // Standard sculk sensor radius [3]

	private transient boolean triggeredThisTick = false;

	public SculkTriggerComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = false;
		this.hasOutputNodes = true;
		this.numOutputs = 1; // Single execute output path [3]
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

	public List<String> getMatchedEvents() {
		return matchedEvents;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public boolean isSideActive(Direction dir) {
		return (activeSidesMask & (1 << dir.ordinal())) != 0;
	}

	public void toggleSide(Direction dir) {
		activeSidesMask ^= (1 << dir.ordinal());
	}

	public void onGameEvent(BlockPos targetPos, VanillaGameEvent event, BlockPos eventPos) {
		// 1. Verify radius bounds calculated relative to our bound Sculk Trigger Cable block [3]
		double distSq = targetPos.distSqr(eventPos);
		if (distSq > this.radius * this.radius) {
			return;
		}

		// 2. Dynamic Directional Acoustic Shielding (Block events originating from muted faces) [3]
		if (distSq > 0.0) {
			double dx = eventPos.getX() - targetPos.getX();
			double dy = eventPos.getY() - targetPos.getY();
			double dz = eventPos.getZ() - targetPos.getZ();

			Direction primaryDir = Direction.getNearest(dx, dy, dz);
			if (!isSideActive(primaryDir)) {
				return; // Face is muted/shielded [3]!
			}
		}

		// 3. String-based Registry Keyword Filter (Acts as wildcard if list is empty) [3]
		ResourceLocation eventLoc = BuiltInRegistries.GAME_EVENT.getKey(event.getVanillaEvent().value());
		if (eventLoc == null) {
			return;
		}

		String eventStr = eventLoc.toString(); // e.g. "minecraft:step"
		boolean matches = false;

		if (this.matchedEvents.isEmpty()) {
			matches = true;
		} else {
			for (String filter : this.matchedEvents) {
				if (eventStr.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))) {
					matches = true;
					break;
				}
			}
		}

		if (matches) {
			this.triggeredThisTick = true;
		}
	}

	@Override
	public boolean evaluateTrigger(Level level, BlockPos pos, long gameTime) {
		if (this.triggeredThisTick) {
			this.triggeredThisTick = false; // Reset the state [3]
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

		ListTag list = new ListTag();
		for (String event : this.matchedEvents) {
			list.add(StringTag.valueOf(event));
		}
		compoundTag.put("matchedEvents", list);
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
					this.activeSidesMask = decoded.getActiveSidesMask();
					this.radius = decoded.getRadius();
					this.matchedEvents.clear();
					this.matchedEvents.addAll(decoded.getMatchedEvents());
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
		if (compoundTag.contains("matchedEvents")) {
			ListTag list = compoundTag.getList("matchedEvents", Tag.TAG_STRING);
			this.matchedEvents.clear();
			for (int i = 0; i < list.size(); i++) {
				this.matchedEvents.add(list.getString(i));
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