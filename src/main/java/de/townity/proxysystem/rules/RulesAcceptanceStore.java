package de.townity.proxysystem.rules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Persists accepted rules versions per player UUID.
 */
public final class RulesAcceptanceStore {

  private final Path path;
  private final Logger logger;
  private final ConcurrentHashMap<UUID, Integer> accepted = new ConcurrentHashMap<>();

  public RulesAcceptanceStore(Path dataDirectory, Logger logger) {
    this.path = dataDirectory.resolve("rules-acceptances.yml");
    this.logger = logger;
  }

  public void load() {
    accepted.clear();
    if (!Files.isRegularFile(path)) {
      return;
    }
    try {
      Map<String, Object> root = new Yaml().load(Files.readString(path));
      if (root == null) {
        return;
      }
      Object players = root.get("players");
      if (!(players instanceof Map<?, ?> map)) {
        return;
      }
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() == null || entry.getValue() == null) {
          continue;
        }
        try {
          UUID uuid = UUID.fromString(String.valueOf(entry.getKey()));
          int version = asInt(entry.getValue());
          if (version > 0) {
            accepted.put(uuid, version);
          }
        } catch (IllegalArgumentException ignored) {
          // skip invalid uuid
        }
      }
    } catch (Exception e) {
      logger.error("Failed to load rules-acceptances.yml", e);
    }
  }

  public int acceptedVersion(UUID uuid) {
    return accepted.getOrDefault(uuid, 0);
  }

  public synchronized void setAccepted(UUID uuid, int version) {
    accepted.put(uuid, version);
    save();
  }

  private void save() {
    try {
      Files.createDirectories(path.getParent());
      Map<String, Object> players = new LinkedHashMap<>();
      accepted.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> players.put(entry.getKey().toString(), entry.getValue()));

      Map<String, Object> root = new LinkedHashMap<>();
      root.put("players", players);

      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      options.setPrettyFlow(true);
      String dump = new Yaml(options).dump(root);
      String header = """
          # Persisted by TownityProxySystem – accepted rules versions per UUID.
          """;
      Files.writeString(path, header + dump, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Failed to save rules-acceptances.yml", e);
    }
  }

  private static int asInt(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }
}
