package de.townity.proxysystem.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.rules.RulesDialogService;
import de.townity.proxysystem.rules.RulesService;
import de.townity.proxysystem.util.Messages;
import java.util.UUID;

public final class RulesDialogListener extends PacketListenerAbstract {

  private final TownityProxySystem plugin;
  private final RulesService rules;
  private final RulesDialogService dialogs;
  private final RulesJoinListener joinListener;

  public RulesDialogListener(
      TownityProxySystem plugin,
      RulesService rules,
      RulesDialogService dialogs,
      RulesJoinListener joinListener
  ) {
    super(PacketListenerPriority.HIGH);
    this.plugin = plugin;
    this.rules = rules;
    this.dialogs = dialogs;
    this.joinListener = joinListener;
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    ResourceLocation id;
    NBT payload;

    if (event.getPacketType() == PacketType.Configuration.Client.CUSTOM_CLICK_ACTION) {
      WrapperConfigClientCustomClickAction wrapper = new WrapperConfigClientCustomClickAction(event);
      id = wrapper.getId();
      payload = wrapper.getPayload();
    } else if (event.getPacketType() == PacketType.Play.Client.CUSTOM_CLICK_ACTION) {
      WrapperPlayClientCustomClickAction wrapper = new WrapperPlayClientCustomClickAction(event);
      id = wrapper.getId();
      payload = wrapper.getPayload();
    } else {
      return;
    }

    if (id == null || !"townity".equals(id.getNamespace()) || !id.getKey().startsWith("rules/")) {
      return;
    }

    event.setCancelled(true);

    UUID uuid = event.getUser() == null ? null : event.getUser().getUUID();
    if (uuid == null) {
      return;
    }

    ResourceLocation actionId = id;
    NBT actionPayload = payload;
    plugin.server().getPlayer(uuid).ifPresent(player ->
        plugin.server().getScheduler()
            .buildTask(plugin, () -> handle(player, actionId, actionPayload))
            .schedule()
    );
  }

  private void handle(Player player, ResourceLocation id, NBT payload) {
    String key = id.getKey();
    if (key.endsWith("accept")) {
      if (!isCheckboxChecked(payload)) {
        Messages.sendError(player, TownityMessages.RULES_CHECKBOX_REQUIRED);
        dialogs.reopen(player);
        return;
      }
      handleAccept(player);
      return;
    }
    if (key.endsWith("decline")) {
      // Nicht die Config-Continuation fortsetzen – sonst Dirt-Loading-Loop.
      // Disconnect räumt pending in RulesJoinListener.onDisconnect auf.
      plugin.logger().info("Rules declined by {}", player.getUsername());
      rules.decline(player);
    }
  }

  private void handleAccept(Player player) {
    if (!rules.needsAccept(player) && !joinListener.isPending(player.getUniqueId())) {
      dialogs.clear(player);
      return;
    }
    rules.accept(player);
    boolean fromConfig = joinListener.completeAccept(player);
    dialogs.clear(player);
    if (!fromConfig) {
      Messages.sendSuccess(player, TownityMessages.RULES_ACCEPTED);
    }
    plugin.logger().info("Rules accepted by {}", player.getUsername());
  }

  private static boolean isCheckboxChecked(NBT payload) {
    if (!(payload instanceof NBTCompound compound)) {
      return false;
    }
    NBT tag = compound.getTagOrNull(RulesDialogService.CHECKBOX_KEY);
    if (tag == null) {
      return false;
    }
    if (tag instanceof NBTString string) {
      String value = string.getValue();
      return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }
    if (tag instanceof NBTByte nbtByte) {
      return nbtByte.getAsByte() != 0;
    }
    if (tag instanceof NBTNumber number) {
      return number.getAsInt() != 0;
    }
    if (compound.getBooleanOr(RulesDialogService.CHECKBOX_KEY, false)) {
      return true;
    }
    String asString = compound.getStringTagValueOrNull(RulesDialogService.CHECKBOX_KEY);
    return asString != null && ("true".equalsIgnoreCase(asString) || "1".equals(asString));
  }
}
