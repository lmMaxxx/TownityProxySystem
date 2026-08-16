package de.townity.playerlimit.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.townity.playerlimit.TownityPlayerLimit;
import de.townity.playerlimit.config.PluginConfig;
import de.townity.playerlimit.util.MotdService;

public final class PingListener {

  private final TownityPlayerLimit plugin;

  public PingListener(TownityPlayerLimit plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onProxyPing(ProxyPingEvent event) {
    PluginConfig config = plugin.config();
    ServerPing.Builder builder = event.getPing().asBuilder();
    boolean changed = false;

    if (config.updatePingMaxPlayers()) {
      builder.maximumPlayers(config.maxPlayers());
      changed = true;
    }

    if (config.motd().enabled()) {
      builder.description(MotdService.buildMotd(plugin));
      changed = true;
    }

    if (changed) {
      event.setPing(builder.build());
    }
  }
}
