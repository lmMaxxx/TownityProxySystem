package de.townity.proxysystem.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

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
    this.config = new PluginConfig(100);
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
      config = new PluginConfig(100);
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

  /**
   * Updates {@code max-players} in memory and persists it to {@code config.yml}.
   */
  public synchronized boolean setMaxPlayers(int maxPlayers) {
    int sanitized = Math.max(0, maxPlayers);
    config = new PluginConfig(sanitized);
    return persistMaxPlayers(sanitized);
  }

  private boolean persistMaxPlayers(int sanitized) {
    try {
      Files.createDirectories(dataDirectory);
      if (!Files.exists(configPath)) {
        load();
        config = new PluginConfig(sanitized);
      }
      String raw = Files.readString(configPath);
      if (raw.lines().anyMatch(line -> line.matches("^max-players:\\s*.*"))) {
        raw = raw.replaceFirst("(?m)^max-players:\\s*.*$", "max-players: " + sanitized);
      } else {
        raw = "max-players: " + sanitized + "\n" + raw;
      }
      Files.writeString(configPath, raw, StandardCharsets.UTF_8);
      return true;
    } catch (IOException e) {
      logger.error("Failed to save max-players to config.yml", e);
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

  private PluginConfig parse(String raw) {
    Map<String, Object> root = new Yaml().load(raw);
    if (root == null) {
      return new PluginConfig(100);
    }
    return new PluginConfig(Math.max(0, asInt(root.get("max-players"), 100)));
  }

  private static java.util.List<String> asStringList(Object value) {
    if (!(value instanceof java.util.List<?> list)) {
      return new ArrayList<>();
    }
    java.util.List<String> result = new ArrayList<>();
    for (Object entry : list) {
      if (entry != null) {
        result.add(String.valueOf(entry));
      }
    }
    return result;
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
