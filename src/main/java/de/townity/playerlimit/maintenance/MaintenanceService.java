package de.townity.playerlimit.maintenance;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.townity.playerlimit.TownityPlayerLimit;
import de.townity.playerlimit.config.PluginConfig;
import de.townity.playerlimit.util.MessageUtil;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;

public final class MaintenanceService {

  private final TownityPlayerLimit plugin;

  public MaintenanceService(TownityPlayerLimit plugin) {
    this.plugin = plugin;
  }

  public boolean isNetworkEnabled() {
    return plugin.configManager().maintenanceState().networkEnabled();
  }

  public boolean isServerEnabled(String serverName) {
    return plugin.configManager().maintenanceState().isServerEnabled(serverName);
  }

  public boolean canBypass(Player player) {
    return player.hasPermission(plugin.config().maintenance().bypassPermission());
  }

  public void setNetworkEnabled(boolean enabled) {
    plugin.configManager().setNetworkMaintenance(enabled);
    if (enabled && plugin.config().maintenance().kickOnEnable()) {
      kickNetwork();
    }
  }

  public boolean setServerEnabled(String serverName, boolean enabled) {
    Optional<RegisteredServer> registered = findServer(serverName);
    if (registered.isEmpty()) {
      return false;
    }
    String name = registered.get().getServerInfo().getName();
    plugin.configManager().setServerMaintenance(name, enabled);
    if (enabled && plugin.config().maintenance().kickOnEnable()) {
      kickServer(registered.get());
    }
    return true;
  }

  public Component networkDeniedMessage() {
    return disconnectMessage(plugin.config().maintenance().messages().networkDenied(), "");
  }

  public Component serverDeniedMessage(String serverName) {
    return disconnectMessage(plugin.config().maintenance().messages().serverDenied(), serverName);
  }

  public Component statusMessage() {
    PluginConfig.MaintenanceState state = plugin.configManager().maintenanceState();
    String network = state.networkEnabled() ? "AN" : "AUS";
    String servers = state.servers().isEmpty()
        ? "-"
        : state.servers().stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", "));
    String raw = plugin.config().maintenance().messages().status()
        .replace("%network%", network)
        .replace("%servers%", servers);
    return MessageUtil.parsePrefixed(
        plugin.config().prefix(),
        raw,
        plugin.config().maintenance().format()
    );
  }

  public Optional<RegisteredServer> findServer(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    Optional<RegisteredServer> exact = plugin.server().getServer(name);
    if (exact.isPresent()) {
      return exact;
    }
    String needle = name.toLowerCase(Locale.ROOT);
    for (RegisteredServer server : plugin.server().getAllServers()) {
      if (server.getServerInfo().getName().toLowerCase(Locale.ROOT).equals(needle)) {
        return Optional.of(server);
      }
    }
    return Optional.empty();
  }

  public void kickNetwork() {
    Component reason = disconnectMessage(plugin.config().maintenance().messages().networkKick(), "");
    for (Player player : plugin.server().getAllPlayers()) {
      if (!canBypass(player)) {
        player.disconnect(reason);
      }
    }
  }

  public void kickServer(RegisteredServer server) {
    String name = server.getServerInfo().getName();
    Component reason = disconnectMessage(plugin.config().maintenance().messages().serverKick(), name);
    for (Player player : server.getPlayersConnected()) {
      if (!canBypass(player)) {
        player.disconnect(reason);
      }
    }
  }

  /** Called after proxy start / reload if persisted network maintenance is on. */
  public void enforcePersistedState() {
    PluginConfig.MaintenanceState state = plugin.configManager().maintenanceState();
    if (!plugin.config().maintenance().kickOnEnable()) {
      return;
    }
    if (state.networkEnabled()) {
      kickNetwork();
    }
    for (String serverName : state.servers()) {
      findServer(serverName).ifPresent(this::kickServer);
    }
  }

  private Component disconnectMessage(String template, String server) {
    MessageUtil.Placeholders placeholders =
        new MessageUtil.Placeholders(plugin.onlinePlayers(), plugin.config().maxPlayers(), server);
    return MessageUtil.parseDisconnect(
        plugin.config().disconnectPrefix(),
        placeholders.apply(template),
        plugin.config().maintenance().format()
    );
  }
}
