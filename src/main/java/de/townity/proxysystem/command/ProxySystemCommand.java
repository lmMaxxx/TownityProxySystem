package de.townity.proxysystem.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.PluginConfig;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.util.MessageUtil;
import de.townity.proxysystem.util.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ProxySystemCommand implements SimpleCommand {

  private static final String RELOAD_PERMISSION = "townity.proxysystem.reload";
  private static final String SET_PERMISSION = "townity.proxysystem.set";
  private static final String STATUS_PERMISSION = "townity.proxysystem.status";

  private final TownityProxySystem plugin;

  public ProxySystemCommand(TownityProxySystem plugin) {
    this.plugin = plugin;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 0) {
      Messages.send(source, TownityMessages.USAGE);
      return;
    }

    if (args[0].matches("\\d+")) {
      handleSet(source, new String[] {"set", args[0]});
      return;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "reload" -> handleReload(source);
      case "set", "slots", "slot", "max" -> handleSet(source, args);
      case "status", "info" -> handleStatus(source);
      default -> Messages.send(source, TownityMessages.USAGE);
    }
  }

  private void handleReload(CommandSource source) {
    if (!source.hasPermission(RELOAD_PERMISSION)) {
      Messages.sendError(source, TownityMessages.NO_PERMISSION);
      return;
    }
    boolean ok = plugin.reload();
    if (ok) {
      Messages.sendSuccess(source, placeholders().apply(TownityMessages.RELOAD_SUCCESS));
      plugin.logger().info("Config reloaded – max players: {}", plugin.config().maxPlayers());
    } else {
      Messages.sendError(source, TownityMessages.RELOAD_FAILED);
    }
  }

  private void handleSet(CommandSource source, String[] args) {
    if (!source.hasPermission(SET_PERMISSION)) {
      Messages.sendError(source, TownityMessages.NO_PERMISSION);
      return;
    }
    if (args.length < 2) {
      Messages.send(source, TownityMessages.USAGE);
      return;
    }
    int max;
    try {
      max = Integer.parseInt(args[1].trim());
    } catch (NumberFormatException e) {
      Messages.sendError(source, TownityMessages.SET_INVALID);
      return;
    }
    if (max < 0) {
      Messages.sendError(source, TownityMessages.SET_INVALID);
      return;
    }

    if (!plugin.setMaxPlayers(max)) {
      Messages.sendError(source, TownityMessages.SET_FAILED);
      return;
    }

    Messages.sendSuccess(source, placeholders().apply(TownityMessages.SET_SUCCESS));
    plugin.logger().info("Max players set to {} by {}", max, source);
  }

  private void handleStatus(CommandSource source) {
    if (!source.hasPermission(STATUS_PERMISSION) && !source.hasPermission(SET_PERMISSION)) {
      Messages.sendError(source, TownityMessages.NO_PERMISSION);
      return;
    }
    Messages.send(source, placeholders().apply(TownityMessages.STATUS));
  }

  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
    String[] args = invocation.arguments();
    if (args.length == 0) {
      return CompletableFuture.completedFuture(List.of("reload", "set", "slots", "status"));
    }
    if (args.length == 1) {
      return CompletableFuture.completedFuture(
          filter(List.of("reload", "set", "slots", "status", "info", "max"), args[0])
      );
    }
    if (args.length == 2) {
      String sub = args[0].toLowerCase(Locale.ROOT);
      if (sub.equals("set") || sub.equals("slots") || sub.equals("slot") || sub.equals("max")) {
        int current = plugin.config().maxPlayers();
        return CompletableFuture.completedFuture(
            filter(List.of(String.valueOf(current), "50", "100", "150", "200"), args[1])
        );
      }
    }
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().hasPermission(RELOAD_PERMISSION)
        || invocation.source().hasPermission(SET_PERMISSION)
        || invocation.source().hasPermission(STATUS_PERMISSION);
  }

  private MessageUtil.Placeholders placeholders() {
    PluginConfig config = plugin.config();
    int occupied = plugin.playerLimit() != null
        ? plugin.playerLimit().occupiedSlots(config)
        : plugin.onlinePlayers();
    return new MessageUtil.Placeholders(occupied, config.maxPlayers());
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
