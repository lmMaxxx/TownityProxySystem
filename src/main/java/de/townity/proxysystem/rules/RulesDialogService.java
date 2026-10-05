package de.townity.proxysystem.rules;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.ConnectionState;
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
import com.github.retrooper.packetevents.protocol.dialog.input.BooleanInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerClearDialog;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerShowDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.util.MessageUtil;
import de.townity.proxysystem.util.Messages;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Rules acceptance via Minecraft configuration-phase dialog (checkbox required).
 */
public final class RulesDialogService {

  public static final ResourceLocation ACCEPT = id("accept");
  public static final ResourceLocation DECLINE = id("decline");
  public static final String CHECKBOX_KEY = "accepted";

  private static final int MIN_DIALOG_PROTOCOL = 771;
  private static final int BODY_WIDTH = 300;
  private static final int BUTTON_WIDTH = 200;
  private static final ClientVersion MIN_DIALOG_VERSION = ClientVersion.V_1_21_6;

  private final RulesService rules;

  public RulesDialogService(de.townity.proxysystem.TownityProxySystem plugin, RulesService rules) {
    this.rules = rules;
  }

  public boolean clientDefinitelyTooOld(Player player) {
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    if (user != null) {
      ClientVersion version = user.getClientVersion();
      return version != null
          && version != ClientVersion.UNKNOWN
          && version.isOlderThan(MIN_DIALOG_VERSION);
    }
    try {
      int protocol = player.getProtocolVersion().getProtocol();
      return protocol > 0 && protocol < MIN_DIALOG_PROTOCOL;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Shows rules in the configuration phase.
   *
   * @return {@code true} if sent; {@code false} if not ready yet (retry)
   */
  public boolean showConfiguration(Player player) {
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    if (user == null) {
      return false;
    }

    ConnectionState state;
    try {
      state = user.getConnectionState();
    } catch (Exception e) {
      return false;
    }
    if (state != ConnectionState.CONFIGURATION) {
      return false;
    }

    ClientVersion version = user.getClientVersion();
    if (version != null && version != ClientVersion.UNKNOWN && version.isOlderThan(MIN_DIALOG_VERSION)) {
      return false;
    }

    try {
      Dialog dialog = buildDialog();
      user.sendPacket(new WrapperConfigServerClearDialog());
      user.sendPacket(new WrapperConfigServerShowDialog(dialog));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void clearConfiguration(Player player) {
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    if (user == null) {
      return;
    }
    try {
      user.sendPacket(new WrapperConfigServerClearDialog());
    } catch (Exception ignored) {
      // ignore
    }
  }

  public void openIfNeeded(Player player) {
    if (!rules.needsAccept(player)) {
      return;
    }
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    if (user == null || (user.getClientVersion() != null
        && user.getClientVersion() != ClientVersion.UNKNOWN
        && user.getClientVersion().isOlderThan(MIN_DIALOG_VERSION))) {
      sendChatFallback(player);
      return;
    }
    user.sendPacket(new WrapperPlayServerClearDialog());
    user.sendPacket(new WrapperPlayServerShowDialog(buildDialog()));
  }

  /** Re-open after missing checkbox (config or play). */
  public void reopen(Player player) {
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    if (user == null) {
      return;
    }
    try {
      ConnectionState state = user.getConnectionState();
      if (state == ConnectionState.CONFIGURATION) {
        showConfiguration(player);
        return;
      }
    } catch (Exception ignored) {
      // fall through to play
    }
    openIfNeeded(player);
  }

  public void clear(Player player) {
    User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    if (user == null) {
      return;
    }
    try {
      user.sendPacket(new WrapperPlayServerClearDialog());
    } catch (Exception ignored) {
      // ignore
    }
    try {
      user.sendPacket(new WrapperConfigServerClearDialog());
    } catch (Exception ignored) {
      // ignore
    }
  }

  private Dialog buildDialog() {
    Component body = parseBody();
    Component title = MessageUtil.parse(TownityMessages.RULES_TITLE);
    Component external = MessageUtil.parse(TownityMessages.DISCONNECT_PREFIX);

    Input checkbox = new Input(
        CHECKBOX_KEY,
        new BooleanInputControl(
            MessageUtil.parse(TownityMessages.RULES_CHECKBOX_LABEL),
            false,
            "true",
            "false"
        )
    );

    CommonDialogData data = new CommonDialogData(
        title,
        external,
        false,
        true,
        DialogAction.CLOSE,
        List.of(new PlainMessageDialogBody(new PlainMessage(body, BODY_WIDTH))),
        List.of(checkbox)
    );

    ActionButton accept = button(
        TownityMessages.RULES_ACCEPT_LABEL,
        "&7Nur mit gesetztem &6Häkchen &7möglich.",
        new DynamicCustomAction(ACCEPT, null)
    );
    ActionButton decline = button(
        TownityMessages.RULES_DECLINE_LABEL,
        "&7Regelwerk &6ablehnen &7und disconnecten.",
        new DynamicCustomAction(DECLINE, null)
    );

    return new MultiActionDialog(data, List.of(accept), decline, 1);
  }

  private Component parseBody() {
    String url = TownityMessages.RULES_URL;
    String[] lines = TownityMessages.RULES_BODY.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    Component result = Component.empty();
    for (int i = 0; i < lines.length; i++) {
      if (i > 0) {
        result = result.append(Component.newline());
      }
      result = result.append(lineWithOptionalUrl(lines[i], url));
    }
    return result;
  }

  private static Component lineWithOptionalUrl(String line, String url) {
    Component parsed = MessageUtil.parse(line);
    if (url.isEmpty() || !line.contains(url)) {
      return parsed;
    }
    return parsed
        .clickEvent(ClickEvent.openUrl(url))
        .hoverEvent(HoverEvent.showText(MessageUtil.parse("&7Öffnen: &6" + url)));
  }

  public void sendChatFallback(Player player) {
    String url = TownityMessages.RULES_URL;
    Messages.sendError(player, TownityMessages.RULES_NEED_UPDATE);
    for (String line : TownityMessages.RULES_BODY.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
      if (!line.isBlank()) {
        Component message = Messages.of(line);
        if (line.contains(url)) {
          message = message
              .clickEvent(ClickEvent.openUrl(url))
              .hoverEvent(HoverEvent.showText(MessageUtil.parse("&7Öffnen: &6" + url)));
        }
        player.sendMessage(message);
      }
    }
    player.sendMessage(
        Messages.of(TownityMessages.RULES_ACCEPT_LABEL)
            .clickEvent(ClickEvent.runCommand("/regeln akzeptieren"))
            .hoverEvent(HoverEvent.showText(MessageUtil.parse("&7Regelwerk &6akzeptieren&7.")))
            .append(MessageUtil.parse(" &7· "))
            .append(
                MessageUtil.parse(TownityMessages.RULES_DECLINE_LABEL)
                    .clickEvent(ClickEvent.runCommand("/regeln ablehnen"))
                    .hoverEvent(HoverEvent.showText(MessageUtil.parse("&7Regelwerk &6ablehnen&7.")))
            )
    );
  }

  private static ActionButton button(String label, String tooltip, Action action) {
    return new ActionButton(
        new CommonButtonData(MessageUtil.parse(label), MessageUtil.parse(tooltip), BUTTON_WIDTH),
        action
    );
  }

  private static ResourceLocation id(String path) {
    return new ResourceLocation("townity", "rules/" + path);
  }
}
