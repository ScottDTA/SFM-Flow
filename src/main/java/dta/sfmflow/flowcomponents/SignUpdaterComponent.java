package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NumericTag; // Import NumericTag [3]
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SignUpdaterComponent extends AbstractFlowComponent implements IInventoryTarget, ISideConfigurable {

	public static final MapCodec<SignUpdaterComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(SignUpdaterComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(SignUpdaterComponent::getInventoryId),
					Direction.CODEC.optionalFieldOf("frontFacing", Direction.NORTH).forGetter(SignUpdaterComponent::getFrontFacing),
					Codec.STRING.listOf().optionalFieldOf("frontLines", List.of("", "", "", "")).forGetter(SignUpdaterComponent::getFrontLines),
					Codec.STRING.listOf().optionalFieldOf("backLines", List.of("", "", "", "")).forGetter(SignUpdaterComponent::getBackLines),
					Codec.BOOL.listOf().optionalFieldOf("updateFront", List.of(true, true, true, true)).forGetter(SignUpdaterComponent::getUpdateFront),
					Codec.BOOL.listOf().optionalFieldOf("updateBack", List.of(true, true, true, true)).forGetter(SignUpdaterComponent::getUpdateBack),
					DyeColor.CODEC.optionalFieldOf("frontColor", DyeColor.BLACK).forGetter(SignUpdaterComponent::getFrontColor),
					DyeColor.CODEC.optionalFieldOf("backColor", DyeColor.BLACK).forGetter(SignUpdaterComponent::getBackColor),
					Codec.BOOL.optionalFieldOf("frontGlow", false).forGetter(SignUpdaterComponent::isFrontGlow),
					Codec.BOOL.optionalFieldOf("backGlow", false).forGetter(SignUpdaterComponent::isBackGlow),
					Codec.BOOL.optionalFieldOf("waxed", false).forGetter(SignUpdaterComponent::isWaxed))
			.apply(instance, (baseProps, invId, facing, fLines, bLines, upF, upB, fColor, bColor, fGlow, bGlow, wax) -> {
				SignUpdaterComponent comp = new SignUpdaterComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.frontFacing = facing;
				comp.frontLines = new ArrayList<>(fLines);
				comp.backLines = new ArrayList<>(bLines);
				comp.updateFront = new ArrayList<>(upF);
				comp.updateBack = new ArrayList<>(upB);
				comp.frontColor = fColor;
				comp.backColor = bColor;
				comp.frontGlow = fGlow;
				comp.backGlow = bGlow;
				comp.waxed = wax;
				return comp;
			}));

	private int inventoryId = -1;
	private Direction frontFacing = Direction.NORTH;
	private List<String> frontLines = new ArrayList<>(List.of("", "", "", ""));
	private List<String> backLines = new ArrayList<>(List.of("", "", "", ""));
	private List<Boolean> updateFront = new ArrayList<>(List.of(true, true, true, true));
	private List<Boolean> updateBack = new ArrayList<>(List.of(true, true, true, true));

	private DyeColor frontColor = DyeColor.BLACK;
	private DyeColor backColor = DyeColor.BLACK;
	private boolean frontGlow = false;
	private boolean backGlow = false;
	private boolean waxed = false;

	public SignUpdaterComponent(UUID uuid) {
		super(uuid);
		this.hasInputNodes = true;
		this.numInputs = 1;
		this.hasOutputNodes = true;
		this.numOutputs = 1;
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.SIGN_UPDATER.get();
	}

	@Override
	public int getInventoryId() {
		return this.inventoryId;
	}

	@Override
	public void setInventoryId(int id) {
		this.inventoryId = id;
	}

	public Direction getFrontFacing() {
		return frontFacing;
	}

	public void setFrontFacing(Direction dir) {
		this.frontFacing = dir == null ? Direction.NORTH : dir;
	}

	public List<String> getFrontLines() {
		return frontLines;
	}

	public List<String> getBackLines() {
		return backLines;
	}

	public List<Boolean> getUpdateFront() {
		return updateFront;
	}

	public List<Boolean> getUpdateBack() {
		return updateBack;
	}

	public DyeColor getFrontColor() {
		return frontColor;
	}

	public void setFrontColor(DyeColor color) {
		this.frontColor = color == null ? DyeColor.BLACK : color;
	}

	public DyeColor getBackColor() {
		return backColor;
	}

	public void setBackColor(DyeColor color) {
		this.backColor = color == null ? DyeColor.BLACK : color;
	}

	public boolean isFrontGlow() {
		return frontGlow;
	}

	public void setFrontGlow(boolean glow) {
		this.frontGlow = glow;
	}

	public boolean isBackGlow() {
		return backGlow;
	}

	public void setBackGlow(boolean glow) {
		this.backGlow = glow;
	}

	public boolean isWaxed() {
		return waxed;
	}

	public void setWaxed(boolean waxed) {
		this.waxed = waxed;
	}

	@Override
	public boolean isSideActive(Direction dir) {
		return dir == this.frontFacing;
	}

	@Override
	public void toggleSide(Direction dir) {
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
		var inventories = context.getConnectedInventories();
		ConnectionBlock targetBlock = null;

		for (var block : inventories) {
			if (block.getId() == this.inventoryId && !block.isSleeping()) {
				targetBlock = block;
				break;
			}
		}

		if (targetBlock != null) {
			BlockPos updaterPos = targetBlock.getBlockPos();
			context.tryWriteTask(ResourceLocation.fromNamespaceAndPath("sfmflow", "sign_updater"),
					context.getSnapshot().getCapturedInventories().get(0).getBlockPos(), 0, null, updaterPos, 0, null,
					new SignUpdaterParams(frontLines, backLines, updateFront, updateBack, frontFacing, frontColor, backColor, frontGlow, backGlow, waxed));
		}

		for (var conn : context.getConnections()) {
			if (conn.getSourceComponentId().equals(this.getId())) {
				context.enqueue(conn.getTargetComponentId());
			}
		}
	}

	@Override
	public CompoundTag saveData(CompoundTag compoundTag) {
		super.saveData(compoundTag);
		compoundTag.putInt("inventoryId", this.inventoryId);
		compoundTag.putString("frontFacing", this.frontFacing.getSerializedName());

		ListTag fLinesList = new ListTag();
		for (String line : frontLines) {
			fLinesList.add(StringTag.valueOf(line));
		}
		compoundTag.put("frontLines", fLinesList);

		ListTag bLinesList = new ListTag();
		for (String line : backLines) {
			bLinesList.add(StringTag.valueOf(line));
		}
		compoundTag.put("backLines", bLinesList);

		ListTag upFList = new ListTag();
		for (boolean b : updateFront) {
			upFList.add(ByteTag.valueOf(b));
		}
		compoundTag.put("updateFront", upFList);

		ListTag upBList = new ListTag();
		for (boolean b : updateBack) {
			upBList.add(ByteTag.valueOf(b));
		}
		compoundTag.put("updateBack", upBList);

		// Save color properties using getSerializedName() (lowercase) to satisfy DyeColor.CODEC
		compoundTag.putString("frontColor", this.frontColor.getSerializedName());
		compoundTag.putString("backColor", this.backColor.getSerializedName());
		compoundTag.putBoolean("frontGlow", this.frontGlow);
		compoundTag.putBoolean("backGlow", this.backGlow);
		compoundTag.putBoolean("waxed", this.waxed);

		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		SignUpdaterComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse sign updater component data: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.frontFacing = decoded.getFrontFacing();
					this.frontLines = new ArrayList<>(decoded.getFrontLines());
					this.backLines = new ArrayList<>(decoded.getBackLines());
					this.updateFront = new ArrayList<>(decoded.getUpdateFront());
					this.updateBack = new ArrayList<>(decoded.getUpdateBack());
					this.frontColor = decoded.getFrontColor();
					this.backColor = decoded.getBackColor();
					this.frontGlow = decoded.isFrontGlow();
					this.backGlow = decoded.isBackGlow();
					this.waxed = decoded.isWaxed();
				});

		super.loadData(compoundTag);

		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("frontFacing")) {
			try {
				this.frontFacing = Direction.valueOf(compoundTag.getString("frontFacing").toUpperCase(java.util.Locale.ROOT));
			} catch (IllegalArgumentException e) {
				this.frontFacing = Direction.NORTH;
			}
		}
		if (compoundTag.contains("frontLines")) {
			ListTag list = compoundTag.getList("frontLines", Tag.TAG_STRING);
			this.frontLines.clear();
			for (int i = 0; i < 4; i++) {
				if (i < list.size()) {
					this.frontLines.add(list.getString(i));
				} else {
					this.frontLines.add("");
				}
			}
		}
		if (compoundTag.contains("backLines")) {
			ListTag list = compoundTag.getList("backLines", Tag.TAG_STRING);
			this.backLines.clear();
			for (int i = 0; i < 4; i++) {
				if (i < list.size()) {
					this.backLines.add(list.getString(i));
				} else {
					this.backLines.add("");
				}
			}
		}
		if (compoundTag.contains("updateFront")) {
			ListTag list = compoundTag.getList("updateFront", Tag.TAG_BYTE);
			this.updateFront.clear();
			for (int i = 0; i < 4; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag numericTag) {
					this.updateFront.add(numericTag.getAsByte() != 0); // Correctly checks NumericTag
				} else {
					this.updateFront.add(true);
				}
			}
		}
		if (compoundTag.contains("updateBack")) {
			ListTag list = compoundTag.getList("updateBack", Tag.TAG_BYTE);
			this.updateBack.clear();
			for (int i = 0; i < 4; i++) {
				if (i < list.size() && list.get(i) instanceof NumericTag numericTag) {
					this.updateBack.add(numericTag.getAsByte() != 0); // Correctly checks NumericTag
				} else {
					this.updateBack.add(true);
				}
			}
		}
		if (compoundTag.contains("frontColor")) {
			try {
				this.frontColor = DyeColor.valueOf(compoundTag.getString("frontColor").toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				this.frontColor = DyeColor.BLACK;
			}
		}
		if (compoundTag.contains("backColor")) {
			try {
				this.backColor = DyeColor.valueOf(compoundTag.getString("backColor").toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				this.backColor = DyeColor.BLACK;
			}
		}
		if (compoundTag.contains("frontGlow")) {
			this.frontGlow = compoundTag.getBoolean("frontGlow");
		}
		if (compoundTag.contains("backGlow")) {
			this.backGlow = compoundTag.getBoolean("backGlow");
		}
		if (compoundTag.contains("waxed")) {
			this.waxed = compoundTag.getBoolean("waxed");
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.sign_updater");
	}

	public record SignUpdaterParams(
			List<String> frontLines, 
			List<String> backLines, 
			List<Boolean> updateFront, 
			List<Boolean> updateBack, 
			Direction facing,
			DyeColor frontColor,
			DyeColor backColor,
			boolean frontGlow,
			boolean backGlow,
			boolean waxed
	) {}
}