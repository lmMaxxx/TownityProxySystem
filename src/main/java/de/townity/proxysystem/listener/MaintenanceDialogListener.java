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
import de.townity.proxysystem.maintenance.MaintenanceDialogService;
import de.townity.proxysystem.maintenance.MaintenanceService;
import de.townity.proxysystem.util.Messages;
import java.util.UUID;

public final class MaintenanceDialogListener extends PacketListenerAbstract {

  private static final String ADMIN_PERMISSION = "townity.wartungssystem";

  private final TownityProxySystem plugin;
  private final MaintenanceService maintenance;
  private final MaintenanceDialogService dialogs;

  public MaintenanceDialogListener(
      TownityProxySystem plugin,
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
      Messages.sendError(player, TownityMessages.NO_PERMISSION);
      return;
    }

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
      Messages.sendSuccess(player, TownityMessages.MAINTENANCE_ENABLED);
      plugin.logger().info("Network maintenance enabled by {}", player.getUsername());
      dialogs.open(player);
      return;
    }

    if (key.endsWith("network_off")) {
      maintenance.setNetworkEnabled(false);
      Messages.sendSuccess(player, TownityMessages.MAINTENANCE_DISABLED);
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
      Messages.sendError(player, TownityMessages.MAINTENANCE_UNKNOWN_SERVER.replace("%server%", serverName));
      return;
    }

    String template = enable
        ? TownityMessages.MAINTENANCE_SERVER_ENABLED
        : TownityMessages.MAINTENANCE_SERVER_DISABLED;
    Messages.sendSuccess(player, template.replace("%server%", serverName));
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
}
