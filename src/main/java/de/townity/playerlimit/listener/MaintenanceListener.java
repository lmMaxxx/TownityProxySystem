package de.townity.playerlimit.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.townity.playerlimit.TownityPlayerLimit;
import de.townity.playerlimit.maintenance.MaintenanceService;

public final class MaintenanceListener {

  private final TownityPlayerLimit plugin;
  private final MaintenanceService maintenance;

  public MaintenanceListener(TownityPlayerLimit plugin, MaintenanceService maintenance) {
    this.plugin = plugin;
    this.maintenance = maintenance;
  }

  @Subscribe(order = PostOrder.EARLY)
  public void onLogin(LoginEvent event) {
    if (!plugin.config().maintenance().blockJoin()) {
      return;
    }
    if (!maintenance.isNetworkEnabled()) {
      return;
    }
    Player player = event.getPlayer();
    if (maintenance.canBypass(player)) {
      return;
    }
    event.setResult(ResultedEvent.ComponentResult.denied(maintenance.networkDeniedMessage()));
  }

  @Subscribe(order = PostOrder.EARLY)
  public void onServerPreConnect(ServerPreConnectEvent event) {
    if (!plugin.config().maintenance().blockJoin()) {
      return;
    }

    Player player = event.getPlayer();
    if (maintenance.canBypass(player)) {
      return;
    }

    RegisteredServer target = event.getResult().getServer().orElse(event.getOriginalServer());
    String targetName = target.getServerInfo().getName();

    if (maintenance.isNetworkEnabled()) {
      denyConnect(event, player, maintenance.networkDeniedMessage());
      return;
    }

    if (maintenance.isServerEnabled(targetName)) {
      denyConnect(event, player, maintenance.serverDeniedMessage(targetName));
    }
  }

  private void denyConnect(ServerPreConnectEvent event, Player player, net.kyori.adventure.text.Component reason) {
    event.setResult(ServerPreConnectEvent.ServerResult.denied());
    // Always disconnect so the client shows the 2-line fullscreen reason
    player.disconnect(reason);
  }
}
