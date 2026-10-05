package de.townity.proxysystem.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.configuration.PlayerConfigurationEvent;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.rules.RulesDialogService;
import de.townity.proxysystem.rules.RulesService;
import de.townity.proxysystem.util.MessageUtil;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pauses the Minecraft configuration phase until the player accepts the rules.
 */
public final class RulesJoinListener {

  private static final int MAX_SEND_ATTEMPTS = 50;
  private static final Duration RETRY_DELAY = Duration.ofMillis(100);

  private final TownityProxySystem plugin;
  private final RulesService rules;
  private final RulesDialogService dialogs;
  private final Map<UUID, Continuation> pending = new ConcurrentHashMap<>();

  public RulesJoinListener(TownityProxySystem plugin, RulesService rules, RulesDialogService dialogs) {
    this.plugin = plugin;
    this.rules = rules;
    this.dialogs = dialogs;
  }

  @Subscribe
  public EventTask onConfiguration(PlayerConfigurationEvent event) {
    Player player = event.player();
    if (!rules.needsAccept(player)) {
      plugin.logger().info(
          "Rules skip for {} (already accepted v{})",
          player.getUsername(),
          TownityMessages.RULES_VERSION
      );
      return null;
    }

    if (dialogs.clientDefinitelyTooOld(player)) {
      plugin.logger().info("Rules kick for {} – client too old", player.getUsername());
      player.disconnect(MessageUtil.parseDisconnect(
          TownityMessages.DISCONNECT_PREFIX,
          TownityMessages.RULES_NEED_UPDATE,
          TownityMessages.FORMAT
      ));
      return null;
    }

    plugin.logger().info("Rules config screen pending for {}", player.getUsername());
    return EventTask.withContinuation(continuation -> {
      pending.put(player.getUniqueId(), continuation);
      scheduleSend(player, 0);
    });
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    Continuation continuation = pending.remove(event.getPlayer().getUniqueId());
    if (continuation != null) {
      try {
        continuation.resume();
      } catch (Exception ignored) {
        // already completed
      }
    }
  }

  public boolean completeAccept(Player player) {
    Continuation continuation = pending.remove(player.getUniqueId());
    dialogs.clearConfiguration(player);
    if (continuation != null) {
      continuation.resume();
      return true;
    }
    return false;
  }

  public boolean completeDecline(Player player) {
    Continuation continuation = pending.remove(player.getUniqueId());
    if (continuation != null) {
      try {
        continuation.resume();
      } catch (Exception ignored) {
        // ignore
      }
    }
    return true;
  }

  public boolean isPending(UUID uuid) {
    return pending.containsKey(uuid);
  }

  private void scheduleSend(Player player, int attempt) {
    plugin.server().getScheduler()
        .buildTask(plugin, () -> {
          if (!player.isActive() || !pending.containsKey(player.getUniqueId())) {
            return;
          }
          if (!rules.needsAccept(player)) {
            completeAccept(player);
            return;
          }
          if (dialogs.showConfiguration(player)) {
            plugin.logger().info("Rules configuration screen shown for {}", player.getUsername());
            return;
          }
          if (attempt + 1 >= MAX_SEND_ATTEMPTS) {
            plugin.logger().warn(
                "Failed to show rules config screen for {} after {} attempts",
                player.getUsername(),
                MAX_SEND_ATTEMPTS
            );
            pending.remove(player.getUniqueId());
            player.disconnect(MessageUtil.parseDisconnect(
                TownityMessages.DISCONNECT_PREFIX,
                TownityMessages.RULES_NEED_UPDATE,
                TownityMessages.FORMAT
            ));
            return;
          }
          scheduleSend(player, attempt + 1);
        })
        .delay(attempt == 0 ? Duration.ofMillis(50) : RETRY_DELAY)
        .schedule();
  }
}
