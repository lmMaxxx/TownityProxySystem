package de.townity.proxysystem.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import de.townity.proxysystem.rangshop.RangShopService;

public final class PlaytimeListener {

  private final RangShopService rangShop;

  public PlaytimeListener(RangShopService rangShop) {
    this.rangShop = rangShop;
  }

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    rangShop.onJoin(event.getPlayer());
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    rangShop.onQuit(event.getPlayer());
  }
}
