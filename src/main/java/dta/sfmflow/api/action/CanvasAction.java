package dta.sfmflow.api.action;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Public API enumeration defining basic workspace operations performable on
 * canvas nodes [3].
 */
public enum CanvasAction implements StringRepresentable {
	COPY("copy"), DELETE("delete"), TOGGLE_OPEN("toggle_open");

	private final String name;

	public static final Codec<CanvasAction> CODEC = StringRepresentable.fromEnum(CanvasAction::values);
	public static final StreamCodec<ByteBuf, CanvasAction> STREAM_CODEC = ByteBufCodecs
			.idMapper(id -> CanvasAction.values()[id], Enum::ordinal);

	CanvasAction(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}