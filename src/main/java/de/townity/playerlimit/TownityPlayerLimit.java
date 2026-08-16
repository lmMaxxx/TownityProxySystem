package de.townity.playerlimit;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.townity.playerlimit.command.PlayerLimitCommand;
import de.townity.playerlimit.config.ConfigManager;
import de.townity.playerlimit.config.PluginConfig;
import de.townity.playerlimit.listener.LoginListener;
import de.townity.playerlimit.listener.PingListener;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(
    id = "townityplayerlimit",
    name = "TownityPlayerLimit",
    version = "1.0.0",
    description = "Caps maximum player count with live reload, bypass and MOTD support.",
    authors = {"Townity"}
)
public final class TownityPlayerLimit {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private final ConfigManager configManager;

  @Inject
  public TownityPlayerLimit(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.configManager = new ConfigManager(dataDirectory, logger);
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    configManager.load();
    server.getEventManager().register(this, new LoginListener(this));
    server.getEventManager().register(this, new PingListener(this));

    CommandManager commandManager = server.getCommandManager();
    CommandMeta meta = commandManager.metaBuilder("playerlimit")
        .aliases("plimit", "playercap")
        .plugin(this)
        .build();
    commandManager.register(meta, new PlayerLimitCommand(this));

    PluginConfig config = configManager.config();
    logger.info("TownityPlayerLimit enabled – max players: {}", config.maxPlayers());
  }

  public ProxyServer server() {
    return server;
  }

  public Logger logger() {
    return logger;
  }

  public Path dataDirectory() {
    return dataDirectory;
  }

  public ConfigManager configManager() {
    return configManager;
  }

  public PluginConfig config() {
    return configManager.config();
  }

  public boolean reload() {
    return configManager.reload();
  }

  public int onlinePlayers() {
    return server.getPlayerCount();
  }

  public boolean isFull() {
    return onlinePlayers() >= config().maxPlayers();
  }
}
