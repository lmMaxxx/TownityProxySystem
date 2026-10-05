package de.townity.proxysystem.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.PluginConfig;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.limit.PlayerLimitService;
import de.townity.proxysystem.util.MessageUtil;
import net.kyori.adventure.text.Component;

public final class LoginListener {

  private final TownityProxySystem plugin;
  private final PlayerLimitService limit;

  public LoginListener(TownityProxySystem plugin, PlayerLimitService limit) {
    this.plugin = plugin;
    this.limit = limit;
  }

  @Subscribe(priority = Short.MIN_VALUE)
  public void onLogin(LoginEvent event) {
    if (!event.getResult().isAllowed()) {
      return;
    }

    PluginConfig config = plugin.config();
    Player player = event.getPlayer();
    if (limit.tryReserve(player, config)) {
      return;
    }

    plugin.logger().info(
        "Join denied for {} – proxy full ({}/{})",
        player.getUsername(),
        limit.occupiedSlots(config),
        config.maxPlayers()
    );
    event.setResult(ResultedEvent.ComponentResult.denied(fullMessage(config)));
  }

  @Subscribe(priority = Short.MIN_VALUE)
  public void onPostLogin(PostLoginEvent event) {
    PluginConfig config = plugin.config();
    Player player = event.getPlayer();
    if (limit.tryReserve(player, config)) {
      return;
    }

    plugin.logger().info(
        "Kicked {} after login – proxy full ({}/{})",
        player.getUsername(),
        limit.occupiedSlots(config),
        config.maxPlayers()
    );
    player.disconnect(fullMessage(config));
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    limit.release(event.getPlayer().getUniqueId());
  }

  private Component fullMessage(PluginConfig config) {
    MessageUtil.Placeholders placeholders = new MessageUtil.Placeholders(
        Math.max(plugin.onlinePlayers(), limit.occupiedSlots(config)),
        config.maxPlayers()
    );
    return MessageUtil.parseDisconnect(
        TownityMessages.DISCONNECT_PREFIX,
        placeholders.apply(TownityMessages.FULL),
        TownityMessages.FORMAT
    );
  }
}
