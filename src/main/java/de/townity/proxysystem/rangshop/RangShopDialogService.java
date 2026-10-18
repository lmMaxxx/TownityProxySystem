package de.townity.proxysystem.rangshop;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.NoticeDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.Action;
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.util.MessageUtil;
import de.townity.proxysystem.util.Messages;
import de.townity.proxysystem.util.TownitySounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/**
 * Native Minecraft-Dialog für den Rangshop (PacketEvents, wie Wartung/Regelwerk).
 */
public final class RangShopDialogService {

  public static final ResourceLocation OPEN_MAIN = id("open_main");
  public static final ResourceLocation OPEN_RANK = id("open_rank");
  public static final ResourceLocation BUY_RANK = id("buy_rank");

  private static final int BODY_WIDTH = 300;
  private static final int BUTTON_WIDTH = 200;
  private static final ClientVersion MIN_DIALOG_VERSION = ClientVersion.V_1_21_6;

  private final TownityProxySystem plugin;
  private final RangShopService service;

  public RangShopDialogService(TownityProxySystem plugin, RangShopService service) {
    this.plugin = plugin;
    this.service = service;
  }

  public boolean canUseDialog(Player player) {
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    return user != null && !user.getClientVersion().isOlderThan(MIN_DIALOG_VERSION);
  }

  public void open(Player player) {
    if (!service.luckPerms().isAvailable()) {
      Messages.sendError(player, TownityMessages.RANGSHOP_LUCKPERMS_MISSING);
      return;
    }
    if (!canUseDialog(player)) {
      Messages.sendError(player, TownityMessages.RANGSHOP_NEED_UPDATE);
      return;
    }
    TownitySounds.dialogOpen(player);
    refreshMain(player);
  }

  public void refreshMain(Player player) {
    service.hasRankAboveShop(player).thenAccept(above ->
        service.highestShopRank(player).thenAccept(owned ->
            plugin.server().getScheduler().buildTask(plugin, () ->
                show(player, buildMainDialog(player, above, owned))
            ).schedule()
        )
    );
  }

  public void openRank(Player player, ShopRank rank) {
    service.hasRankAboveShop(player).thenAccept(above ->
        service.highestShopRank(player).thenAccept(owned -> {
          if (isDisabled(above, owned)) {
            plugin.server().getScheduler().buildTask(plugin, () ->
                show(player, buildDisabledDialog(above, owned))
            ).schedule();
            return;
          }
          plugin.server().getScheduler().buildTask(plugin, () ->
              show(player, buildRankDialog(player, rank, above, owned))
          ).schedule();
        })
    );
  }

  private Dialog buildMainDialog(Player player, boolean aboveShop, Optional<ShopRank> owned) {
    if (isDisabled(aboveShop, owned)) {
      return buildDisabledDialog(aboveShop, owned);
    }

    int points = service.points(player);
    int max = service.maxPoints();

    Component body = MessageUtil.parse("&7Mit &6Netzwerk-Spielzeit &7sammelst du Punkte")
        .append(Component.newline())
        .append(MessageUtil.parse("&7und schaltest Ränge nacheinander frei."))
        .append(Component.newline())
        .append(Component.newline())
        .append(MessageUtil.parse("&7Punkte: &6" + points + "&7 / &6" + max));

    if (owned.isPresent()) {
      body = body
          .append(Component.newline())
          .append(MessageUtil.parse("&7Aktueller Rang: &6" + owned.get().displayName()));
    } else {
      body = body
          .append(Component.newline())
          .append(MessageUtil.parse("&7Aktueller Rang: &6keiner"));
    }

    List<ActionButton> actions = new ArrayList<>();
    for (ShopRank rank : ShopRank.values()) {
      String status = statusLabel(rank, false, owned);
      actions.add(button(
          "&6" + rank.displayName() + " &7· &6" + rank.cost() + " Pkt",
          status + " &7Rechte & Status anzeigen.",
          new DynamicCustomAction(OPEN_RANK, payloadRank(rank))
      ));
    }

    return multiAction(TownityMessages.RANGSHOP_DIALOG_TITLE, body, actions, closeButton());
  }

  private Dialog buildDisabledDialog(boolean aboveShop, Optional<ShopRank> owned) {
    String bodyRaw = aboveShop
        ? TownityMessages.RANGSHOP_ABOVE_BODY
        : TownityMessages.RANGSHOP_COMPLETE_BODY;
    if (!aboveShop && owned.isPresent()) {
      bodyRaw = bodyRaw + "\n\n&7Aktueller Rang: &6" + owned.get().displayName();
    }

    Component body = parseMultiline(bodyRaw);
    Component title = MessageUtil.parse(TownityMessages.RANGSHOP_DIALOG_TITLE);
    Component external = MessageUtil.parse(TownityMessages.DISCONNECT_PREFIX);
    CommonDialogData data = new CommonDialogData(
        title,
        external,
        true,
        false,
        DialogAction.CLOSE,
        List.of(new PlainMessageDialogBody(new PlainMessage(body, BODY_WIDTH))),
        List.of()
    );
    return new NoticeDialog(data, closeButton());
  }

  private Dialog buildRankDialog(
      Player player,
      ShopRank rank,
      boolean aboveShop,
      Optional<ShopRank> owned
  ) {
    int points = service.points(player);
    Component body = MessageUtil.parse("&6" + rank.displayName())
        .append(Component.newline())
        .append(MessageUtil.parse("&7Kosten: &6" + rank.cost() + " &7Punkte"))
        .append(Component.newline())
        .append(MessageUtil.parse("&7Dein Guthaben: &6" + points))
        .append(Component.newline())
        .append(Component.newline())
        .append(MessageUtil.parse("&7Rechte:"));

    for (String right : rank.rights()) {
      body = body
          .append(Component.newline())
          .append(MessageUtil.parse("&7· &6" + right));
    }

    body = body
        .append(Component.newline())
        .append(Component.newline())
        .append(MessageUtil.parse(statusLabel(rank, aboveShop, owned)));

    List<ActionButton> actions = new ArrayList<>();
    boolean canBuy = !aboveShop
        && owned.map(o -> o.order() < rank.order()).orElse(true)
        && rank.previous().map(prev -> owned.isPresent() && owned.get().order() >= prev.order()).orElse(true)
        && points >= rank.cost();

    if (canBuy) {
      actions.add(button(
          "&6Freischalten",
          "&7Rang &6" + rank.displayName() + " &7für &6" + rank.cost() + " &7Punkte freischalten.",
          new DynamicCustomAction(BUY_RANK, payloadRank(rank))
      ));
    }

    actions.add(button(
        "&7Zurück",
        "&7Zurück zur Übersicht.",
        new DynamicCustomAction(OPEN_MAIN, null)
    ));

    return multiAction(
        "&8» &6&l" + rank.displayName(),
        body,
        actions,
        closeButton()
    );
  }

  public static boolean isDisabled(boolean aboveShop, Optional<ShopRank> owned) {
    return aboveShop || owned.map(rank -> rank == ShopRank.GRAF).orElse(false);
  }

  private static String statusLabel(ShopRank rank, boolean aboveShop, Optional<ShopRank> owned) {
    if (aboveShop) {
      return TownityMessages.RANGSHOP_ABOVE_SHORT;
    }
    if (owned.isPresent() && owned.get().order() > rank.order()) {
      return "&7Du hast bereits einen &6höheren Rang&7.";
    }
    if (owned.isPresent() && owned.get() == rank) {
      return "&7Dieser Rang ist bereits &6freigeschaltet&7.";
    }
    Optional<ShopRank> previous = rank.previous();
    if (previous.isPresent()) {
      int ownedOrder = owned.map(ShopRank::order).orElse(0);
      if (ownedOrder < previous.get().order()) {
        return "&7Zuerst &6" + previous.get().displayName() + " &7freischalten.";
      }
    }
    return "&7Bereit zur Freischaltung.";
  }

  private void show(Player player, Dialog dialog) {
    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerClearDialog());
    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerShowDialog(dialog));
  }

  private Dialog multiAction(String titleRaw, Component body, List<ActionButton> actions, ActionButton exit) {
    Component title = MessageUtil.parse(titleRaw);
    Component external = MessageUtil.parse(TownityMessages.DISCONNECT_PREFIX);
    CommonDialogData data = new CommonDialogData(
        title,
        external,
        true,
        false,
        DialogAction.CLOSE,
        List.of(new PlainMessageDialogBody(new PlainMessage(body, BODY_WIDTH))),
        List.of()
    );
    return new MultiActionDialog(data, actions, exit, 1);
  }

  private static Component parseMultiline(String raw) {
    String[] lines = raw.split("\n", -1);
    Component result = Component.empty();
    for (int i = 0; i < lines.length; i++) {
      if (i > 0) {
        result = result.append(Component.newline());
      }
      String line = lines[i];
      if (line.isEmpty()) {
        continue;
      }
      result = result.append(MessageUtil.parse(line));
    }
    return result;
  }

  private static ActionButton closeButton() {
    return button("&7Schließen", "&7Dialog schließen. &6ESC &7geht auch.", null);
  }

  private static ActionButton button(String label, String tooltip, Action action) {
    return new ActionButton(
        new CommonButtonData(MessageUtil.parse(label), MessageUtil.parse(tooltip), BUTTON_WIDTH),
        action
    );
  }

  private static NBTCompound payloadRank(ShopRank rank) {
    NBTCompound compound = new NBTCompound();
    compound.setTag("rank", new NBTString(rank.group()));
    return compound;
  }

  private static ResourceLocation id(String path) {
    return new ResourceLocation("townity", "rangshop/" + path);
  }
}
