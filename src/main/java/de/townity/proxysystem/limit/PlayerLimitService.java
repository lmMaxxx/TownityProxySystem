package de.townity.proxysystem.limit;

import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.PluginConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts occupied slots including in-flight logins so concurrent joins cannot exceed the cap.
 * Every joining player consumes a slot – there is no bypass.
 */
public final class PlayerLimitService {

  private final TownityProxySystem plugin;
  private final Set<UUID> reservedSlots = ConcurrentHashMap.newKeySet();
  private final Object lock = new Object();

  public PlayerLimitService(TownityProxySystem plugin) {
    this.plugin = plugin;
  }

  /**
   * Reserves a slot for the player if one is free.
   *
   * @return {@code true} if the player may stay / continue login
   */
  public boolean tryReserve(Player player, PluginConfig config) {
    UUID id = player.getUniqueId();
    synchronized (lock) {
      if (reservedSlots.contains(id)) {
        return true;
      }
      if (countOccupiedExcept(id) >= config.maxPlayers()) {
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
      return countOccupiedExcept(null);
    }
  }

  public void syncOnline(PluginConfig config) {
    synchronized (lock) {
      reservedSlots.clear();
      for (Player online : plugin.server().getAllPlayers()) {
        reservedSlots.add(online.getUniqueId());
      }
    }
  }

  private int countOccupiedExcept(UUID exclude) {
    Set<UUID> counted = new HashSet<>(reservedSlots);
    for (Player online : plugin.server().getAllPlayers()) {
      counted.add(online.getUniqueId());
    }
    if (exclude != null) {
      counted.remove(exclude);
    }
    return counted.size();
  }
}
