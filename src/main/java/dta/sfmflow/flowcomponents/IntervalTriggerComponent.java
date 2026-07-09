package dta.sfmflow.flowcomponents;

import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.api.logging.FlowLogger;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Specialized event trigger that executes logic repeatedly at set periodic
 * intervals [3]. Serializes local time configurations, tracks execution
 * periods, and provides robust direct NBT saving and loading fallback routines
 * [3].
 */
public class IntervalTriggerComponent extends AbstractTriggerComponent {
	public enum TimeUnit {
		TICKS(1, "Ticks"), SECONDS(20, "Seconds"), MINUTES(1200, "Minutes");

		private final int factor;
		private final String displayName;

		TimeUnit(int factor, String displayName) {
			this.factor = factor;
			this.displayName = displayName;
		}

		public int getFactor() {
			return factor;
		}

		public Component getDisplayName() {
			return Component.literal(displayName);
		}
	}

	public static final Codec<TimeUnit> TIME_UNIT_CODEC = Codec.STRING.xmap(TimeUnit::valueOf, TimeUnit::name);

	public static final MapCodec<IntervalTriggerComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(AbstractFlowComponent.BaseProperties.CODEC.fieldOf("base")
					.forGetter(IntervalTriggerComponent::getBaseProperties),
					TIME_UNIT_CODEC.optionalFieldOf("timeUnit", TimeUnit.TICKS)
							.forGetter(IntervalTriggerComponent::getTimeUnit),
					Codec.INT.optionalFieldOf("intervalValue", 10)
							.forGetter(IntervalTriggerComponent::getIntervalValue))
			.apply(instance,
					(AbstractFlowComponent.BaseProperties baseProps, TimeUnit timeUnit, Integer intervalValue) -> {
						IntervalTriggerComponent comp = new IntervalTriggerComponent(baseProps.id());
						comp.setBaseProperties(baseProps);
						comp.setTimeUnit(timeUnit);
						comp.setIntervalValue(intervalValue);
						return comp;
					}));

	private TimeUnit timeUnit = TimeUnit.TICKS;
	private int intervalValue = 10;
	private transient long lastExecutedTick = 0L;

	public IntervalTriggerComponent(UUID uuid) {
		super(uuid);
	}

	public int getTotalTicks() {
		int rawTicks = intervalValue * timeUnit.getFactor();
		int minTicks = ServerConfig.MIN_INTERVAL_TICKS.get();
		int maxTicks = ServerConfig.MAX_INTERVAL_TICKS.get();
		return Math.max(minTicks, Math.min(rawTicks, maxTicks));
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public int getIntervalValue() {
		return intervalValue;
	}

	public void setIntervalValue(int intervalValue) {
		this.intervalValue = intervalValue;
	}

	public long getLastExecutionTick() {
		return this.lastExecutedTick;
	}

	public void setLastExecutionTick(long tick) {
		this.lastExecutedTick = tick;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.INTERVAL_TRIGGER.get();
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		// Direct NBT saving fallback as an exploit and desync shield [3]
		compoundTag.putString("timeUnit", this.timeUnit.name());
		compoundTag.putInt("intervalValue", this.intervalValue);
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		// Use RegistryOps to ensure safety across updates
		var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		IntervalTriggerComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse interval trigger data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.setTimeUnit(decoded.getTimeUnit());
					this.setIntervalValue(decoded.getIntervalValue());
				});

		// Direct NBT loading fallback as an exploit and desync shield [3]
		if (compoundTag.contains("timeUnit")) {
			try {
				this.timeUnit = TimeUnit.valueOf(compoundTag.getString("timeUnit"));
			} catch (IllegalArgumentException e) {
				this.timeUnit = TimeUnit.TICKS;
			}
		}
		if (compoundTag.contains("intervalValue")) {
			this.intervalValue = compoundTag.getInt("intervalValue");
		}
	}

	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable("gui.sfmflow.interval_trigger");
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
	public boolean evaluateTrigger(Level level, BlockPos pos, long gameTime) {
		long elapsedTrigger = gameTime - this.lastExecutedTick;

		if (elapsedTrigger < 0) {
			this.lastExecutedTick = gameTime;
			return false;
		} else if (elapsedTrigger >= this.getTotalTicks()) {
			FlowLogger.execution(
					"Trigger Fired: ID=%s, Hash=%d, GameTime=%d, LastExecuted=%d, Elapsed=%d, Total=%d",
					this.getId(), System.identityHashCode(this), gameTime,
					this.lastExecutedTick, elapsedTrigger, this.getTotalTicks());

			this.lastExecutedTick = gameTime;
			return true;
		}
		return false;
	}
}