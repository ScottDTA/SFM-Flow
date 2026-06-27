package dta.sfmflow.flowcomponents;

import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.ServerConfig;
import net.minecraft.network.chat.Component;

/**
 * Specialized event trigger that executes logic repeatedly at set periodic
 * intervals [3]. Serializes local time configurations and tracks execution
 * periods [3].
 */
public class IntervalTriggerComponent extends AbstractTriggerComponent {
	/**
	 * Logical time units for configuring execution intervals [3].
	 */
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

	/**
	 * Bulletproof codec mapping the TimeUnit enum values using standard string
	 * xmapping [3].
	 */
	public static final Codec<TimeUnit> TIME_UNIT_CODEC = Codec.STRING.xmap(TimeUnit::valueOf, TimeUnit::name);

	/**
	 * Declarative MapCodec mapping the TimeUnit and durational delay properties.
	 * Leverages flat .fieldOf mapping to bypass OptionalFieldCodec validation
	 * limits cleanly.
	 */
	public static final MapCodec<IntervalTriggerComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(
					// 🔥 THE CURE: Use .fieldOf("base") directly on the MapCodec without .codec()
					// wrappers!
					// This guarantees DFU processes the sub-compound map cleanly.
					BaseProperties.CODEC.fieldOf("base").forGetter(IntervalTriggerComponent::getBaseProperties),

					TIME_UNIT_CODEC.optionalFieldOf("timeUnit", TimeUnit.TICKS)
							.forGetter(IntervalTriggerComponent::getTimeUnit),
					Codec.INT.optionalFieldOf("intervalValue", 10)
							.forGetter(IntervalTriggerComponent::getIntervalValue))
			.apply(instance, (baseProps, timeUnit, intervalValue) -> {
				IntervalTriggerComponent comp = new IntervalTriggerComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.setTimeUnit(timeUnit);
				comp.setIntervalValue(intervalValue);
				return comp;
			}));

	private TimeUnit timeUnit = TimeUnit.TICKS;
	private int intervalValue = 10;

	public IntervalTriggerComponent(UUID uuid) {
		super(uuid);
	}

	/**
	 * Calculates the total tick count represented by the current interval value and
	 * time unit [3]. Automatically clamps the resulting value between the server's
	 * configured minimum and maximum limits [3].
	 *
	 * @return the clamped total ticks [3]
	 */
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

	@Override
	public FlowComponentType getType() {
		return FlowComponentType.INTERVAL_TRIGGER.get();
	}

	@Override
	public void loadData(net.minecraft.nbt.CompoundTag compoundTag) {
		IntervalTriggerComponent.CODEC.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag)
				.resultOrPartial(
						err -> dta.sfmflow.SFMFlow.LOGGER.error("Failed to parse interval trigger data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.setTimeUnit(decoded.getTimeUnit());
					this.setIntervalValue(decoded.getIntervalValue());
				});
	}

	/**
	 * Overridden to prioritize custom naming overrides safely [3].
	 *
	 * @return the designated naming display Component [3]
	 */
	@Override
	public Component getName() {
		if (getCustomName() != null && !getCustomName().isEmpty()) {
			return Component.literal(getCustomName());
		}
		return Component.translatable("gui.sfmflow.interval_trigger");
	}
}