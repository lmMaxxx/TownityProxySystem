package de.townity.proxysystem.util;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/** Feedback-Sounds wie TownityCitybuildSystem. */
public final class TownitySounds {

  private static final Sound SUCCESS = Sound.sound(
      Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.MASTER, 1f, 1f);
  private static final Sound ERROR = Sound.sound(
      Key.key("minecraft:entity.villager.no"), Sound.Source.MASTER, 1f, 1f);
  private static final Sound MENU = Sound.sound(
      Key.key("minecraft:ui.button.click"), Sound.Source.MASTER, 1f, 1f);

  private TownitySounds() {}

  public static void success(Player player) {
    player.playSound(SUCCESS);
  }

  public static void error(Player player) {
    player.playSound(ERROR);
  }

  public static void menu(Player player) {
    player.playSound(MENU);
  }
}
