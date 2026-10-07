package de.townity.proxysystem.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.util.Messages;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class BauServerCommand implements SimpleCommand {

  private final TownityProxySystem plugin;

  public BauServerCommand(TownityProxySystem plugin) {
    this.plugin = plugin;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();

    if (!source.hasPermission(TownityMessages.BAUSERVER_PERMISSION)) {
      Messages.sendError(source, TownityMessages.NO_PERMISSION);
      return;
    }

    if (!(source instanceof Player player)) {
      Messages.sendError(source, TownityMessages.BAUSERVER_PLAYERS_ONLY);
      return;
    }

    String targetName = TownityMessages.BAUSERVER_SERVER;
    Optional<RegisteredServer> target = findServer(targetName);
    if (target.isEmpty()) {
      Messages.sendError(player, TownityMessages.BAUSERVER_UNKNOWN.replace("%server%", targetName));
      return;
    }

    RegisteredServer bauserver = target.get();
    String serverName = bauserver.getServerInfo().getName();
    Optional<ServerConnection> current = player.getCurrentServer();
    if (current.isPresent()
        && current.get().getServerInfo().getName().equalsIgnoreCase(serverName)) {
      Messages.send(player, TownityMessages.BAUSERVER_ALREADY.replace("%server%", serverName));
      return;
    }

    player.createConnectionRequest(bauserver).connect().whenComplete((result, error) -> {
      if (error != null) {
        Messages.sendError(player, TownityMessages.BAUSERVER_FAILED.replace("%server%", serverName));
        plugin.logger().warn("Bauserver connect failed for {}: {}", player.getUsername(), error.toString());
        return;
      }
      if (result != null && result.getStatus() == ConnectionRequestBuilder.Status.SUCCESS) {
        Messages.sendSuccess(player, TownityMessages.BAUSERVER_SUCCESS.replace("%server%", serverName));
      } else if (result != null && result.getStatus() == ConnectionRequestBuilder.Status.ALREADY_CONNECTED) {
        Messages.send(player, TownityMessages.BAUSERVER_ALREADY.replace("%server%", serverName));
      } else {
        Messages.sendError(player, TownityMessages.BAUSERVER_FAILED.replace("%server%", serverName));
      }
    });
  }

  private Optional<RegisteredServer> findServer(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    Optional<RegisteredServer> exact = plugin.server().getServer(name);
    if (exact.isPresent()) {
      return exact;
    }
    String needle = name.toLowerCase(Locale.ROOT);
    for (RegisteredServer server : plugin.server().getAllServers()) {
      if (server.getServerInfo().getName().toLowerCase(Locale.ROOT).equals(needle)) {
        return Optional.of(server);
      }
    }
    return Optional.empty();
  }

  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().hasPermission(TownityMessages.BAUSERVER_PERMISSION);
  }
}
