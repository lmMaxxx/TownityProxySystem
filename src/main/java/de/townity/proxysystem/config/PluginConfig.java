package de.townity.proxysystem.config;

import java.util.Set;

/** Runtime settings. Only {@code maxPlayers} is loaded from config.yml. */
public record PluginConfig(int maxPlayers) {

  /** Wartungszustand – separat in maintenance-state.yml. */
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
