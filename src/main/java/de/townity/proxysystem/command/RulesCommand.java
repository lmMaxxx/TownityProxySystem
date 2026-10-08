package de.townity.proxysystem.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.rules.RulesDialogService;
import de.townity.proxysystem.rules.RulesService;
import de.townity.proxysystem.util.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RulesCommand implements SimpleCommand {

  private final TownityProxySystem plugin;
  private final RulesService rules;
  private final RulesDialogService dialogs;

  public RulesCommand(
      TownityProxySystem plugin,
      RulesService rules,
      RulesDialogService dialogs
  ) {
    this.plugin = plugin;
    this.rules = rules;
    this.dialogs = dialogs;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length > 0 && isResetSub(args[0])) {
      handleReset(source, args);
      return;
    }

    if (!(source instanceof Player player)) {
      Messages.sendError(source, TownityMessages.RULES_PLAYERS_ONLY);
      return;
    }

    if (args.length == 0) {
      if (rules.needsAccept(player)) {
        dialogs.openIfNeeded(player);
      } else {
        Messages.send(player, TownityMessages.RULES_ALREADY);
      }
      return;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "akzeptieren", "accept", "ja", "yes" -> {
        if (!rules.needsAccept(player)) {
          Messages.send(player, TownityMessages.RULES_ALREADY);
          return;
        }
        rules.accept(player);
        dialogs.clear(player);
        Messages.sendSuccess(player, TownityMessages.RULES_ACCEPTED);
      }
      case "ablehnen", "decline", "nein", "no" -> {
        if (!rules.needsAccept(player)) {
          Messages.send(player, TownityMessages.RULES_ALREADY);
          return;
        }
        rules.decline(player);
      }
      default -> Messages.send(player, TownityMessages.RULES_USAGE);
    }
  }

  private void handleReset(CommandSource source, String[] args) {
    if (!source.hasPermission(TownityMessages.RULES_RESET_PERMISSION)) {
      Messages.sendError(source, TownityMessages.NO_PERMISSION);
      return;
    }
    if (args.length < 2) {
      Messages.send(source, TownityMessages.RULES_RESET_USAGE);
      return;
    }

    String target = args[1];
    if (target.equalsIgnoreCase("all") || target.equalsIgnoreCase("*") || target.equalsIgnoreCase("alle")) {
      int count = rules.resetAll();
      for (Player online : plugin.server().getAllPlayers()) {
        notifyReset(online);
      }
      Messages.sendSuccess(source, TownityMessages.RULES_RESET_ALL.replace("%count%", String.valueOf(count)));
      return;
    }

    Optional<Player> online = plugin.server().getPlayer(target);
    if (online.isPresent()) {
      Player player = online.get();
      boolean had = rules.reset(player.getUniqueId());
      notifyReset(player);
      if (had) {
        Messages.sendSuccess(
            source,
            TownityMessages.RULES_RESET_PLAYER.replace("%player%", player.getUsername())
        );
      } else {
        Messages.send(
            source,
            TownityMessages.RULES_RESET_PLAYER_NONE.replace("%player%", player.getUsername())
        );
      }
      return;
    }

    UUID uuid;
    try {
      uuid = UUID.fromString(target);
    } catch (IllegalArgumentException e) {
      Messages.sendError(source, TownityMessages.RULES_RESET_UNKNOWN_PLAYER.replace("%player%", target));
      return;
    }

    boolean had = rules.reset(uuid);
    if (had) {
      Messages.sendSuccess(source, TownityMessages.RULES_RESET_PLAYER.replace("%player%", uuid.toString()));
    } else {
      Messages.send(source, TownityMessages.RULES_RESET_PLAYER_NONE.replace("%player%", uuid.toString()));
    }
  }

  private void notifyReset(Player player) {
    if (!rules.needsAccept(player)) {
      return;
    }
    Messages.send(player, TownityMessages.RULES_RESET_NOTIFY);
    dialogs.openIfNeeded(player);
  }

  private static boolean isResetSub(String arg) {
    String sub = arg.toLowerCase(Locale.ROOT);
    return sub.equals("reset") || sub.equals("zurücksetzen") || sub.equals("zuruecksetzen");
  }

  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
    String[] args = invocation.arguments();
    CommandSource source = invocation.source();

    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
      List<String> options = new ArrayList<>(List.of("akzeptieren", "ablehnen"));
      if (source.hasPermission(TownityMessages.RULES_RESET_PERMISSION)) {
        options.add("reset");
      }
      return CompletableFuture.completedFuture(
          options.stream().filter(s -> s.startsWith(prefix)).toList()
      );
    }

    if (args.length == 2 && isResetSub(args[0])
        && source.hasPermission(TownityMessages.RULES_RESET_PERMISSION)) {
      String prefix = args[1].toLowerCase(Locale.ROOT);
      List<String> options = new ArrayList<>();
      options.add("all");
      for (Player player : plugin.server().getAllPlayers()) {
        options.add(player.getUsername());
      }
      return CompletableFuture.completedFuture(
          options.stream()
              .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
              .toList()
      );
    }

    return CompletableFuture.completedFuture(List.of());
  }
}
