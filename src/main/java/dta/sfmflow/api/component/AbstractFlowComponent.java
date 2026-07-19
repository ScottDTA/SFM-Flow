package dta.sfmflow.api.component;

import java.util.UUID;

import javax.annotation.Nullable;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.util.Color;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

/**
 * Public API base class representing an interactive workspace flowchart
 * component. Houses hardcoded coordinates and locked visual dimensions for
 * standard node rendering.
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
	 * Codec usage.
	 */
	public record BaseProperties(UUID id, int x, int y, int z, String customName, Color colorMask, Optional<UUID> parentGroupId) {
		public static final MapCodec<BaseProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
				.group(UUIDUtil.CODEC.fieldOf("id").forGetter(BaseProperties::id),
						Codec.INT.optionalFieldOf("x", -9999).forGetter(BaseProperties::x),
						Codec.INT.optionalFieldOf("y", -9999).forGetter(BaseProperties::y),
						Codec.INT.optionalFieldOf("z", 0).forGetter(BaseProperties::z),
						Codec.STRING.optionalFieldOf("customName", "").forGetter(BaseProperties::customName),
						Color.CODEC.optionalFieldOf("colorMask")
								.forGetter(props -> Optional.ofNullable(props.colorMask())),
						UUIDUtil.CODEC.optionalFieldOf("parentGroupId").forGetter(BaseProperties::parentGroupId))
				.apply(instance, (id, x, y, z, customName, colorMaskOpt, parentGroupIdOpt) -> new BaseProperties(id, x, y, z, customName,
						colorMaskOpt.orElse(Color.WHITE), parentGroupIdOpt)));
	}

	public static final Codec<AbstractFlowComponent> CODEC = FlowComponentType.REGISTRY.byNameCodec()
			.partialDispatch("type", component -> DataResult.success(component.getType()), typeEntry -> {
				if (typeEntry == null) {
					return DataResult.error(
							() -> "Flowchart component type identifier key is completely missing from the active registry mapping map grid!");
				}
				return DataResult.success(typeEntry.codec());
			});

	protected UUID id;
	private int x;
	private int y;
	private int z;
	protected boolean hasOutputNodes = false;
	protected int numOutputs = 0;
	protected boolean hasInputNodes = false;
	protected int numInputs = 0;
	protected String customName = "";
	protected Color colorMask = Color.WHITE;
	protected @Nullable UUID parentGroupId = null;

	protected AbstractFlowComponent(UUID uuid) {
		this.id = uuid;
		this.x = 50;
		this.y = 50;
		this.z = 0;
		this.colorMask = Color.WHITE;
	}

	public BaseProperties getBaseProperties() {
		return new BaseProperties(id, x, y, z, customName, colorMask, Optional.ofNullable(parentGroupId));
	}

	public void setBaseProperties(BaseProperties props) {
		this.id = props.id();
		this.x = props.x() == -9999 ? 50 : props.x();
		this.y = props.y() == -9999 ? 50 : props.y();
		this.z = props.z() == -1 ? 0 : props.z();
		this.customName = props.customName() == null ? "" : props.customName();
		this.colorMask = props.colorMask() == null ? Color.WHITE : props.colorMask();
		this.parentGroupId = props.parentGroupId().orElse(null);
	}
	/**
	 * Locked to 64px width standard.
	 */
	public int getVisualWidth() {
		return BASE_WIDTH;
	}	

	public @Nullable UUID getParentGroupId() {
		return parentGroupId;
	}

	public void setParentGroupId(@Nullable UUID id) {
		this.parentGroupId = id;
	}

	/**
	 * Locked to 20px plus output terminal space.
	 */
	public int getVisualHeight() {
		int h = BASE_HEIGHT;
		if (this.hasOutputNodes) {
			h += OUTPUT_EXTENSION;
		}
		return h;
	}

	public CompoundTag saveData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		AbstractFlowComponent.CODEC.encodeStart(ops, this)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to encode flow component: {}", err))
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
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		AbstractFlowComponent.CODEC.parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to decode flow component: {}", err))
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

	public void plan(FlowchartPlanningContext context) {
	}

	/**
	 * Resolves a customized hovering tooltip for a specific input pin index.
	 */
	@Nullable
	public Component getInputNodeTooltip(int index) {
		return null;
	}

	/**
	 * Resolves a customized hovering tooltip for a specific output pin index.
	 */
	@Nullable
	public Component getOutputNodeTooltip(int index) {
		return null;
	}
	
	public void setNumInputs(int count) {
		this.numInputs = count;
	}

	public void setNumOutputs(int count) {
		this.numOutputs = count;
	}

}