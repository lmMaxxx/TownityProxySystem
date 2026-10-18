package de.townity.proxysystem.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.rangshop.RangShopDialogService;
import de.townity.proxysystem.rangshop.RangShopService;
import de.townity.proxysystem.rangshop.ShopRank;
import de.townity.proxysystem.util.Messages;
import java.util.Optional;
import java.util.UUID;

public final class RangShopDialogListener extends PacketListenerAbstract {

  private final TownityProxySystem plugin;
  private final RangShopService service;
  private final RangShopDialogService dialogs;

  public RangShopDialogListener(
      TownityProxySystem plugin,
      RangShopService service,
      RangShopDialogService dialogs
  ) {
    super(PacketListenerPriority.HIGH);
    this.plugin = plugin;
    this.service = service;
    this.dialogs = dialogs;
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (event.getPacketType() != PacketType.Play.Client.CUSTOM_CLICK_ACTION) {
      return;
    }

    WrapperPlayClientCustomClickAction wrapper = new WrapperPlayClientCustomClickAction(event);
    ResourceLocation id = wrapper.getId();
    if (id == null || !"townity".equals(id.getNamespace()) || !id.getKey().startsWith("rangshop/")) {
      return;
    }

    event.setCancelled(true);

    UUID uuid = event.getUser() == null ? null : event.getUser().getUUID();
    if (uuid == null) {
      return;
    }
    plugin.server().getPlayer(uuid).ifPresent(player ->
        plugin.server().getScheduler().buildTask(plugin, () -> handle(player, id, wrapper.getPayload())).schedule()
    );
  }

  private void handle(Player player, ResourceLocation id, NBT payload) {
    String key = id.getKey();

    if (key.endsWith("open_main")) {
      dialogs.refreshMain(player);
      return;
    }

    Optional<ShopRank> rank = ShopRank.byKey(readRank(payload));
    if (rank.isEmpty()) {
      return;
    }

    if (key.endsWith("open_rank")) {
      dialogs.openRank(player, rank.get());
      return;
    }

    if (key.endsWith("buy_rank")) {
      service.hasRankAboveShop(player).thenAccept(above ->
          service.highestShopRank(player).thenAccept(owned -> {
            if (RangShopDialogService.isDisabled(above, owned)) {
              plugin.server().getScheduler().buildTask(plugin, () -> {
                Messages.sendError(player, TownityMessages.RANGSHOP_DISABLED_ERROR);
                dialogs.refreshMain(player);
              }).schedule();
              return;
            }
            service.purchase(player, rank.get()).thenAccept(result ->
                plugin.server().getScheduler().buildTask(plugin, () -> {
                  switch (result) {
                    case SUCCESS -> {
                      Messages.sendSuccess(
                          player,
                          TownityMessages.RANGSHOP_BOUGHT.replace("%rank%", rank.get().displayName())
                      );
                      dialogs.refreshMain(player);
                    }
                    case LUCKPERMS_MISSING -> Messages.sendError(player, TownityMessages.RANGSHOP_LUCKPERMS_MISSING);
                    case HIGHER_RANK -> Messages.sendError(player, TownityMessages.RANGSHOP_ABOVE_ERROR);
                    case ALREADY_OWNED -> Messages.sendError(player, TownityMessages.RANGSHOP_ALREADY);
                    case NEED_PREVIOUS -> {
                      String prev = rank.get().previous().map(ShopRank::displayName).orElse("?");
                      Messages.sendError(player, TownityMessages.RANGSHOP_NEED_PREVIOUS.replace("%rank%", prev));
                    }
                    case NOT_ENOUGH_POINTS -> Messages.sendError(
                        player,
                        TownityMessages.RANGSHOP_NOT_ENOUGH
                            .replace("%need%", String.valueOf(rank.get().cost()))
                            .replace("%points%", String.valueOf(service.points(player)))
                    );
                    case FAILED -> Messages.sendError(player, TownityMessages.RANGSHOP_FAILED);
                  }
                  if (result != RangShopService.PurchaseResult.SUCCESS) {
                    dialogs.openRank(player, rank.get());
                  }
                }).schedule()
            );
          })
      );
    }
  }

  private static String readRank(NBT payload) {
    if (payload instanceof NBTCompound compound) {
      return compound.getStringTagValueOrNull("rank");
    }
    return null;
  }
}
