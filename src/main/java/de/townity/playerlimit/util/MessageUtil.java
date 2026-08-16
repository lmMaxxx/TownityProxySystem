package de.townity.playerlimit.util;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Parses MOTD / kick messages from MiniMessage, legacy &amp;/§ codes (MOTD websites),
 * or Adventure JSON components.
 */
public final class MessageUtil {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_SECTION =
      LegacyComponentSerializer.legacySection();
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.legacyAmpersand();
  private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

  private MessageUtil() {}

  public static Component parse(String input, String format) {
    if (input == null || input.isBlank()) {
      return Component.empty();
    }

    String normalized = input.replace("\\n", "\n");
    String mode = format == null ? "auto" : format.toLowerCase(Locale.ROOT);

    return switch (mode) {
      case "minimessage", "mini", "mm" -> MINI_MESSAGE.deserialize(normalized);
      case "legacy", "bukkit", "ampersand" -> parseLegacy(normalized);
      case "json", "adventure" -> parseJson(normalized);
      default -> parseAuto(normalized);
    };
  }

  public static Component parseAuto(String input) {
    String trimmed = input.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      try {
        return GSON.deserialize(trimmed);
      } catch (Exception ignored) {
        // fall through
      }
    }
    if (looksLikeMiniMessage(trimmed)) {
      return MINI_MESSAGE.deserialize(input);
    }
    return parseLegacy(input);
  }

  public static Component joinLines(Iterable<String> lines, String format, Placeholders placeholders) {
    Component result = Component.empty();
    boolean first = true;
    for (String line : lines) {
      if (line == null) {
        continue;
      }
      Component parsed = parse(placeholders.apply(line), format);
      if (first) {
        result = parsed;
        first = false;
      } else {
        result = result.append(Component.newline()).append(parsed);
      }
    }
    return result;
  }

  private static Component parseLegacy(String input) {
    if (input.indexOf('§') >= 0) {
      return LEGACY_SECTION.deserialize(input);
    }
    return LEGACY_AMPERSAND.deserialize(input);
  }

  private static Component parseJson(String input) {
    try {
      return GSON.deserialize(input.trim());
    } catch (Exception e) {
      return MINI_MESSAGE.deserialize(input);
    }
  }

  private static boolean looksLikeMiniMessage(String input) {
    return input.indexOf('<') >= 0 && input.indexOf('>') > input.indexOf('<');
  }

  public record Placeholders(int online, int max) {
    public String apply(String text) {
      int remaining = Math.max(0, max - online);
      return text
          .replace("%online%", String.valueOf(online))
          .replace("%max%", String.valueOf(max))
          .replace("%remaining%", String.valueOf(remaining));
    }
  }
}
