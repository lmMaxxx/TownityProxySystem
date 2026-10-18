package de.townity.proxysystem.rangshop;

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
 * Persistiert Spielzeitpunkte und bisher verdiente Punkte (Cap = Summe aller Rang-Kosten).
 */
public final class PlaytimePointsStore {

  private final Path path;
  private final Logger logger;
  private final ConcurrentHashMap<UUID, PlayerPoints> players = new ConcurrentHashMap<>();

  public PlaytimePointsStore(Path dataDirectory, Logger logger) {
    this.path = dataDirectory.resolve("rangshop-points.yml");
    this.logger = logger;
  }

  public void load() {
    players.clear();
    if (!Files.isRegularFile(path)) {
      return;
    }
    try {
      Map<String, Object> root = new Yaml().load(Files.readString(path));
      if (root == null) {
        return;
      }
      Object raw = root.get("players");
      if (!(raw instanceof Map<?, ?> map)) {
        return;
      }
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() == null || entry.getValue() == null) {
          continue;
        }
        try {
          UUID uuid = UUID.fromString(String.valueOf(entry.getKey()));
          players.put(uuid, parseEntry(entry.getValue()));
        } catch (IllegalArgumentException ignored) {
          // skip invalid uuid
        }
      }
    } catch (Exception e) {
      logger.error("Failed to load rangshop-points.yml", e);
    }
  }

  public int points(UUID uuid) {
    PlayerPoints data = players.get(uuid);
    return data == null ? 0 : data.points;
  }

  public int lifetimeEarned(UUID uuid) {
    PlayerPoints data = players.get(uuid);
    return data == null ? 0 : data.lifetimeEarned;
  }

  /**
   * Gutschrift bis zum Lebenszeit-Cap. Gibt tatsächlich gutgeschriebene Punkte zurück.
   */
  public synchronized int credit(UUID uuid, int amount, int lifetimeCap) {
    if (amount <= 0) {
      return 0;
    }
    PlayerPoints data = players.computeIfAbsent(uuid, id -> new PlayerPoints(0, 0));
    int room = Math.max(0, lifetimeCap - data.lifetimeEarned);
    int granted = Math.min(amount, room);
    if (granted <= 0) {
      return 0;
    }
    data.points += granted;
    data.lifetimeEarned += granted;
    save();
    return granted;
  }

  public synchronized boolean spend(UUID uuid, int amount) {
    if (amount <= 0) {
      return true;
    }
    PlayerPoints data = players.get(uuid);
    if (data == null || data.points < amount) {
      return false;
    }
    data.points -= amount;
    save();
    return true;
  }

  /** Rückbuchung ohne Lifetime-Cap (z. B. fehlgeschlagener LuckPerms-Kauf). */
  public synchronized void refund(UUID uuid, int amount) {
    if (amount <= 0) {
      return;
    }
    PlayerPoints data = players.computeIfAbsent(uuid, id -> new PlayerPoints(0, 0));
    data.points += amount;
    save();
  }

  private void save() {
    try {
      Files.createDirectories(path.getParent());
      Map<String, Object> playersOut = new LinkedHashMap<>();
      players.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("points", entry.getValue().points);
            row.put("lifetime", entry.getValue().lifetimeEarned);
            playersOut.put(entry.getKey().toString(), row);
          });

      Map<String, Object> root = new LinkedHashMap<>();
      root.put("players", playersOut);

      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      options.setPrettyFlow(true);
      String dump = new Yaml(options).dump(root);
      String header = """
          # Persisted by TownityProxySystem – Rangshop Spielzeitpunkte.
          # points = aktuelles Guthaben, lifetime = jemals verdient (Cap).
          """;
      Files.writeString(path, header + dump, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Failed to save rangshop-points.yml", e);
    }
  }

  private static PlayerPoints parseEntry(Object value) {
    if (value instanceof Number number) {
      int points = Math.max(0, number.intValue());
      return new PlayerPoints(points, points);
    }
    if (value instanceof Map<?, ?> map) {
      int points = Math.max(0, asInt(map.get("points")));
      int lifetime = Math.max(points, asInt(map.get("lifetime")));
      return new PlayerPoints(points, lifetime);
    }
    return new PlayerPoints(0, 0);
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

  private static final class PlayerPoints {
    private int points;
    private int lifetimeEarned;

    private PlayerPoints(int points, int lifetimeEarned) {
      this.points = points;
      this.lifetimeEarned = lifetimeEarned;
    }
  }
}
