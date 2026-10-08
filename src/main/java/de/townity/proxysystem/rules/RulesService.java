package de.townity.proxysystem.rules;

import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.util.MessageUtil;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public final class RulesService {

  private final RulesAcceptanceStore store;

  public RulesService(TownityProxySystem plugin, RulesAcceptanceStore store) {
    this.store = store;
  }

  public RulesAcceptanceStore store() {
    return store;
  }

  public boolean isEnabled() {
    return TownityMessages.RULES_ENABLED;
  }

  /** No bypass – everyone must accept. */
  public boolean needsAccept(Player player) {
    if (!isEnabled()) {
      return false;
    }
    return store.acceptedVersion(player.getUniqueId()) < TownityMessages.RULES_VERSION;
  }

  public void accept(Player player) {
    store.setAccepted(player.getUniqueId(), TownityMessages.RULES_VERSION);
  }

  public void decline(Player player) {
    player.disconnect(declineDisconnectMessage());
  }

  /** @return {@code true} if the player had an acceptance entry */
  public boolean reset(UUID uuid) {
    return store.reset(uuid);
  }

  /** @return number of cleared acceptances */
  public int resetAll() {
    return store.resetAll();
  }

  public Component declineDisconnectMessage() {
    return MessageUtil.parseDisconnect(
        TownityMessages.DISCONNECT_PREFIX,
        TownityMessages.RULES_DECLINE_KICK,
        TownityMessages.FORMAT
    );
  }
}
