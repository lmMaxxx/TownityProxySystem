package de.townity.playerlimit.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.townity.playerlimit.TownityPlayerLimit;
import de.townity.playerlimit.config.PluginConfig;
import de.townity.playerlimit.util.MessageUtil;
import net.kyori.adventure.text.Component;

public final class LoginListener {

  private final TownityPlayerLimit plugin;

  public LoginListener(TownityPlayerLimit plugin) {
    this.plugin = plugin;
  }

  @Subscribe(order = PostOrder.LAST)
  public void onLogin(LoginEvent event) {
    PluginConfig config = plugin.config();
    Player player = event.getPlayer();

    if (player.hasPermission(config.bypassPermission())) {
      return;
    }

    int max = config.maxPlayers();
    int occupied = countOccupiedSlots(config);
    // LoginEvent fires before the joining player is in getAllPlayers()
    if (occupied < max) {
      return;
    }

    MessageUtil.Placeholders placeholders =
        new MessageUtil.Placeholders(plugin.onlinePlayers(), max);
    Component reason = MessageUtil.parse(placeholders.apply(config.messages().full()), "auto");
    event.setResult(ResultedEvent.ComponentResult.denied(reason));
  }

  private int countOccupiedSlots(PluginConfig config) {
    if (config.bypassCountsAsSlot()) {
      return plugin.onlinePlayers();
    }
    int occupied = 0;
    for (Player online : plugin.server().getAllPlayers()) {
      if (!online.hasPermission(config.bypassPermission())) {
        occupied++;
      }
    }
    return occupied;
  }
}
