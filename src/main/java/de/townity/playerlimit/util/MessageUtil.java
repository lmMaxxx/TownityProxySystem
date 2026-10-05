package de.townity.playerlimit.util;

import java.util.Locale;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Parses messages/MOTDs from MiniMessage, legacy {@code &}/{@code §} (incl. hex from MOTD websites),
 * or Adventure JSON components.
 */
public final class MessageUtil {

  public static final String DEFAULT_PREFIX = "&6&lTOWNITY.DE &7⇒ ";
  public static final String DEFAULT_DISCONNECT_PREFIX = "&6&lTOWNITY.DE";

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

  /** Supports {@code &a}, {@code &#RRGGBB}, {@code &x&r&r&g&g&b&b}. */
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.builder()
          .character('&')
          .hexColors()
          .useUnusualXRepeatedCharacterHexFormat()
          .build();

  /** Supports {@code §a}, {@code §#RRGGBB}, {@code §x§r§r§g§g§b§b}. */
  private static final LegacyComponentSerializer LEGACY_SECTION =
      LegacyComponentSerializer.builder()
          .character('§')
          .hexColors()
          .useUnusualXRepeatedCharacterHexFormat()
          .build();

  private static final Pattern LEGACY_CODE =
      Pattern.compile("(?i)(?:[&§]#[0-9a-f]{6}|[&§]x(?:[&§][0-9a-f]){6}|[&§][0-9a-fk-or])");

  /** Explicit false so kick screens / children do not inherit {@code &l} from the prefix. */
  private static final Style RESET_DECORATIONS = Style.style()
      .decoration(TextDecoration.BOLD, false)
      .decoration(TextDecoration.ITALIC, false)
      .decoration(TextDecoration.UNDERLINED, false)
      .decoration(TextDecoration.STRIKETHROUGH, false)
      .decoration(TextDecoration.OBFUSCATED, false)
      .build();

  private MessageUtil() {}

  public static Component parse(String input) {
    return parse(input, "auto");
  }

  /** Parses a chat message and prepends the configured Townity prefix (with arrow). */
  public static Component parsePrefixed(String prefix, String body, String format) {
    String safePrefix = prefix == null || prefix.isBlank() ? DEFAULT_PREFIX : prefix;
    String safeBody = body == null ? "" : body;
    return parse(safePrefix + safeBody, format);
  }

  /**
   * Disconnect-/Kick-Screen: Zeile 1 = Prefix ohne Pfeil, Zeile 2 = Text.
   * Body starts on a fresh style so {@code &l} from the title does not carry over
   * (Adventure children inherit parent decorations; legacy {@code §l} also survives newlines).
   */
  public static Component parseDisconnect(String disconnectPrefix, String body, String format) {
    String title = disconnectPrefix == null || disconnectPrefix.isBlank()
        ? DEFAULT_DISCONNECT_PREFIX
        : disconnectPrefix;
    String safeBody = body == null ? "" : body;
    return Component.empty()
        .append(parse(title, format))
        .append(Component.newline())
        .append(Component.empty().style(RESET_DECORATIONS).append(parse(withLegacyReset(safeBody), format)));
  }

  private static String withLegacyReset(String body) {
    if (body.isEmpty()) {
      return body;
    }
    if (body.regionMatches(true, 0, "&r", 0, 2) || body.regionMatches(true, 0, "§r", 0, 2)) {
      return body;
    }
    return "&r" + body;
  }

  public static Component parse(String input, String format) {
    if (input == null || input.isBlank()) {
      return Component.empty();
    }

    String normalized = input.replace("\\n", "\n");
    String mode = format == null ? "auto" : format.toLowerCase(Locale.ROOT);

    return switch (mode) {
      case "minimessage", "mini", "mm" -> MINI_MESSAGE.deserialize(normalized);
      case "legacy", "bukkit", "ampersand", "section" -> parseLegacy(normalized);
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
    // Prefer &/§ (MOTD websites) over MiniMessage when legacy codes are present
    if (containsLegacyCodes(input)) {
      return parseLegacy(input);
    }
    if (looksLikeMiniMessage(trimmed)) {
      return MINI_MESSAGE.deserialize(input);
    }
    return Component.text(input);
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
    // Normalize to one style so mixed "&aHello §bWorld" works
    if (input.indexOf('§') >= 0 && input.indexOf('&') >= 0) {
      return LEGACY_SECTION.deserialize(input.replace('&', '§'));
    }
    if (input.indexOf('§') >= 0) {
      return LEGACY_SECTION.deserialize(input);
    }
    return LEGACY_AMPERSAND.deserialize(input);
  }

  private static Component parseJson(String input) {
    try {
      return GSON.deserialize(input.trim());
    } catch (Exception e) {
      return parseAuto(input);
    }
  }

  private static boolean containsLegacyCodes(String input) {
    return LEGACY_CODE.matcher(input).find();
  }

  private static boolean looksLikeMiniMessage(String input) {
    return input.indexOf('<') >= 0 && input.indexOf('>') > input.indexOf('<');
  }

  public record Placeholders(int online, int max, String server) {
    public Placeholders(int online, int max) {
      this(online, max, "");
    }

    public String apply(String text) {
      int remaining = Math.max(0, max - online);
      String result = text
          .replace("%online%", String.valueOf(online))
          .replace("%max%", String.valueOf(max))
          .replace("%remaining%", String.valueOf(remaining));
      if (server != null && !server.isEmpty()) {
        result = result.replace("%server%", server);
      }
      return result;
    }
  }
}
