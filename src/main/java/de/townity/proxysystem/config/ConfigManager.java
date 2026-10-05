package de.townity.proxysystem.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import de.townity.proxysystem.util.MessageUtil;

public final class ConfigManager {

  private final Path dataDirectory;
  private final Path configPath;
  private final Path maintenanceStatePath;
  private final Logger logger;
  private volatile PluginConfig config;
  private volatile PluginConfig.MaintenanceState maintenanceState;

  public ConfigManager(Path dataDirectory, Logger logger) {
    this.dataDirectory = dataDirectory;
    this.configPath = dataDirectory.resolve("config.yml");
    this.maintenanceStatePath = dataDirectory.resolve("maintenance-state.yml");
    this.logger = logger;
    this.config = defaults();
    this.maintenanceState = new PluginConfig.MaintenanceState(false, Set.of());
  }

  public PluginConfig config() {
    return config;
  }

  public PluginConfig.MaintenanceState maintenanceState() {
    return maintenanceState;
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
      maintenanceState = loadMaintenanceState();
    } catch (IOException e) {
      logger.error("Failed to load config.yml, using defaults", e);
      config = defaults();
      maintenanceState = new PluginConfig.MaintenanceState(false, Set.of());
    }
  }

  public boolean reload() {
    try {
      if (!Files.exists(configPath)) {
        load();
        return true;
      }
      config = parse(Files.readString(configPath));
      maintenanceState = loadMaintenanceState();
      return true;
    } catch (Exception e) {
      logger.error("Failed to reload config.yml", e);
      return false;
    }
  }

  public synchronized void setNetworkMaintenance(boolean enabled) {
    maintenanceState = new PluginConfig.MaintenanceState(enabled, maintenanceState.servers());
    saveMaintenanceState();
  }

  public synchronized void setServerMaintenance(String serverName, boolean enabled) {
    Set<String> servers = new LinkedHashSet<>(maintenanceState.servers());
    String canonical = findCanonical(servers, serverName);
    if (enabled) {
      if (canonical == null) {
        servers.add(serverName);
      }
    } else if (canonical != null) {
      servers.remove(canonical);
    }
    maintenanceState = new PluginConfig.MaintenanceState(maintenanceState.networkEnabled(), Set.copyOf(servers));
    saveMaintenanceState();
  }

  private static String findCanonical(Set<String> servers, String name) {
    for (String entry : servers) {
      if (entry.equalsIgnoreCase(name)) {
        return entry;
      }
    }
    return null;
  }

  private PluginConfig.MaintenanceState loadMaintenanceState() {
    if (!Files.isRegularFile(maintenanceStatePath)) {
      return new PluginConfig.MaintenanceState(false, Set.of());
    }
    try {
      Map<String, Object> root = new Yaml().load(Files.readString(maintenanceStatePath));
      if (root == null) {
        return new PluginConfig.MaintenanceState(false, Set.of());
      }
      boolean network = asBoolean(root.get("network-enabled"), false);
      Set<String> servers = new LinkedHashSet<>(asStringList(root.get("servers")));
      return new PluginConfig.MaintenanceState(network, Set.copyOf(servers));
    } catch (Exception e) {
      logger.error("Failed to load maintenance-state.yml", e);
      return new PluginConfig.MaintenanceState(false, Set.of());
    }
  }

  private void saveMaintenanceState() {
    try {
      Files.createDirectories(dataDirectory);
      Map<String, Object> root = new LinkedHashMap<>();
      root.put("network-enabled", maintenanceState.networkEnabled());
      root.put("servers", new ArrayList<>(maintenanceState.servers()));

      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      options.setPrettyFlow(true);
      String dump = new Yaml(options).dump(root);
      String header = """
          # Persisted by TownityProxySystem – survives proxy restarts.
          # Prefer /wartung commands over editing this file while the proxy is running.
          """;
      Files.writeString(maintenanceStatePath, header + dump, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Failed to save maintenance-state.yml", e);
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
    String prefix = asString(root.get("prefix"), MessageUtil.DEFAULT_PREFIX);
    String disconnectPrefix = asString(root.get("disconnect-prefix"), MessageUtil.DEFAULT_DISCONNECT_PREFIX);

    Map<String, Object> messagesMap = asMap(root.get("messages"));
    PluginConfig.Messages messages = new PluginConfig.Messages(
        asString(messagesMap.get("full"), "&7Der Server ist aktuell &6voll&7! &7(&6%online%&7/&6%max%&7)"),
        asString(messagesMap.get("reload-success"), "&7Die Config wurde &6erfolgreich &7neu geladen&7. &7Max: &6%max%"),
        asString(messagesMap.get("reload-failed"), "&7Der Reload ist &6fehlgeschlagen&7. Siehe Konsole."),
        asString(messagesMap.get("no-permission"), "&7Dazu hast du &6keine Berechtigung&7."),
        asString(messagesMap.get("usage"), "&7Verwendung: &6/playerlimit reload &7| &6/wartung")
    );

    Map<String, Object> motdMap = asMap(root.get("motd"));
    List<String> lines = asStringList(motdMap.get("lines"));
    if (lines.isEmpty()) {
      lines = List.of(
          "&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■",
          "&6       ⚙ &6ᴄɪᴛʏʙᴜɪʟᴅ &e• &6ᴇᴠᴇɴᴛꜱ &e• &6ᴄᴏᴍᴍᴜɴɪᴛʏ ⚙"
      );
    }

    PluginConfig.Motd motd = new PluginConfig.Motd(
        asBoolean(motdMap.get("enabled"), true),
        asString(motdMap.get("format"), "auto").toLowerCase(),
        asString(motdMap.get("file"), ""),
        List.copyOf(lines)
    );

    Map<String, Object> maintenanceMap = asMap(root.get("maintenance"));
    Map<String, Object> maintenanceMessagesMap = asMap(maintenanceMap.get("messages"));
    Map<String, Object> maintenanceMotdMap = asMap(maintenanceMap.get("motd"));
    List<String> maintenanceMotdLines = asStringList(maintenanceMotdMap.get("lines"));
    if (maintenanceMotdLines.isEmpty()) {
      maintenanceMotdLines = List.of(
          "&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■",
          "&6          ⚙ &6ᴡᴀʀᴛᴜɴɢ &e• &6ʙᴀʟᴅ &e• &6ᴢᴜʀüᴄᴋ ⚙"
      );
    }

    PluginConfig.MaintenanceMessages maintenanceMessages = new PluginConfig.MaintenanceMessages(
        asString(maintenanceMessagesMap.get("network-kick"), "&7Das Netzwerk ist derzeit in &6Wartung&7."),
        asString(maintenanceMessagesMap.get("server-kick"), "&7Der Server &6%server% &7ist in &6Wartung&7."),
        asString(maintenanceMessagesMap.get("network-denied"), "&7Das Netzwerk ist derzeit in &6Wartung&7."),
        asString(maintenanceMessagesMap.get("server-denied"), "&7Du kannst &6%server% &7gerade nicht betreten (&6Wartung&7)."),
        asString(maintenanceMessagesMap.get("enabled"), "&7Netzwerk-Wartung wurde &6aktiviert&7."),
        asString(maintenanceMessagesMap.get("disabled"), "&7Netzwerk-Wartung wurde &6deaktiviert&7."),
        asString(maintenanceMessagesMap.get("server-enabled"), "&7Wartung für &6%server% &7wurde &6aktiviert&7."),
        asString(maintenanceMessagesMap.get("server-disabled"), "&7Wartung für &6%server% &7wurde &6deaktiviert&7."),
        asString(maintenanceMessagesMap.get("status"), "&7Netzwerk: &6%network% &7| Server: &6%servers%"),
        asString(maintenanceMessagesMap.get("unknown-server"), "&7Unbekannter Server: &6%server%"),
        asString(maintenanceMessagesMap.get("usage"), "&7Verwendung: &6/wartung &7| &6an&7/&6aus &7| &6server <name> <an|aus> &7| &6status")
    );

    PluginConfig.Maintenance maintenance = new PluginConfig.Maintenance(
        asString(maintenanceMap.get("bypass-permission"), "townity.wartungssystem.bypass"),
        asBoolean(maintenanceMap.get("kick-on-enable"), true),
        asBoolean(maintenanceMap.get("block-join"), true),
        asBoolean(maintenanceMotdMap.get("enabled"), true),
        List.copyOf(maintenanceMotdLines),
        asString(maintenanceMotdMap.get("format"), asString(motdMap.get("format"), "auto")).toLowerCase(),
        maintenanceMessages
    );

    return new PluginConfig(
        maxPlayers,
        bypassCountsAsSlot,
        bypassPermission,
        updatePingMaxPlayers,
        prefix,
        disconnectPrefix,
        messages,
        motd,
        maintenance
    );
  }

  private static PluginConfig defaults() {
    return new PluginConfig(
        100,
        false,
        "townity.playerlimit.bypass",
        true,
        MessageUtil.DEFAULT_PREFIX,
        MessageUtil.DEFAULT_DISCONNECT_PREFIX,
        new PluginConfig.Messages(
            "&7Der Server ist aktuell &6voll&7! &7(&6%online%&7/&6%max%&7)",
            "&7Die Config wurde &6erfolgreich &7neu geladen&7. &7Max: &6%max%",
            "&7Der Reload ist &6fehlgeschlagen&7. Siehe Konsole.",
            "&7Dazu hast du &6keine Berechtigung&7.",
            "&7Verwendung: &6/playerlimit reload &7| &6/wartung"
        ),
        new PluginConfig.Motd(
            true,
            "auto",
            "",
            List.of(
                "&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■",
                "&6       ⚙ &6ᴄɪᴛʏʙᴜɪʟᴅ &e• &6ᴇᴠᴇɴᴛꜱ &e• &6ᴄᴏᴍᴍᴜɴɪᴛʏ ⚙"
            )
        ),
        new PluginConfig.Maintenance(
            "townity.wartungssystem.bypass",
            true,
            true,
            true,
            List.of(
                "&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■",
                "&6          ⚙ &6ᴡᴀʀᴛᴜɴɢ &e• &6ʙᴀʟᴅ &e• &6ᴢᴜʀüᴄᴋ ⚙"
            ),
            "auto",
            new PluginConfig.MaintenanceMessages(
                "&7Das Netzwerk ist derzeit in &6Wartung&7.",
                "&7Der Server &6%server% &7ist in &6Wartung&7.",
                "&7Das Netzwerk ist derzeit in &6Wartung&7.",
                "&7Du kannst &6%server% &7gerade nicht betreten (&6Wartung&7).",
                "&7Netzwerk-Wartung wurde &6aktiviert&7.",
                "&7Netzwerk-Wartung wurde &6deaktiviert&7.",
                "&7Wartung für &6%server% &7wurde &6aktiviert&7.",
                "&7Wartung für &6%server% &7wurde &6deaktiviert&7.",
                "&7Netzwerk: &6%network% &7| Server: &6%servers%",
                "&7Unbekannter Server: &6%server%",
                "&7Verwendung: &6/wartung &7| &6an&7/&6aus &7| &6server <name> <an|aus> &7| &6status"
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
