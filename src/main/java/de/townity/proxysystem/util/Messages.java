package de.townity.proxysystem.util;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.config.TownityMessages;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

/**
 * Townity Nachrichten-Konvention (wie TownityCitybuildSystem / DESIGNSPRACHE):
 *
 * <pre>
 * Prefix:  &amp;6&amp;lTOWNITY.DE &amp;7⇒
 * Wichtig: &amp;6
 * Rest:    &amp;7
 * Fehler:  &amp;c ohne Prefix (+ Error-Sound)
 * Erfolg:  Prefix-Nachricht (+ Orb-Pickup)
 * </pre>
 */
public final class Messages {

  public static final String PREFIX_RAW = TownityMessages.PREFIX;

  private Messages() {}

  /** Prefix + Body. */
  public static Component of(String bodyWithColors) {
    return MessageUtil.parse(PREFIX_RAW + " " + bodyWithColors, TownityMessages.FORMAT);
  }

  public static Component error(String errorBody) {
    String raw = errorBody == null ? "" : errorBody;
    if (!raw.startsWith("&c") && !raw.startsWith("§c")) {
      raw = "&c" + raw;
    }
    return MessageUtil.parse(raw, TownityMessages.FORMAT);
  }

  public static void send(CommandSource source, String bodyWithColors) {
    source.sendMessage(of(bodyWithColors));
  }

  public static void sendSuccess(CommandSource source, String bodyWithColors) {
    source.sendMessage(of(bodyWithColors));
    if (source instanceof Player player) {
      TownitySounds.success(player);
    }
  }

  public static void sendError(CommandSource source, String errorBody) {
    source.sendMessage(error(errorBody));
    if (source instanceof Player player) {
      TownitySounds.error(player);
    }
  }
}
