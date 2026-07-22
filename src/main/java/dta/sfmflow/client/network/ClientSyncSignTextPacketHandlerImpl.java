package dta.sfmflow.client.network;

import dta.sfmflow.networking.IPacketHandler;
import dta.sfmflow.networking.packets.clientbound.SyncSignTextPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class ClientSyncSignTextPacketHandlerImpl implements IPacketHandler<SyncSignTextPacket> {
	@Override
	public void handle(SyncSignTextPacket payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null && payload.pos() != null) {
				BlockPos pos = payload.pos();
				BlockEntity be = mc.level.getBlockEntity(pos);
				if (be instanceof SignBlockEntity sign) {
					// 1. Rebuild Front Text in client memory [3]
					SignText frontText = sign.getFrontText();
					for (int i = 0; i < 4; i++) {
						if (i < payload.frontLines().size()) {
							frontText = frontText.setMessage(i, Component.literal(payload.frontLines().get(i)));
						}
					}
					frontText = frontText.setColor(payload.frontColor());
					frontText = frontText.setHasGlowingText(payload.frontGlow());
					sign.setText(frontText, true);

					// 2. Rebuild Back Text in client memory [3]
					SignText backText = sign.getBackText();
					for (int i = 0; i < 4; i++) {
						if (i < payload.backLines().size()) {
							backText = backText.setMessage(i, Component.literal(payload.backLines().get(i)));
						}
					}
					backText = backText.setColor(payload.backColor());
					backText = backText.setHasGlowingText(payload.backGlow());
					sign.setText(backText, false);

					// 3. Update global waxed state [3]
					sign.setWaxed(payload.waxed());
					sign.setChanged();

					// 4. Force an unconditional client-side world rendering cache rebuild [3]
					BlockState state = mc.level.getBlockState(pos);
					if (mc.levelRenderer != null) {
						mc.levelRenderer.blockChanged(mc.level, pos, state, state, 3);
					}
				}
			}
		});
	}
}