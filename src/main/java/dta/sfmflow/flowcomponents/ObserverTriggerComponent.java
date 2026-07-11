package dta.sfmflow.flowcomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dta.sfmflow.SFMFlow;
import dta.sfmflow.api.component.AbstractFlowComponent;
import dta.sfmflow.api.component.AbstractTriggerComponent;
import dta.sfmflow.api.component.FlowComponentType;
import dta.sfmflow.api.component.IInventoryTarget;
import dta.sfmflow.api.component.ISideConfigurable;
import dta.sfmflow.api.execution.FlowchartPlanningContext;
import dta.sfmflow.block.entity.ManagerBlockEntity;
import dta.sfmflow.plugin.vanilla.VanillaSFMFlowPlugin;
import dta.sfmflow.util.ConnectionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.UUID;

public class ObserverTriggerComponent extends AbstractTriggerComponent implements IInventoryTarget, ISideConfigurable {

	public static final MapCodec<ObserverTriggerComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(BaseProperties.CODEC.fieldOf("base").forGetter(ObserverTriggerComponent::getBaseProperties),
					Codec.INT.optionalFieldOf("inventoryId", -1).forGetter(ObserverTriggerComponent::getInventoryId),
					Codec.BOOL.optionalFieldOf("previousPowered", false).forGetter(ObserverTriggerComponent::isPreviousPowered),
					Direction.CODEC.optionalFieldOf("frontFacing", Direction.NORTH).forGetter(ObserverTriggerComponent::getFrontFacing))
			.apply(instance, (baseProps, invId, prevPowered, frontFacing) -> {
				ObserverTriggerComponent comp = new ObserverTriggerComponent(baseProps.id());
				comp.setBaseProperties(baseProps);
				comp.inventoryId = invId;
				comp.previousPowered = prevPowered;
				comp.frontFacing = frontFacing;
				return comp;
			}));

	private int inventoryId = -1;
	private boolean previousPowered = false;
	private Direction frontFacing = Direction.NORTH;

	public ObserverTriggerComponent(UUID uuid) {
		super(uuid);
	}

	@Override
	public FlowComponentType getType() {
		return VanillaSFMFlowPlugin.OBSERVER_TRIGGER.get();
	}

	@Override
	public int getInventoryId() {
		return this.inventoryId;
	}

	@Override
	public void setInventoryId(int id) {
		this.inventoryId = id;
	}

	public boolean isPreviousPowered() {
		return this.previousPowered;
	}

	public Direction getFrontFacing() {
		return this.frontFacing;
	}

	public void setFrontFacing(Direction dir) {
		this.frontFacing = dir == null ? Direction.NORTH : dir;
	}

	@Override
	public boolean isSideActive(Direction dir) {
		return dir == this.frontFacing; // The front face is the only active side [3]
	}

	@Override
	public void toggleSide(Direction dir) {
		// No-op for observer trigger [3]
	}

	@Override
	public boolean evaluateTrigger(Level level, BlockPos pos, long gameTime) {
		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof ManagerBlockEntity manager)) {
			return false;
		}

		ConnectionBlock targetBlock = null;
		for (ConnectionBlock inv : manager.getInventories()) {
			if (inv.getId() == this.inventoryId && !inv.isSleeping()) {
				targetBlock = inv;
				break;
			}
		}

		if (targetBlock == null) {
			return false;
		}

		BlockPos targetPos = targetBlock.getBlockPos();
		BlockState targetState = level.getBlockState(targetPos);
		if (!targetState.hasProperty(BlockStateProperties.POWERED)) {
			return false;
		}

		boolean currentPowered = targetState.getValue(BlockStateProperties.POWERED);
		boolean fired = currentPowered && !previousPowered;
		this.previousPowered = currentPowered;

		return fired;
	}

	@Override
	public void plan(FlowchartPlanningContext context) {
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
		compoundTag.putBoolean("previousPowered", this.previousPowered);
		compoundTag.putString("frontFacing", this.frontFacing.name());
		return compoundTag;
	}

	@Override
	public void loadData(CompoundTag compoundTag) {
		HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(NbtOps.INSTANCE, registries);

		ObserverTriggerComponent.CODEC.codec().parse(ops, compoundTag)
				.resultOrPartial(err -> SFMFlow.LOGGER.error("Failed to parse observer trigger component: {}", err))
				.ifPresent(decoded -> {
					this.setBaseProperties(decoded.getBaseProperties());
					this.inventoryId = decoded.getInventoryId();
					this.previousPowered = decoded.isPreviousPowered();
					this.frontFacing = decoded.getFrontFacing();
				});

		if (compoundTag.contains("inventoryId")) {
			this.inventoryId = compoundTag.getInt("inventoryId");
		}
		if (compoundTag.contains("previousPowered")) {
			this.previousPowered = compoundTag.getBoolean("previousPowered");
		}
		if (compoundTag.contains("frontFacing")) {
			try {
				this.frontFacing = Direction.valueOf(compoundTag.getString("frontFacing"));
			} catch (IllegalArgumentException e) {
				this.frontFacing = Direction.NORTH;
			}
		}
	}

	@Override
	public Component getName() {
		if (this.getCustomName() != null && !this.getCustomName().isEmpty()) {
			return Component.literal(this.getCustomName());
		}
		return Component.translatable("gui.sfmflow.observer_trigger");
	}
}