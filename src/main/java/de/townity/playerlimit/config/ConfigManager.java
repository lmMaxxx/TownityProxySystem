package de.townity.playerlimit.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

public final class ConfigManager {

  private final Path dataDirectory;
  private final Path configPath;
  private final Logger logger;
  private volatile PluginConfig config;

  public ConfigManager(Path dataDirectory, Logger logger) {
    this.dataDirectory = dataDirectory;
    this.configPath = dataDirectory.resolve("config.yml");
    this.logger = logger;
    this.config = defaults();
  }

  public PluginConfig config() {
    return config;
  }

  public void load() {
    try {
      Files.createDirectories(dataDirectory);
      if (!Files.exists(configPath)) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
          if (in == null) {
            throw new IOException("Bundled config.yml missing");
          }
          Files.copy(in, configPath);
        }
      }
      config = parse(Files.readString(configPath));
    } catch (IOException e) {
      logger.error("Failed to load config.yml, using defaults", e);
      config = defaults();
    }
  }

  public boolean reload() {
    try {
      if (!Files.exists(configPath)) {
        load();
        return true;
      }
      config = parse(Files.readString(configPath));
      return true;
    } catch (Exception e) {
      logger.error("Failed to reload config.yml", e);
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private PluginConfig parse(String raw) {
    Map<String, Object> root = new Yaml().load(raw);
    if (root == null) {
      return defaults();
    }

    int maxPlayers = Math.max(0, asInt(root.get("max-players"), 100));
    boolean bypassCountsAsSlot = asBoolean(root.get("bypass-counts-as-slot"), false);
    String bypassPermission = asString(root.get("bypass-permission"), "townity.playerlimit.bypass");
    boolean updatePingMaxPlayers = asBoolean(root.get("update-ping-max-players"), true);

    Map<String, Object> messagesMap = asMap(root.get("messages"));
    PluginConfig.Messages messages = new PluginConfig.Messages(
        asString(messagesMap.get("full"), "<red>Der Server ist voll!</red> <gray>(%online%/%max%)</gray>"),
        asString(messagesMap.get("reload-success"), "<green>TownityPlayerLimit neu geladen.</green> <gray>Max: %max%</gray>"),
        asString(messagesMap.get("reload-failed"), "<red>Reload fehlgeschlagen. Siehe Konsole.</red>"),
        asString(messagesMap.get("no-permission"), "<red>Dazu hast du keine Berechtigung.</red>"),
        asString(messagesMap.get("usage"), "<gray>Usage:</gray> <white>/playerlimit reload</white>")
    );

    Map<String, Object> motdMap = asMap(root.get("motd"));
    List<String> lines = asStringList(motdMap.get("lines"));
    if (lines.isEmpty()) {
      lines = List.of(
          "<gradient:#57F287:#5865F2>Townity</gradient>",
          "<gray>Online:</gray> <green>%online%</green><gray>/</gray><white>%max%</white>"
      );
    }

    PluginConfig.Motd motd = new PluginConfig.Motd(
        asBoolean(motdMap.get("enabled"), false),
        asString(motdMap.get("format"), "auto").toLowerCase(),
        asString(motdMap.get("file"), ""),
        List.copyOf(lines)
    );

    return new PluginConfig(maxPlayers, bypassCountsAsSlot, bypassPermission, updatePingMaxPlayers, messages, motd);
  }

  private static PluginConfig defaults() {
    return new PluginConfig(
        100,
        false,
        "townity.playerlimit.bypass",
        true,
        new PluginConfig.Messages(
            "<red>Der Server ist voll!</red> <gray>(%online%/%max%)</gray>",
            "<green>TownityPlayerLimit neu geladen.</green> <gray>Max: %max%</gray>",
            "<red>Reload fehlgeschlagen. Siehe Konsole.</red>",
            "<red>Dazu hast du keine Berechtigung.</red>",
            "<gray>Usage:</gray> <white>/playerlimit reload</white>"
        ),
        new PluginConfig.Motd(
            false,
            "auto",
            "",
            List.of(
                "<gradient:#57F287:#5865F2>Townity</gradient>",
                "<gray>Online:</gray> <green>%online%</green><gray>/</gray><white>%max%</white>"
            )
        )
    );
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return Collections.emptyMap();
  }

  private static List<String> asStringList(Object value) {
    if (!(value instanceof List<?> list)) {
      return new ArrayList<>();
    }
    List<String> result = new ArrayList<>();
    for (Object entry : list) {
      if (entry != null) {
        result.add(String.valueOf(entry));
      }
    }
    return result;
  }

  private static String asString(Object value, String fallback) {
    return value == null ? fallback : String.valueOf(value);
  }

  private static int asInt(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException ignored) {
        return fallback;
      }
    }
    return fallback;
  }

  private static boolean asBoolean(Object value, boolean fallback) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof String text) {
      return Boolean.parseBoolean(text.trim());
    }
    return fallback;
  }
}
