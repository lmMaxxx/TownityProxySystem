package de.townity.proxysystem.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.maintenance.MaintenanceDialogService;
import de.townity.proxysystem.maintenance.MaintenanceService;
import de.townity.proxysystem.util.Messages;
import de.townity.proxysystem.util.TownitySounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class MaintenanceCommand implements SimpleCommand {

  private static final String ADMIN_PERMISSION = "townity.wartungssystem";

  private final TownityProxySystem plugin;
  private final MaintenanceService maintenance;
  private final MaintenanceDialogService dialogs;

  public MaintenanceCommand(
      TownityProxySystem plugin,
      MaintenanceService maintenance,
      MaintenanceDialogService dialogs
  ) {
    this.plugin = plugin;
    this.maintenance = maintenance;
    this.dialogs = dialogs;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (!source.hasPermission(ADMIN_PERMISSION)) {
      Messages.sendError(source, TownityMessages.NO_PERMISSION);
      return;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
      if (source instanceof Player player) {
        TownitySounds.menu(player);
        dialogs.open(player);
      } else {
        Messages.send(source, TownityMessages.MAINTENANCE_USAGE);
      }
      return;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "an", "ein", "on", "enable" -> {
        maintenance.setNetworkEnabled(true);
        Messages.sendSuccess(source, TownityMessages.MAINTENANCE_ENABLED);
        plugin.logger().info("Network maintenance enabled by {}", source);
      }
      case "aus", "off", "disable" -> {
        maintenance.setNetworkEnabled(false);
        Messages.sendSuccess(source, TownityMessages.MAINTENANCE_DISABLED);
        plugin.logger().info("Network maintenance disabled by {}", source);
      }
      case "status" -> source.sendMessage(maintenance.statusMessage());
      case "server" -> handleServer(source, args);
      default -> Messages.send(source, TownityMessages.MAINTENANCE_USAGE);
    }
  }

  private void handleServer(CommandSource source, String[] args) {
    if (args.length < 3) {
      Messages.send(source, TownityMessages.MAINTENANCE_USAGE);
      return;
    }
    String serverName = args[1];
    String toggle = args[2].toLowerCase(Locale.ROOT);
    boolean enable = toggle.equals("an") || toggle.equals("ein") || toggle.equals("on") || toggle.equals("enable");
    boolean disable = toggle.equals("aus") || toggle.equals("off") || toggle.equals("disable");
    if (!enable && !disable) {
      Messages.send(source, TownityMessages.MAINTENANCE_USAGE);
      return;
    }

    if (!maintenance.setServerEnabled(serverName, enable)) {
      Messages.sendError(source, TownityMessages.MAINTENANCE_UNKNOWN_SERVER.replace("%server%", serverName));
      return;
    }

    RegisteredServer server = maintenance.findServer(serverName).orElseThrow();
    String name = server.getServerInfo().getName();
    String template = enable
        ? TownityMessages.MAINTENANCE_SERVER_ENABLED
        : TownityMessages.MAINTENANCE_SERVER_DISABLED;
    Messages.sendSuccess(source, template.replace("%server%", name));
    plugin.logger().info("Server maintenance {} for {} by {}", enable ? "enabled" : "disabled", name, source);
  }

  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
    String[] args = invocation.arguments();
    if (args.length == 0) {
      return CompletableFuture.completedFuture(List.of("an", "aus", "server", "status"));
    }
    if (args.length == 1) {
      return CompletableFuture.completedFuture(filter(List.of("an", "aus", "server", "status"), args[0]));
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("server")) {
      List<String> names = new ArrayList<>();
      for (RegisteredServer server : plugin.server().getAllServers()) {
        names.add(server.getServerInfo().getName());
      }
      return CompletableFuture.completedFuture(filter(names, args[1]));
    }
    if (args.length == 3 && args[0].equalsIgnoreCase("server")) {
      return CompletableFuture.completedFuture(filter(List.of("an", "aus"), args[2]));
    }
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().hasPermission(ADMIN_PERMISSION);
  }

  private static List<String> filter(List<String> options, String prefix) {
    String needle = prefix.toLowerCase(Locale.ROOT);
    List<String> result = new ArrayList<>();
    for (String option : options) {
      if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
        result.add(option);
      }
    }
    return result;
  }
}
