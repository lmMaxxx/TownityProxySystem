package de.townity.playerlimit;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.townity.playerlimit.command.MaintenanceCommand;
import de.townity.playerlimit.command.PlayerLimitCommand;
import de.townity.playerlimit.config.ConfigManager;
import de.townity.playerlimit.config.PluginConfig;
import de.townity.playerlimit.limit.PlayerLimitService;
import de.townity.playerlimit.listener.LoginListener;
import de.townity.playerlimit.listener.MaintenanceDialogListener;
import de.townity.playerlimit.listener.MaintenanceListener;
import de.townity.playerlimit.listener.PingListener;
import de.townity.playerlimit.maintenance.MaintenanceDialogService;
import de.townity.playerlimit.maintenance.MaintenanceService;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(
    id = "townityplayerlimit",
    name = "TownityPlayerLimit",
    version = "1.2.0",
    description = "Player cap, live reload, MOTD and persistent network/server maintenance.",
    authors = {"Townity"}
)
public final class TownityPlayerLimit {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private final PluginContainer pluginContainer;
  private final ConfigManager configManager;
  private MaintenanceService maintenanceService;
  private MaintenanceDialogService dialogService;
  private PlayerLimitService playerLimitService;

  @Inject
  public TownityPlayerLimit(
      ProxyServer server,
      Logger logger,
      @DataDirectory Path dataDirectory,
      PluginContainer pluginContainer
  ) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.pluginContainer = pluginContainer;
    this.configManager = new ConfigManager(dataDirectory, logger);
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    configManager.load();
    playerLimitService = new PlayerLimitService(this);
    playerLimitService.syncOnline(configManager.config());
    maintenanceService = new MaintenanceService(this);
    dialogService = new MaintenanceDialogService(this, maintenanceService);

    PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, pluginContainer, logger, dataDirectory));
    PacketEvents.getAPI().getSettings().checkForUpdates(false);
    PacketEvents.getAPI().load();
    PacketEvents.getAPI().getEventManager().registerListener(
        new MaintenanceDialogListener(this, maintenanceService, dialogService)
    );
    PacketEvents.getAPI().init();

    server.getEventManager().register(this, new MaintenanceListener(this, maintenanceService));
    server.getEventManager().register(this, new LoginListener(this, playerLimitService));
    server.getEventManager().register(this, new PingListener(this));

    CommandManager commandManager = server.getCommandManager();
    CommandMeta limitMeta = commandManager.metaBuilder("playerlimit")
        .aliases("plimit", "playercap")
        .plugin(this)
        .build();
    commandManager.register(limitMeta, new PlayerLimitCommand(this));

    CommandMeta maintenanceMeta = commandManager.metaBuilder("wartung")
        .aliases("maintenance", "maintenancemode")
        .plugin(this)
        .build();
    commandManager.register(maintenanceMeta, new MaintenanceCommand(this, maintenanceService, dialogService));

    PluginConfig config = configManager.config();
    PluginConfig.MaintenanceState state = configManager.maintenanceState();
    logger.info(
        "TownityPlayerLimit enabled – max players: {}, network maintenance: {}, servers: {}",
        config.maxPlayers(),
        state.networkEnabled(),
        state.servers().isEmpty() ? "-" : state.servers()
    );
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    PacketEvents.getAPI().terminate();
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

  public MaintenanceService maintenance() {
    return maintenanceService;
  }

  public MaintenanceDialogService dialogs() {
    return dialogService;
  }

  public PluginConfig config() {
    return configManager.config();
  }

  public boolean reload() {
    boolean ok = configManager.reload();
    if (ok && maintenanceService != null) {
      maintenanceService.enforcePersistedState();
    }
    if (ok && playerLimitService != null) {
      playerLimitService.syncOnline(configManager.config());
    }
    return ok;
  }

  public PlayerLimitService playerLimit() {
    return playerLimitService;
  }

  public int onlinePlayers() {
    return server.getPlayerCount();
  }

  public boolean isFull() {
    PluginConfig config = config();
    if (playerLimitService != null) {
      return playerLimitService.occupiedSlots(config) >= config.maxPlayers();
    }
    return onlinePlayers() >= config.maxPlayers();
  }
}
