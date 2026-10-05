package de.townity.proxysystem.config;

import java.util.List;
import java.util.Set;

public record PluginConfig(
    int maxPlayers,
    boolean bypassCountsAsSlot,
    String bypassPermission,
    boolean updatePingMaxPlayers,
    String prefix,
    String disconnectPrefix,
    Messages messages,
    Motd motd,
    Maintenance maintenance
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

  public record Maintenance(
      String bypassPermission,
      boolean kickOnEnable,
      boolean blockJoin,
      boolean overrideMotd,
      List<String> motdLines,
      String format,
      MaintenanceMessages messages
  ) {}

  public record MaintenanceMessages(
      String networkKick,
      String serverKick,
      String networkDenied,
      String serverDenied,
      String enabled,
      String disabled,
      String serverEnabled,
      String serverDisabled,
      String status,
      String unknownServer,
      String usage
  ) {}

  /** Runtime state persisted separately across restarts. */
  public record MaintenanceState(
      boolean networkEnabled,
      Set<String> servers
  ) {
    public boolean isServerEnabled(String serverName) {
      if (serverName == null) {
        return false;
      }
      for (String entry : servers) {
        if (entry.equalsIgnoreCase(serverName)) {
          return true;
        }
      }
      return false;
    }
  }
}
