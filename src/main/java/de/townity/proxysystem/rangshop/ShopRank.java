package de.townity.proxysystem.rangshop;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Freischaltbare Netzwerk-Ränge (LuckPerms-Gruppen), günstig → teuer.
 * Kosten in Spielzeitpunkten (1 Minute Netzwerk-Spielzeit = 1 Punkt).
 */
public enum ShopRank {
  RITTER(
      "ritter",
      "Ritter",
      180,
      List.of(
          "Goldenes Chat-Prefix",
          "Zugang zu Ritter-Bereichen",
          "Kleine Extras auf Unterservern"
      )
  ),
  BARON(
      "baron",
      "Baron",
      540,
      List.of(
          "Alles von Ritter",
          "Erweitertes Chat-Prefix",
          "Zusätzliche Komfort-Rechte"
      )
  ),
  GRAF(
      "graf",
      "Graf",
      1080,
      List.of(
          "Alles von Baron",
          "Höchstes Spieler-Prefix im Shop",
          "Volle Shop-Vorteile im Netzwerk"
      )
  );

  private final String group;
  private final String displayName;
  private final int cost;
  private final List<String> rights;

  ShopRank(String group, String displayName, int cost, List<String> rights) {
    this.group = group;
    this.displayName = displayName;
    this.cost = cost;
    this.rights = List.copyOf(rights);
  }

  public String group() {
    return group;
  }

  public String displayName() {
    return displayName;
  }

  /** Kosten in Spielzeitpunkten. */
  public int cost() {
    return cost;
  }

  public List<String> rights() {
    return rights;
  }

  /** 1 = Ritter, 2 = Baron, 3 = Graf. */
  public int order() {
    return ordinal() + 1;
  }

  public Optional<ShopRank> previous() {
    int index = ordinal() - 1;
    if (index < 0) {
      return Optional.empty();
    }
    return Optional.of(values()[index]);
  }

  public static int totalCost() {
    return Arrays.stream(values()).mapToInt(ShopRank::cost).sum();
  }

  public static Optional<ShopRank> byGroup(String group) {
    if (group == null || group.isBlank()) {
      return Optional.empty();
    }
    String key = group.toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(rank -> rank.group.equals(key)).findFirst();
  }

  public static Optional<ShopRank> byKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    String normalized = key.toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(rank -> rank.group.equals(normalized) || rank.name().equalsIgnoreCase(normalized))
        .findFirst();
  }
}
