package de.townity.proxysystem.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.util.MessageUtil;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

public final class PlayerLimitCommand implements SimpleCommand {

  private static final String RELOAD_PERMISSION = "townity.playerlimit.reload";

  private final TownityProxySystem plugin;

  public PlayerLimitCommand(TownityProxySystem plugin) {
    this.plugin = plugin;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
      source.sendMessage(parse(plugin.config().messages().usage()));
      return;
    }

    if (!source.hasPermission(RELOAD_PERMISSION)) {
      source.sendMessage(parse(plugin.config().messages().noPermission()));
      return;
    }

    boolean ok = plugin.reload();
    MessageUtil.Placeholders placeholders =
        new MessageUtil.Placeholders(plugin.onlinePlayers(), plugin.config().maxPlayers());

    if (ok) {
      source.sendMessage(parse(placeholders.apply(plugin.config().messages().reloadSuccess())));
      plugin.logger().info("Config reloaded – max players: {}", plugin.config().maxPlayers());
    } else {
      source.sendMessage(parse(plugin.config().messages().reloadFailed()));
    }
  }

  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
    String[] args = invocation.arguments();
    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0].toLowerCase();
      if ("reload".startsWith(prefix)) {
        return CompletableFuture.completedFuture(List.of("reload"));
      }
    }
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return true;
  }

  private Component parse(String message) {
    return MessageUtil.parsePrefixed(plugin.config().prefix(), message, "auto");
  }
}
