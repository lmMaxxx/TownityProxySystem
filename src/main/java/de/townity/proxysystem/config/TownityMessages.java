package de.townity.proxysystem.config;

import java.util.List;

/**
 * Hardcoded Townity texts. Nur {@code max-players} liegt in config.yml.
 * Stil: TownityCitybuildSystem docs/DESIGNSPRACHE.md
 */
public final class TownityMessages {

  private TownityMessages() {}

  /** Ohne trailing space – {@link de.townity.proxysystem.util.Messages} hängt Leerzeichen an. */
  public static final String PREFIX = "&6&lTOWNITY.DE &7⇒";
  public static final String DISCONNECT_PREFIX = "&6&lTOWNITY.DE";
  public static final String FORMAT = "auto";

  public static final String LOBBY_SERVER = "Lobby";
  public static final boolean UPDATE_PING_MAX_PLAYERS = true;

  public static final String FULL =
      "&7Der Server ist aktuell &6voll&7! &7(&6%online%&7/&6%max%&7)";
  public static final String RELOAD_SUCCESS =
      "&7Die Config wurde &6erfolgreich &7neu geladen&7. &7Max: &6%max%";
  public static final String RELOAD_FAILED = "Der Reload ist fehlgeschlagen. Siehe Konsole.";
  public static final String NO_PERMISSION = "Dazu hast du keine Berechtigung!";
  public static final String USAGE =
      "&7Verwendung: &6/proxysystem set <zahl>&7 | &6status&7 | &6reload&7 | &6/wartung";
  public static final String SET_SUCCESS =
      "&7Slots live auf &6%max% &7gesetzt. &7(&6%online%&7/&6%max%&7)";
  public static final String SET_FAILED = "Slots konnten nicht gespeichert werden. Siehe Konsole.";
  public static final String SET_INVALID = "Bitte eine gültige Zahl angeben (0 oder höher).";
  public static final String STATUS = "&7Slots: &6%online%&7/&6%max%";

  public static final String LOBBY_SUCCESS = "&7Du wirst zu &6%server% &7verbunden.";
  public static final String LOBBY_ALREADY = "&7Du bist bereits auf &6%server%&7.";
  public static final String LOBBY_UNKNOWN = "Lobby-Server %server% wurde nicht gefunden.";
  public static final String LOBBY_FAILED = "Verbindung zu %server% fehlgeschlagen.";
  public static final String LOBBY_PLAYERS_ONLY = "Nur Spieler können zur Lobby.";

  public static final String BAUSERVER_SERVER = "Bauserver";
  public static final String BAUSERVER_PERMISSION = "townity.proxysystem.builder";
  public static final String BAUSERVER_SUCCESS = "&7Du wirst zu &6%server% &7verbunden.";
  public static final String BAUSERVER_ALREADY = "&7Du bist bereits auf &6%server%&7.";
  public static final String BAUSERVER_UNKNOWN = "Bauserver %server% wurde nicht gefunden.";
  public static final String BAUSERVER_FAILED = "Verbindung zu %server% fehlgeschlagen.";
  public static final String BAUSERVER_PLAYERS_ONLY = "Nur Spieler können zum Bauserver.";

  public static final boolean MOTD_ENABLED = true;
  public static final List<String> MOTD_LINES = List.of(
      "&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■",
      "&6       ⚙ &6ᴄɪᴛʏʙᴜɪʟᴅ &e• &6ᴇᴠᴇɴᴛꜱ &e• &6ᴄᴏᴍᴍᴜɴɪᴛʏ ⚙"
  );

  public static final boolean RULES_ENABLED = true;
  public static final int RULES_VERSION = 4;
  public static final String RULES_URL = "https://regelwerk.townity.de";
  /** Dialog-Titel wie Citybuild-GUIs: {@code &8» &6&l…} */
  public static final String RULES_TITLE = "&8» &6&lRegelwerk";
  public static final String RULES_BODY = """
      &7Bitte lies das &6Regelwerk &7von &6TOWNITY.DE
      &7unter &6https://regelwerk.townity.de
      &7und setze danach das &6Häkchen&7.
      """.strip();
  public static final String RULES_CHECKBOX_LABEL =
      "&7Ich habe das &6Regelwerk &7gelesen und akzeptiert";
  public static final String RULES_CHECKBOX_REQUIRED =
      "Bitte setze das Häkchen, um fortzufahren.";
  public static final String RULES_ACCEPT_LABEL = "&6Bestätigen";
  public static final String RULES_DECLINE_LABEL = "&7Ablehnen";
  public static final String RULES_DECLINE_KICK =
      "&7Du hast das &6Regelwerk &7nicht akzeptiert.";
  public static final String RULES_NEED_UPDATE =
      "Bitte Minecraft 1.21.6+ nutzen, um das Regelwerk zu akzeptieren.";
  public static final String RULES_ACCEPTED =
      "&7Danke! Du hast das &6Regelwerk &7gelesen und akzeptiert.";
  public static final String RULES_ALREADY =
      "&7Du hast das &6Regelwerk &7bereits akzeptiert.";
  public static final String RULES_USAGE =
      "&7Verwendung: &6/regeln &7| &6akzeptieren&7 | &6ablehnen";
  public static final String RULES_PLAYERS_ONLY = "Nur Spieler können das Regelwerk akzeptieren.";

  public static final String MAINTENANCE_BYPASS = "townity.wartungssystem.bypass";
  public static final boolean MAINTENANCE_KICK_ON_ENABLE = true;
  public static final boolean MAINTENANCE_BLOCK_JOIN = true;
  public static final boolean MAINTENANCE_OVERRIDE_MOTD = true;
  public static final String MAINTENANCE_DIALOG_TITLE = "&8» &6&lWartung";
  public static final List<String> MAINTENANCE_MOTD_LINES = List.of(
      "&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■",
      "&6          ⚙ &6ᴡᴀʀᴛᴜɴɢ &e• &6ʙᴀʟᴅ &e• &6ᴢᴜʀüᴄᴋ ⚙"
  );
  public static final String MAINTENANCE_NETWORK_KICK =
      "&7Das Netzwerk ist derzeit in &6Wartung&7.";
  public static final String MAINTENANCE_SERVER_KICK =
      "&7Der Server &6%server% &7ist in &6Wartung&7.";
  public static final String MAINTENANCE_NETWORK_DENIED =
      "&7Das Netzwerk ist derzeit in &6Wartung&7.";
  public static final String MAINTENANCE_SERVER_DENIED =
      "&7Du kannst &6%server% &7gerade nicht betreten (&6Wartung&7).";
  public static final String MAINTENANCE_ENABLED =
      "&7Netzwerk-Wartung wurde &6aktiviert&7.";
  public static final String MAINTENANCE_DISABLED =
      "&7Netzwerk-Wartung wurde &6deaktiviert&7.";
  public static final String MAINTENANCE_SERVER_ENABLED =
      "&7Wartung für &6%server% &7wurde &6aktiviert&7.";
  public static final String MAINTENANCE_SERVER_DISABLED =
      "&7Wartung für &6%server% &7wurde &6deaktiviert&7.";
  public static final String MAINTENANCE_STATUS =
      "&7Netzwerk: &6%network% &7| Server: &6%servers%";
  public static final String MAINTENANCE_UNKNOWN_SERVER = "Unbekannter Server: %server%";
  public static final String MAINTENANCE_USAGE =
      "&7Verwendung: &6/wartung &7| &6an&7/&6aus &7| &6server <name> <an|aus> &7| &6status";
}
