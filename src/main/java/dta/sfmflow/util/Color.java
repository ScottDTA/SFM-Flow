package dta.sfmflow.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Public API enumeration defining the color palette used for visual workspace
 * component masks [3]. Stores compile-time default values but routes active
 * color queries through an injectable resolver [3].
 */
public enum Color implements StringRepresentable {
	BLACK("black", ChatFormatting.BLACK, 0x1D1D21, 0xFFFFFF), BLUE("blue", ChatFormatting.BLUE, 0x3C44AA, 0xFFFFFF),
	GREEN("green", ChatFormatting.GREEN, 0x5E7C16, 0xFFFFFF),
	CYAN("cyan", ChatFormatting.DARK_AQUA, 0x169C9C, 0xFFFFFF), RED("red", ChatFormatting.RED, 0xB02E26, 0xFFFFFF),
	PURPLE("purple", ChatFormatting.DARK_PURPLE, 0x8932B8, 0xFFFFFF),
	ORANGE("orange", ChatFormatting.GOLD, 0xF9801D, 0x404040),
	LIGHT_GRAY("light_gray", ChatFormatting.GRAY, 0x9D9D97, 0x404040),
	GRAY("gray", ChatFormatting.DARK_GRAY, 0x474F52, 0xFFFFFF),
	LIGHT_BLUE("light_blue", ChatFormatting.AQUA, 0x3AB3DA, 0x404040),
	LIME("lime", ChatFormatting.GREEN, 0x80C71F, 0x404040),
	// TURQUOISE("turquoise", ChatFormatting.AQUA, 0x169C9C, 0xFFFFFF),
	BROWN("brown", ChatFormatting.GOLD, 0x835432, 0xFFFFFF),
	PINK("pink", ChatFormatting.LIGHT_PURPLE, 0xF38BAA, 0x404040),
	MAGENTA("magenta", ChatFormatting.LIGHT_PURPLE, 0xC74EBD, 0xFFFFFF),
	YELLOW("yellow", ChatFormatting.YELLOW, 0xFED83D, 0x404040),
	WHITE("white", ChatFormatting.WHITE, 0xF9FFFE, 0x404040);

	private final String name;
	private final ChatFormatting chatFormat;
	private final int defaultHexColor;
	private final int defaultHexTextColor;

	public static final Codec<Color> CODEC = StringRepresentable.fromEnum(Color::values);
	public static final StreamCodec<ByteBuf, Color> STREAM_CODEC = ByteBufCodecs.idMapper(id -> Color.values()[id],
			Enum::ordinal);

	// Interface supplier to fetch config values without referencing ClientConfig
	// directly in common code [3]
	private static java.util.function.BiFunction<Color, Boolean, Integer> colorResolver = (color,
			isText) -> isText ? color.defaultHexTextColor : color.defaultHexColor;

	Color(String name, ChatFormatting chatFormat, int hexColor, int hexTextColor) {
		this.name = name;
		this.chatFormat = chatFormat;
		this.defaultHexColor = hexColor;
		this.defaultHexTextColor = hexTextColor;
	}

	/**
	 * Configures the active client resolver delegate [3]. Called exclusively on the
	 * physical client setup phase [3].
	 *
	 * @param resolver the resolver function mapping Color and text-state to integer
	 *                 [3]
	 */
	public static void setResolver(java.util.function.BiFunction<Color, Boolean, Integer> resolver) {
		colorResolver = resolver;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int getDefaultHexColor() {
		return defaultHexColor;
	}

	public int getDefaultHexTextColor() {
		return defaultHexTextColor;
	}

	public int getHexColor() {
		return colorResolver.apply(this, false);
	}

	public int getHexTextColor() {
		return colorResolver.apply(this, true);
	}

	public ChatFormatting getChatFormat() {
		return chatFormat;
	}
}