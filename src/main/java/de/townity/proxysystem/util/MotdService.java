package de.townity.proxysystem.util;

import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class MotdService {

  private MotdService() {}

  public static Component buildMotd(TownityProxySystem plugin) {
    MessageUtil.Placeholders placeholders =
        new MessageUtil.Placeholders(plugin.onlinePlayers(), plugin.config().maxPlayers());
    return MessageUtil.joinLines(TownityMessages.MOTD_LINES, TownityMessages.FORMAT, placeholders);
  }
}
