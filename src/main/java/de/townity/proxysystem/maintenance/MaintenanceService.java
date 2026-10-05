package de.townity.proxysystem.maintenance;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.PluginConfig;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.util.MessageUtil;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;

public final class MaintenanceService {

  private final TownityProxySystem plugin;

  public MaintenanceService(TownityProxySystem plugin) {
    this.plugin = plugin;
  }

  public boolean isNetworkEnabled() {
    return plugin.configManager().maintenanceState().networkEnabled();
  }

  public boolean isServerEnabled(String serverName) {
    return plugin.configManager().maintenanceState().isServerEnabled(serverName);
  }

  public boolean canBypass(Player player) {
    return player.hasPermission(TownityMessages.MAINTENANCE_BYPASS);
  }

  public void setNetworkEnabled(boolean enabled) {
    plugin.configManager().setNetworkMaintenance(enabled);
    if (enabled && TownityMessages.MAINTENANCE_KICK_ON_ENABLE) {
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
    if (enabled && TownityMessages.MAINTENANCE_KICK_ON_ENABLE) {
      kickServer(registered.get());
    }
    return true;
  }

  public Component networkDeniedMessage() {
    return disconnectMessage(TownityMessages.MAINTENANCE_NETWORK_DENIED, "");
  }

  public Component serverDeniedMessage(String serverName) {
    return disconnectMessage(TownityMessages.MAINTENANCE_SERVER_DENIED, serverName);
  }

  public Component statusMessage() {
    PluginConfig.MaintenanceState state = plugin.configManager().maintenanceState();
    String network = state.networkEnabled() ? "AN" : "AUS";
    String servers = state.servers().isEmpty()
        ? "-"
        : state.servers().stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", "));
    String raw = TownityMessages.MAINTENANCE_STATUS
        .replace("%network%", network)
        .replace("%servers%", servers);
    return MessageUtil.parsePrefixed(TownityMessages.PREFIX, raw, TownityMessages.FORMAT);
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
    Component reason = disconnectMessage(TownityMessages.MAINTENANCE_NETWORK_KICK, "");
    for (Player player : plugin.server().getAllPlayers()) {
      if (!canBypass(player)) {
        player.disconnect(reason);
      }
    }
  }

  public void kickServer(RegisteredServer server) {
    String name = server.getServerInfo().getName();
    Component reason = disconnectMessage(TownityMessages.MAINTENANCE_SERVER_KICK, name);
    for (Player player : server.getPlayersConnected()) {
      if (!canBypass(player)) {
        player.disconnect(reason);
      }
    }
  }

  /** Called after proxy start / reload if persisted network maintenance is on. */
  public void enforcePersistedState() {
    PluginConfig.MaintenanceState state = plugin.configManager().maintenanceState();
    if (!TownityMessages.MAINTENANCE_KICK_ON_ENABLE) {
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
        TownityMessages.DISCONNECT_PREFIX,
        placeholders.apply(template),
        TownityMessages.FORMAT
    );
  }
}
