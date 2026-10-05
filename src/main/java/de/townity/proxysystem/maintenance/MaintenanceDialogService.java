package de.townity.proxysystem.maintenance;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.util.MessageUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Native Minecraft dialogs (same client feature as Paper's Dialog API) sent from the proxy.
 */
public final class MaintenanceDialogService {

  public static final ResourceLocation NETWORK_ON = id("network_on");
  public static final ResourceLocation NETWORK_OFF = id("network_off");
  public static final ResourceLocation SERVER_ON = id("server_on");
  public static final ResourceLocation SERVER_OFF = id("server_off");
  public static final ResourceLocation OPEN_SERVERS = id("open_servers");
  public static final ResourceLocation OPEN_MAIN = id("open_main");

  private static final int BODY_WIDTH = 300;
  private static final int BUTTON_WIDTH = 200;
  private static final ClientVersion MIN_DIALOG_VERSION = ClientVersion.V_1_21_6;

  private final TownityProxySystem plugin;
  private final MaintenanceService maintenance;

  public MaintenanceDialogService(TownityProxySystem plugin, MaintenanceService maintenance) {
    this.plugin = plugin;
    this.maintenance = maintenance;
  }

  public boolean canUseDialog(Player player) {
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    return user != null && !user.getClientVersion().isOlderThan(MIN_DIALOG_VERSION);
  }

  public void open(Player player) {
    if (!canUseDialog(player)) {
      sendChatFallback(player);
      return;
    }
    show(player, buildMainDialog());
  }

  public void openServers(Player player) {
    if (!canUseDialog(player)) {
      sendChatFallback(player);
      return;
    }
    show(player, buildServersDialog());
  }

  public void refresh(Player player) {
    open(player);
  }

  private void show(Player player, Dialog dialog) {
    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerClearDialog());
    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerShowDialog(dialog));
  }

  private Dialog buildMainDialog() {
    boolean network = maintenance.isNetworkEnabled();
    List<String> maintained = maintainedServerNames();

    Component body = MessageUtil.parse("&7Netzwerk: &6" + (network ? "AN" : "AUS"))
        .append(Component.newline())
        .append(Component.newline())
        .append(MessageUtil.parse("&7Unterserver: "))
        .append(MessageUtil.parse(maintained.isEmpty()
            ? "&6keine"
            : "&6" + String.join("&7, &6", maintained)));

    List<ActionButton> actions = new ArrayList<>();
    if (network) {
      actions.add(button(
          "&7Wartung &6ausschalten",
          "&7Netzwerk-Wartung &6deaktivieren&7.",
          new DynamicCustomAction(NETWORK_OFF, null)
      ));
    } else {
      actions.add(button(
          "&7Wartung &6einschalten",
          "&7Gesamtes Netzwerk in &6Wartung &7versetzen.",
          new DynamicCustomAction(NETWORK_ON, null)
      ));
    }
    actions.add(button(
        "&7Unterserver",
        "&7Wartung für einzelne Server umschalten.",
        new DynamicCustomAction(OPEN_SERVERS, null)
    ));

    return multiAction(body, actions, closeButton());
  }

  private Dialog buildServersDialog() {
    List<RegisteredServer> servers = sortedServers();
    Component body = MessageUtil.parse("&7Unterserver-Wartung");
    if (servers.isEmpty()) {
      body = body.append(Component.newline()).append(MessageUtil.parse("&7Keine Server gefunden."));
    } else {
      for (RegisteredServer server : servers) {
        String name = server.getServerInfo().getName();
        body = body
            .append(Component.newline())
            .append(MessageUtil.parse("&6" + name + " &7· &6" + (maintenance.isServerEnabled(name) ? "AN" : "AUS")));
      }
    }

    List<ActionButton> actions = new ArrayList<>();
    for (RegisteredServer server : servers) {
      String name = server.getServerInfo().getName();
      boolean on = maintenance.isServerEnabled(name);
      actions.add(button(
          "&6" + name + " &7· &6" + (on ? "AN" : "AUS"),
          on
              ? "&7Klicken: Wartung für &6" + name + " &7ausschalten."
              : "&7Klicken: Wartung für &6" + name + " &7einschalten.",
          new DynamicCustomAction(on ? SERVER_OFF : SERVER_ON, payload(name))
      ));
    }
    actions.add(button(
        "&7Zurück",
        "&7Zurück zur Übersicht.",
        new DynamicCustomAction(OPEN_MAIN, null)
    ));

    return multiAction(body, actions, closeButton());
  }

  private Dialog multiAction(Component body, List<ActionButton> actions, ActionButton exit) {
    Component title = MessageUtil.parse(TownityMessages.MAINTENANCE_DIALOG_TITLE);
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

  private static ActionButton closeButton() {
    return button("&7Schließen", "&7Dialog schließen. &6ESC &7geht auch.", null);
  }

  private List<String> maintainedServerNames() {
    return sortedServers().stream()
        .map(server -> server.getServerInfo().getName())
        .filter(maintenance::isServerEnabled)
        .collect(Collectors.toList());
  }

  private List<RegisteredServer> sortedServers() {
    List<RegisteredServer> servers = new ArrayList<>(plugin.server().getAllServers());
    servers.sort(Comparator.comparing(s -> s.getServerInfo().getName(), String.CASE_INSENSITIVE_ORDER));
    return servers;
  }

  private void sendChatFallback(Player player) {
    player.sendMessage(prefixed("&7Minecraft &61.21.6+ &7wird für den Dialog benötigt."));
    player.sendMessage(commandLine("&7Netzwerk &6an", "/wartung an", "&7Gesamtes Netzwerk in &6Wartung &7versetzen."));
    player.sendMessage(commandLine("&7Netzwerk &6aus", "/wartung aus", "&7Netzwerk-Wartung &6deaktivieren&7."));

    for (RegisteredServer server : sortedServers()) {
      String name = server.getServerInfo().getName();
      String state = maintenance.isServerEnabled(name) ? "AN" : "AUS";
      player.sendMessage(
          prefixed("&7" + name + " &7(&6" + state + "&7) ")
              .append(MessageUtil.parse("&6an")
                  .clickEvent(ClickEvent.runCommand("/wartung server " + name + " an"))
                  .hoverEvent(HoverEvent.showText(MessageUtil.parse("&7Wartung für &6" + name + " &7aktivieren."))))
              .append(MessageUtil.parse(" &7· "))
              .append(MessageUtil.parse("&6aus")
                  .clickEvent(ClickEvent.runCommand("/wartung server " + name + " aus"))
                  .hoverEvent(HoverEvent.showText(MessageUtil.parse("&7Wartung für &6" + name + " &7deaktivieren."))))
      );
    }
  }

  private Component prefixed(String body) {
    return MessageUtil.parsePrefixed(TownityMessages.PREFIX, body, TownityMessages.FORMAT);
  }

  private Component commandLine(String label, String command, String tooltip) {
    return prefixed(label)
        .clickEvent(ClickEvent.runCommand(command))
        .hoverEvent(HoverEvent.showText(MessageUtil.parse(tooltip)));
  }

  private static ActionButton button(String label, String tooltip, Action action) {
    return new ActionButton(
        new CommonButtonData(MessageUtil.parse(label), MessageUtil.parse(tooltip), BUTTON_WIDTH),
        action
    );
  }

  private static NBTCompound payload(String server) {
    NBTCompound compound = new NBTCompound();
    compound.setTag("server", new NBTString(server));
    return compound;
  }

  private static ResourceLocation id(String path) {
    return new ResourceLocation("townity", "maintenance/" + path);
  }
}
