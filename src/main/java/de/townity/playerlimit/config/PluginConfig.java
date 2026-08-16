package de.townity.playerlimit.config;

import java.util.List;

public record PluginConfig(
    int maxPlayers,
    boolean bypassCountsAsSlot,
    String bypassPermission,
    boolean updatePingMaxPlayers,
    Messages messages,
    Motd motd
) {

  public record Messages(
      String full,
      String reloadSuccess,
      String reloadFailed,
      String noPermission,
      String usage
  ) {}

  public record Motd(
      boolean enabled,
      String format,
      String file,
      List<String> lines
  ) {}
}
