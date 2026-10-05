package de.townity.proxysystem.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.rules.RulesDialogService;
import de.townity.proxysystem.rules.RulesService;
import de.townity.proxysystem.util.Messages;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class RulesCommand implements SimpleCommand {

  private final RulesService rules;
  private final RulesDialogService dialogs;

  public RulesCommand(
      de.townity.proxysystem.TownityProxySystem plugin,
      RulesService rules,
      RulesDialogService dialogs
  ) {
    this.rules = rules;
    this.dialogs = dialogs;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    if (!(source instanceof Player player)) {
      Messages.sendError(source, TownityMessages.RULES_PLAYERS_ONLY);
      return;
    }

    String[] args = invocation.arguments();
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

  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
    String[] args = invocation.arguments();
    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
      return CompletableFuture.completedFuture(
          List.of("akzeptieren", "ablehnen").stream()
              .filter(s -> s.startsWith(prefix))
              .toList()
      );
    }
    return CompletableFuture.completedFuture(List.of());
  }
}
