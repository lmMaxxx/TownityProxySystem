package de.townity.proxysystem;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.townity.proxysystem.command.BauServerCommand;
import de.townity.proxysystem.command.LobbyCommand;
import de.townity.proxysystem.command.MaintenanceCommand;
import de.townity.proxysystem.command.ProxySystemCommand;
import de.townity.proxysystem.command.RangShopCommand;
import de.townity.proxysystem.command.RulesCommand;
import de.townity.proxysystem.config.ConfigManager;
import de.townity.proxysystem.config.PluginConfig;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.limit.PlayerLimitService;
import de.townity.proxysystem.listener.LoginListener;
import de.townity.proxysystem.listener.MaintenanceDialogListener;
import de.townity.proxysystem.listener.MaintenanceListener;
import de.townity.proxysystem.listener.PingListener;
import de.townity.proxysystem.listener.PlaytimeListener;
import de.townity.proxysystem.listener.RangShopDialogListener;
import de.townity.proxysystem.listener.RulesDialogListener;
import de.townity.proxysystem.listener.RulesJoinListener;
import de.townity.proxysystem.maintenance.MaintenanceDialogService;
import de.townity.proxysystem.maintenance.MaintenanceService;
import de.townity.proxysystem.rangshop.LuckPermsBridge;
import de.townity.proxysystem.rangshop.PlaytimePointsStore;
import de.townity.proxysystem.rangshop.RangShopDialogService;
import de.townity.proxysystem.rangshop.RangShopService;
import de.townity.proxysystem.rules.RulesAcceptanceStore;
import de.townity.proxysystem.rules.RulesDialogService;
import de.townity.proxysystem.rules.RulesService;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(
    id = "townityproxysystem",
    name = "TownityProxySystem",
    version = "1.3.0",
    description = "Proxy system: player cap, MOTD, rules, maintenance and playtime rangshop.",
    authors = {"Townity"},
    dependencies = {
        @Dependency(id = "luckperms", optional = true)
    }
)
public final class TownityProxySystem {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private final PluginContainer pluginContainer;
  private final ConfigManager configManager;
  private MaintenanceService maintenanceService;
  private MaintenanceDialogService dialogService;
  private RulesService rulesService;
  private RulesDialogService rulesDialogService;
  private PlayerLimitService playerLimitService;
  private RangShopService rangShopService;
  private RangShopDialogService rangShopDialogService;

  @Inject
  public TownityProxySystem(
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

    RulesAcceptanceStore rulesStore = new RulesAcceptanceStore(dataDirectory, logger);
    rulesStore.load();
    rulesService = new RulesService(this, rulesStore);
    rulesDialogService = new RulesDialogService(this, rulesService);
    RulesJoinListener rulesJoinListener = new RulesJoinListener(this, rulesService, rulesDialogService);

    PlaytimePointsStore pointsStore = new PlaytimePointsStore(dataDirectory, logger);
    pointsStore.load();
    LuckPermsBridge luckPermsBridge = new LuckPermsBridge(logger);
    rangShopService = new RangShopService(this, pointsStore, luckPermsBridge);
    rangShopDialogService = new RangShopDialogService(this, rangShopService);
    rangShopService.start();

    PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, pluginContainer, logger, dataDirectory));
    PacketEvents.getAPI().getSettings().checkForUpdates(false);
    PacketEvents.getAPI().load();
    PacketEvents.getAPI().getEventManager().registerListener(
        new MaintenanceDialogListener(this, maintenanceService, dialogService)
    );
    PacketEvents.getAPI().getEventManager().registerListener(
        new RulesDialogListener(this, rulesService, rulesDialogService, rulesJoinListener)
    );
    PacketEvents.getAPI().getEventManager().registerListener(
        new RangShopDialogListener(this, rangShopService, rangShopDialogService)
    );
    PacketEvents.getAPI().init();

    server.getEventManager().register(this, new MaintenanceListener(this, maintenanceService));
    server.getEventManager().register(this, new LoginListener(this, playerLimitService));
    server.getEventManager().register(this, new PingListener(this));
    server.getEventManager().register(this, rulesJoinListener);
    server.getEventManager().register(this, new PlaytimeListener(rangShopService));

    CommandManager commandManager = server.getCommandManager();
    CommandMeta proxyMeta = commandManager.metaBuilder("proxysystem")
        .aliases("tpsys", "tpsystem", "slots")
        .plugin(this)
        .build();
    commandManager.register(proxyMeta, new ProxySystemCommand(this));

    CommandMeta maintenanceMeta = commandManager.metaBuilder("wartung")
        .aliases("maintenance", "maintenancemode")
        .plugin(this)
        .build();
    commandManager.register(maintenanceMeta, new MaintenanceCommand(this, maintenanceService, dialogService));

    CommandMeta lobbyMeta = commandManager.metaBuilder("lobby")
        .aliases("hub", "l", "leave")
        .plugin(this)
        .build();
    commandManager.register(lobbyMeta, new LobbyCommand(this));

    CommandMeta bauserverMeta = commandManager.metaBuilder("bauserver")
        .aliases("bs")
        .plugin(this)
        .build();
    commandManager.register(bauserverMeta, new BauServerCommand(this));

    CommandMeta rulesMeta = commandManager.metaBuilder("regeln")
        .aliases("rules", "regelwerk")
        .plugin(this)
        .build();
    commandManager.register(rulesMeta, new RulesCommand(this, rulesService, rulesDialogService));

    CommandMeta rangShopMeta = commandManager.metaBuilder("vrangshop")
        .aliases("rangshop")
        .plugin(this)
        .build();
    commandManager.register(rangShopMeta, new RangShopCommand(rangShopDialogService));

    PluginConfig config = configManager.config();
    PluginConfig.MaintenanceState state = configManager.maintenanceState();
    logger.info(
        "TownityProxySystem enabled – max players: {}, rules v{}, network maintenance: {}, servers: {}, luckperms: {}",
        config.maxPlayers(),
        TownityMessages.RULES_VERSION,
        state.networkEnabled(),
        state.servers().isEmpty() ? "-" : state.servers(),
        luckPermsBridge.isAvailable()
    );
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    if (rangShopService != null) {
      rangShopService.shutdown();
    }
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

  public RulesService rules() {
    return rulesService;
  }

  public RulesDialogService rulesDialogs() {
    return rulesDialogService;
  }

  public RangShopService rangShop() {
    return rangShopService;
  }

  public RangShopDialogService rangShopDialogs() {
    return rangShopDialogService;
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

  public boolean setMaxPlayers(int maxPlayers) {
    boolean ok = configManager.setMaxPlayers(maxPlayers);
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
