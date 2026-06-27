package dta.sfmflow;

import dta.sfmflow.util.Color;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.EnumMap;
import java.util.Map;

/**
 * Client-only configuration class managing custom canvas, label hex colors, and GUI scale overrides [3].
 * Config values are built programmatically from the common Color enum to ease future expansion [3].
 */
public class ClientConfig
 {
  private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

  /**
   * Forces a specific GUI scale when opening the Machine Inventory Manager screen [3].
   * Set to 0 to use standard adaptive auto-scaling [3].
   */
  public static final ModConfigSpec.IntValue FORCE_GUI_SCALE;

  /**
   * Holds the background config values built programmatically for each Color [3].
   */
  public static final Map<Color, ModConfigSpec.ConfigValue<String>> BG_CONFIGS = new EnumMap<>(Color.class);

  /**
   * Holds the foreground label text config values built programmatically for each Color [3].
   */
  public static final Map<Color, ModConfigSpec.ConfigValue<String>> TEXT_CONFIGS = new EnumMap<>(Color.class);

  public static final ModConfigSpec SPEC;

  static
   {
    BUILDER.push("general");
    FORCE_GUI_SCALE = BUILDER.comment("Force a specific GUI scale when opening the Machine Inventory Manager screen. Set to 0 to use standard adaptive auto-scaling.")
                             .translation("sfmflow.configuration.forceGuiScale")
                             .defineInRange("forceGuiScale", 0, 0, 8);
    BUILDER.pop();

    BUILDER.comment("Customizable component background and text colors. Supports standard '#RRGGBB' strings.")
           .push("colors");

    for (Color color : Color.values())
     {
      BUILDER.push(color.getSerializedName());

      // Format current compile-time values to hexadecimal standard #RRGGBB strings [3]
      String defaultBg = String.format("#%06X", color.getDefaultHexColor() & 0xFFFFFF);
      ModConfigSpec.ConfigValue<String> bgVal = BUILDER.comment("The background panel and border tint color for " + color.getSerializedName())
                                                       .define("background", defaultBg);
      BG_CONFIGS.put(color, bgVal);

      String defaultText = String.format("#%06X", color.getDefaultHexTextColor() & 0xFFFFFF);
      ModConfigSpec.ConfigValue<String> textVal = BUILDER.comment("The foreground text and label color for " + color.getSerializedName())
                                                         .define("text", defaultText);
      TEXT_CONFIGS.put(color, textVal);

      BUILDER.pop();
     }

    BUILDER.pop();
    SPEC = BUILDER.build();
   }

  /**
   * Defensively parses hexadecimal color strings with integer overflow safety [3].
   * Uses Integer.parseUnsignedInt to safely support high-bit Alpha values [3].
   *
   * @param hexStr the hexadecimal string to parse [3]
   * @param fallback default integer to return if parsing fails [3]
   * @return parsed color integer [3]
   */
  public static int parseHexColor(String hexStr, int fallback)
   {
    if (hexStr == null || hexStr.isEmpty())
     {
      return fallback;
     }
    try
     {
      String cleaned = hexStr.trim();
      if (cleaned.startsWith("#"))
       {
        cleaned = cleaned.substring(1);
       }
      return Integer.parseUnsignedInt(cleaned, 16);
     }
    catch (NumberFormatException e)
     {
      return fallback;
     }
   }
 }