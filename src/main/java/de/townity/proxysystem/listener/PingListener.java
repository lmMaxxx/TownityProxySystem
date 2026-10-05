package de.townity.proxysystem.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.PluginConfig;
import de.townity.proxysystem.util.MessageUtil;
import de.townity.proxysystem.util.MotdService;
import net.kyori.adventure.text.Component;

public final class PingListener {

  private final TownityProxySystem plugin;

  public PingListener(TownityProxySystem plugin) {
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

    Component motd = resolveMotd(config);
    if (motd != null) {
      builder.description(motd);
      changed = true;
    }

    if (changed) {
      event.setPing(builder.build());
    }
  }

  private Component resolveMotd(PluginConfig config) {
    if (plugin.maintenance() != null
        && plugin.maintenance().isNetworkEnabled()
        && config.maintenance().overrideMotd()) {
      MessageUtil.Placeholders placeholders =
          new MessageUtil.Placeholders(plugin.onlinePlayers(), config.maxPlayers());
      return MessageUtil.joinLines(
          config.maintenance().motdLines(),
          config.maintenance().format(),
          placeholders
      );
    }
    if (config.motd().enabled()) {
      return MotdService.buildMotd(plugin);
    }
    return null;
  }
}
