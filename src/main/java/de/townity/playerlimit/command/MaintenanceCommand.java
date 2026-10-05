package de.townity.playerlimit.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.townity.playerlimit.TownityPlayerLimit;
import de.townity.playerlimit.config.PluginConfig;
import de.townity.playerlimit.maintenance.MaintenanceDialogService;
import de.townity.playerlimit.maintenance.MaintenanceService;
import de.townity.playerlimit.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

public final class MaintenanceCommand implements SimpleCommand {

  private static final String ADMIN_PERMISSION = "townity.wartungssystem";

  private final TownityPlayerLimit plugin;
  private final MaintenanceService maintenance;
  private final MaintenanceDialogService dialogs;

  public MaintenanceCommand(
      TownityPlayerLimit plugin,
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
    PluginConfig.MaintenanceMessages messages = plugin.config().maintenance().messages();

    if (!source.hasPermission(ADMIN_PERMISSION)) {
      source.sendMessage(parse(plugin.config().messages().noPermission()));
      return;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
      if (source instanceof Player player) {
        dialogs.open(player);
      } else {
        source.sendMessage(parse(messages.usage()));
      }
      return;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "an", "ein", "on", "enable" -> {
        maintenance.setNetworkEnabled(true);
        source.sendMessage(parse(messages.enabled()));
        plugin.logger().info("Network maintenance enabled by {}", source);
      }
      case "aus", "off", "disable" -> {
        maintenance.setNetworkEnabled(false);
        source.sendMessage(parse(messages.disabled()));
        plugin.logger().info("Network maintenance disabled by {}", source);
      }
      case "status" -> source.sendMessage(maintenance.statusMessage());
      case "server" -> handleServer(source, args, messages);
      default -> source.sendMessage(parse(messages.usage()));
    }
  }

  private void handleServer(CommandSource source, String[] args, PluginConfig.MaintenanceMessages messages) {
    if (args.length < 3) {
      source.sendMessage(parse(messages.usage()));
      return;
    }
    String serverName = args[1];
    String toggle = args[2].toLowerCase(Locale.ROOT);
    boolean enable = toggle.equals("an") || toggle.equals("ein") || toggle.equals("on") || toggle.equals("enable");
    boolean disable = toggle.equals("aus") || toggle.equals("off") || toggle.equals("disable");
    if (!enable && !disable) {
      source.sendMessage(parse(messages.usage()));
      return;
    }

    if (!maintenance.setServerEnabled(serverName, enable)) {
      source.sendMessage(parse(messages.unknownServer().replace("%server%", serverName)));
      return;
    }

    RegisteredServer server = maintenance.findServer(serverName).orElseThrow();
    String name = server.getServerInfo().getName();
    String template = enable ? messages.serverEnabled() : messages.serverDisabled();
    source.sendMessage(parse(template.replace("%server%", name)));
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

  private Component parse(String message) {
    return MessageUtil.parsePrefixed(
        plugin.config().prefix(),
        message,
        plugin.config().maintenance().format()
    );
  }
}
