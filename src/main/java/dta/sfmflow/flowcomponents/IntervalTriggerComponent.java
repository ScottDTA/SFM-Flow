package dta.sfmflow.flowcomponents;

import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.ServerConfig;
import net.minecraft.network.chat.Component;

/**
 * Specialized event trigger that executes logic repeatedly at set periodic intervals [3].
 * Serializes local time configurations and tracks execution periods [3].
 * Upgraded to track server tick timestamps to enforce execution boundaries [3].
 */
public class IntervalTriggerComponent extends AbstractTriggerComponent
 {
  public enum TimeUnit
   {
	TICKS(1, "Ticks"),
	SECONDS(20, "Seconds"),
	MINUTES(1200, "Minutes");

	private final int factor;
	private final String displayName;

	TimeUnit(int factor, String displayName)
	 {
	  this.factor = factor;
	  this.displayName = displayName;
	 }

	public int getFactor()
	 {
	  return factor;
	 }

	public Component getDisplayName()
	 {
	  return Component.literal(displayName);
	 }
   }

  public static final Codec<TimeUnit> TIME_UNIT_CODEC = Codec.STRING.xmap(TimeUnit::valueOf, TimeUnit::name);

  public static final MapCodec<IntervalTriggerComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      BaseProperties.CODEC.fieldOf("base").forGetter(IntervalTriggerComponent::getBaseProperties),
      TIME_UNIT_CODEC.optionalFieldOf("timeUnit", TimeUnit.TICKS).forGetter(IntervalTriggerComponent::getTimeUnit),
      Codec.INT.optionalFieldOf("intervalValue", 10).forGetter(IntervalTriggerComponent::getIntervalValue)
  ).apply(instance, (baseProps, timeUnit, intervalValue) -> {
      IntervalTriggerComponent comp = new IntervalTriggerComponent(baseProps.id());
      comp.setBaseProperties(baseProps);
      comp.setTimeUnit(timeUnit);
      comp.setIntervalValue(intervalValue);
      return comp;
  }));

  private TimeUnit timeUnit = TimeUnit.TICKS;
  private int intervalValue = 10;
  
  // Transient tracking field for execution timing (unsaved) [3]
  private transient long lastExecutedTick = 0L;

  public IntervalTriggerComponent(UUID uuid)
   {
    super(uuid);
   }

  public int getTotalTicks()
   {
    int rawTicks = intervalValue * timeUnit.getFactor();
    int minTicks = ServerConfig.MIN_INTERVAL_TICKS.get();
    int maxTicks = ServerConfig.MAX_INTERVAL_TICKS.get();
    return Math.max(minTicks, Math.min(rawTicks, maxTicks));
   }

  public TimeUnit getTimeUnit()
   {
    return timeUnit;
   }

  public void setTimeUnit(TimeUnit timeUnit)
   {
    this.timeUnit = timeUnit;
   }

  public int getIntervalValue()
   {
    return intervalValue;
   }

  public void setIntervalValue(int intervalValue)
   {
    this.intervalValue = intervalValue;
   }

  /**
   * Retrieves the last cached execution timestamp of this trigger [3].
   *
   * @return server game time tick [3]
   */
  public long getLastExecutionTick() {
      return this.lastExecutedTick;
  }

  /**
   * Sets the last cached execution timestamp of this trigger [3].
   *
   * @param tick server game time [3]
   */
  public void setLastExecutionTick(long tick) {
      this.lastExecutedTick = tick;
  }

  @Override
  public FlowComponentType getType() {
      // Change from FlowComponentType.INTERVAL_TRIGGER to VanillaSFMFlowPlugin.INTERVAL_TRIGGER
      return VanillaSFMFlowPlugin.INTERVAL_TRIGGER.get();
  }

  @Override
  public void loadData(net.minecraft.nbt.CompoundTag compoundTag)
   {
    IntervalTriggerComponent.CODEC.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag)
        .resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse interval trigger data: {}", err))
        .ifPresent(decoded -> {
            this.setBaseProperties(decoded.getBaseProperties());
            this.setTimeUnit(decoded.getTimeUnit());
            this.setIntervalValue(decoded.getIntervalValue());
        });
   }

  @Override
  public Component getName()
   {
    if (getCustomName() != null && !getCustomName().isEmpty())
     {
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
  
 }