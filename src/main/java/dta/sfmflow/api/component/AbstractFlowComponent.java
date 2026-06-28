package dta.sfmflow.api.component;

import java.util.UUID;
import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.util.Color;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Public API base class representing an interactive workspace flowchart
 * component [3]. Houses hardcoded coordinates and locked visual dimensions for
 * standard node rendering [3].
 */
public abstract class AbstractFlowComponent {
	public static final int BASE_WIDTH = 64;
	public static final int BASE_HEIGHT = 20;
	public static final int OUTPUT_EXTENSION = 6;
	public static final int INPUT_EXTENSION = 6;
	public static final int CANVAS_MAX_X = 508;
	public static final int CANVAS_MAX_Y = 240;

	/**
	 * Helper record containing core flowchart component properties to streamline
	 * Codec usage [3]. NBT Purged: Completely removed isOpen, xBeforeOpen, and
	 * yBeforeOpen fields to match compact specifications [3].
	 */
	public record BaseProperties(UUID id, int x, int y, int z, String customName, dta.sfmflow.util.Color colorMask) {
		/**
		 * MapCodec handling the base flowchart component fields. Fully purged of old
		 * toggled coordinates [3].
		 */
		public static final MapCodec<BaseProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
				.group(UUIDUtil.CODEC.fieldOf("id").forGetter(BaseProperties::id),
						Codec.INT.optionalFieldOf("x", -9999).forGetter(BaseProperties::x),
						Codec.INT.optionalFieldOf("y", -9999).forGetter(BaseProperties::y),
						Codec.INT.optionalFieldOf("z", 0).forGetter(BaseProperties::z),
						Codec.STRING.optionalFieldOf("customName", "").forGetter(BaseProperties::customName),
						dta.sfmflow.util.Color.CODEC.optionalFieldOf("colorMask")
								.forGetter(props -> Optional.ofNullable(props.colorMask())))
				.apply(instance, (id, x, y, z, customName, colorMaskOpt) -> new BaseProperties(id, x, y, z, customName,
						colorMaskOpt.orElse(Color.WHITE))));
	}

	public static final Codec<AbstractFlowComponent> CODEC = FlowComponentType.REGISTRY.byNameCodec().partialDispatch(
			"type", component -> com.mojang.serialization.DataResult.success(component.getType()), typeEntry -> {
				if (typeEntry == null) {
					return com.mojang.serialization.DataResult.error(
							() -> "Flowchart component type identifier key is completely missing from the active registry mapping map grid!");
				}
				return com.mojang.serialization.DataResult.success(typeEntry.codec());
			});

	protected UUID id;
	private int x;
	private int y;
	private int z;
	protected boolean hasOutputNodes = false;
	protected int numOutputs = 0;
	protected boolean hasInputNodes = false;
	protected int numInputs = 0;
	private int activeCategory = 0;
	protected String customName = "";
	protected Color colorMask = Color.WHITE;

	protected AbstractFlowComponent(UUID uuid) {
		this.id = uuid;
		this.x = 50;
		this.y = 50;
		this.z = 0;
		this.colorMask = Color.WHITE;
	}

	/**
	 * Packs the core fields of this component into a BaseProperties wrapper [3].
	 *
	 * @return the BaseProperties instance [3]
	 */
	public BaseProperties getBaseProperties() {
		return new BaseProperties(id, x, y, z, customName, colorMask);
	}

	/**
	 * Unpacks core fields from a BaseProperties wrapper into this component [3].
	 *
	 * @param props the BaseProperties instance containing values to apply [3]
	 */
	public void setBaseProperties(BaseProperties props) {
		this.id = props.id();
		this.x = props.x() == -9999 ? 50 : props.x();
		this.y = props.y() == -9999 ? 50 : props.y();
		this.z = props.z() == -1 ? 0 : props.z();
		this.customName = props.customName() == null ? "" : props.customName();
		this.colorMask = props.colorMask() == null ? Color.WHITE : props.colorMask();
	}

	/**
	 * Stub helper method retained as transient to prevent compile errors [3].
	 */
	public void setOpen(boolean open) {
	}

	/**
	 * Always compact layout: returns false [3].
	 */
	public boolean isOpen() {
		return false;
	}

	public int getXBeforeOpen() {
		return -1;
	}

	public int getYBeforeOpen() {
		return -1;
	}

	public void setXBeforeOpen(int x) {
	}

	/**
	 * Locked to 64px width standard [3].
	 */
	public int getVisualWidth() {
		return BASE_WIDTH;
	}

	/**
	 * Locked to 20px plus output terminal space [3].
	 */
	public int getVisualHeight() {
		int h = BASE_HEIGHT;
		if (this.hasOutputNodes) {
			h += OUTPUT_EXTENSION;
		}
		return h;
	}

	public CompoundTag saveData(CompoundTag compoundTag) {
		AbstractFlowComponent.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this)
				.resultOrPartial(err -> dta.sfmflow.SFMFlow.LOGGER.error("Failed to encode flow component: {}", err))
				.ifPresent(nbt -> {
					if (nbt instanceof CompoundTag c) {
						compoundTag.merge(c);
					}
				});

		ResourceLocation registryKey = FlowComponentType.REGISTRY.getKey(this.getType());
		if (registryKey != null) {
			compoundTag.putString("type", registryKey.toString());
		}
		return compoundTag;
	}

	public void loadData(CompoundTag compoundTag) {
		AbstractFlowComponent.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag)
				.resultOrPartial(err -> dta.sfmflow.SFMFlow.LOGGER.error("Failed to decode flow component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
				});
	}

	public abstract FlowComponentType getType();

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public Component getName() {
		if (this.customName != null && !this.customName.isEmpty()) {
			return Component.literal(this.customName);
		}
		return Component.literal("Unknown Flow Component");
	}

	public String getCustomName() {
		return this.customName;
	}

	public void setCustomName(String name) {
		this.customName = name == null ? "" : name;
	}

	public Color getColorMask() {
		return this.colorMask;
	}

	public void setColorMask(Color color) {
		this.colorMask = color == null ? Color.WHITE : color;
	}

	public boolean hasOutputNodes() {
		return hasOutputNodes;
	}

	public int getNumOutputs() {
		return numOutputs;
	}

	public boolean hasInputNodes() {
		return hasInputNodes;
	}

	public int getNumInputs() {
		return numInputs;
	}

	public int getActiveCategory() {
		return activeCategory;
	}

	public void setActiveCategory(int category) {
		this.activeCategory = category;
	}
	
	/**
	 * Executes or plans the logical behavior of this component during a flowchart evaluation sweep [3].
	 * Custom components can override this to implement custom logic [3].
	 *
	 * @param context the execution context providing safe access to snapshot states and connection queues [3]
	 */
	public void plan(dta.sfmflow.api.execution.FlowchartPlanningContext context) {
		// Default implementation: do nothing [3]
	}	
	
}