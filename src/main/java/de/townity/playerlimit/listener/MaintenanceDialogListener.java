package de.townity.playerlimit.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.velocitypowered.api.proxy.Player;
import de.townity.playerlimit.TownityPlayerLimit;
import de.townity.playerlimit.config.PluginConfig;
import de.townity.playerlimit.maintenance.MaintenanceDialogService;
import de.townity.playerlimit.maintenance.MaintenanceService;
import de.townity.playerlimit.util.MessageUtil;
import java.util.UUID;

public final class MaintenanceDialogListener extends PacketListenerAbstract {

  private static final String ADMIN_PERMISSION = "townity.wartungssystem";

  private final TownityPlayerLimit plugin;
  private final MaintenanceService maintenance;
  private final MaintenanceDialogService dialogs;

  public MaintenanceDialogListener(
      TownityPlayerLimit plugin,
      MaintenanceService maintenance,
      MaintenanceDialogService dialogs
  ) {
    super(PacketListenerPriority.HIGH);
    this.plugin = plugin;
    this.maintenance = maintenance;
    this.dialogs = dialogs;
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (event.getPacketType() != PacketType.Play.Client.CUSTOM_CLICK_ACTION) {
      return;
    }

    WrapperPlayClientCustomClickAction wrapper = new WrapperPlayClientCustomClickAction(event);
    ResourceLocation id = wrapper.getId();
    if (id == null || !"townity".equals(id.getNamespace()) || !id.getKey().startsWith("maintenance/")) {
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
    if (!player.hasPermission(ADMIN_PERMISSION)) {
      player.sendMessage(parse(plugin.config().messages().noPermission()));
      return;
    }

    PluginConfig.MaintenanceMessages messages = plugin.config().maintenance().messages();
    String key = id.getKey();

    if (key.endsWith("open_servers")) {
      dialogs.openServers(player);
      return;
    }

    if (key.endsWith("open_main")) {
      dialogs.open(player);
      return;
    }

    if (key.endsWith("network_on")) {
      maintenance.setNetworkEnabled(true);
      player.sendMessage(parse(messages.enabled()));
      plugin.logger().info("Network maintenance enabled by {}", player.getUsername());
      dialogs.open(player);
      return;
    }

    if (key.endsWith("network_off")) {
      maintenance.setNetworkEnabled(false);
      player.sendMessage(parse(messages.disabled()));
      plugin.logger().info("Network maintenance disabled by {}", player.getUsername());
      dialogs.open(player);
      return;
    }

    String serverName = readServer(payload);
    if (serverName == null || serverName.isBlank()) {
      return;
    }

    boolean enable = key.endsWith("server_on");
    if (!maintenance.setServerEnabled(serverName, enable)) {
      player.sendMessage(parse(messages.unknownServer().replace("%server%", serverName)));
      return;
    }

    String template = enable ? messages.serverEnabled() : messages.serverDisabled();
    player.sendMessage(parse(template.replace("%server%", serverName)));
    plugin.logger().info(
        "Server maintenance {} for {} by {}",
        enable ? "enabled" : "disabled",
        serverName,
        player.getUsername()
    );
    dialogs.openServers(player);
  }

  private static String readServer(NBT payload) {
    if (payload instanceof NBTCompound compound) {
      return compound.getStringTagValueOrNull("server");
    }
    return null;
  }

  private net.kyori.adventure.text.Component parse(String message) {
    return MessageUtil.parsePrefixed(
        plugin.config().prefix(),
        message,
        plugin.config().maintenance().format()
    );
  }
}
