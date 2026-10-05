package de.townity.playerlimit.limit;

import com.velocitypowered.api.proxy.Player;
import de.townity.playerlimit.TownityPlayerLimit;
import de.townity.playerlimit.config.PluginConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts occupied slots including in-flight logins so concurrent joins cannot bypass the cap.
 */
public final class PlayerLimitService {

  private final TownityPlayerLimit plugin;
  private final Set<UUID> reservedSlots = ConcurrentHashMap.newKeySet();
  private final Object lock = new Object();

  public PlayerLimitService(TownityPlayerLimit plugin) {
    this.plugin = plugin;
  }

  public boolean takesSlot(Player player, PluginConfig config) {
    return config.bypassCountsAsSlot() || !player.hasPermission(config.bypassPermission());
  }

  /**
   * Reserves a slot for the player if one is free.
   *
   * @return {@code true} if the player may stay / continue login
   */
  public boolean tryReserve(Player player, PluginConfig config) {
    UUID id = player.getUniqueId();
    if (!takesSlot(player, config)) {
      reservedSlots.remove(id);
      return true;
    }
    synchronized (lock) {
      if (reservedSlots.contains(id)) {
        return true;
      }
      if (countOccupiedExcept(id, config) >= config.maxPlayers()) {
        return false;
      }
      reservedSlots.add(id);
      return true;
    }
  }

  public void release(UUID uuid) {
    if (uuid != null) {
      reservedSlots.remove(uuid);
    }
  }

  public int occupiedSlots(PluginConfig config) {
    synchronized (lock) {
      return countOccupiedExcept(null, config);
    }
  }

  public void syncOnline(PluginConfig config) {
    synchronized (lock) {
      reservedSlots.clear();
      for (Player online : plugin.server().getAllPlayers()) {
        if (takesSlot(online, config)) {
          reservedSlots.add(online.getUniqueId());
        }
      }
    }
  }

  private int countOccupiedExcept(UUID exclude, PluginConfig config) {
    Set<UUID> counted = new HashSet<>(reservedSlots);
    for (Player online : plugin.server().getAllPlayers()) {
      UUID id = online.getUniqueId();
      if (takesSlot(online, config)) {
        counted.add(id);
      } else {
        counted.remove(id);
      }
    }
    if (exclude != null) {
      counted.remove(exclude);
    }
    return counted.size();
  }
}
