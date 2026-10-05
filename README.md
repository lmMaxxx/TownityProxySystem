# TownityPlayerLimit

Velocity-Plugin (4.1.0-SNAPSHOT) für Player-Cap, MOTD und persistente Wartung.

**Design / Brand für Messages & MOTD:** siehe [`DESIGN.md`](DESIGN.md)

## Features

- Einstellbares Player-Cap (`max-players`)
- Live-Reload: `/playerlimit reload`
- Bypass bei vollem Server: `townity.playerlimit.bypass`
- Wartung: `townity.wartungssystem` / `townity.wartungssystem.bypass`
- Nachrichten & MOTD mit **MiniMessage**, **`&`**, **`§`** und Hex (`&#RRGGBB`, `&x&r&r&g&g&b&b`)
- MOTD-Import von Website-Exports
- **Wartung (neustart-persistent)**
  - Dialog: `/wartung` (Minecraft 1.21.6+, Paper-Dialogs auf dem Proxy)
  - ganzes Netzwerk: `/wartung an|aus`
  - einzelne Backend-Server: `/wartung server <name> an|aus`
  - ohne Bypass: Join blockiert / Kick beim Aktivieren
  - Status in `maintenance-state.yml` (überlebt Proxy-Restarts)

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
mvn -B package
```

JAR: `target/TownityPlayerLimit-1.2.0.jar`

## Commands

| Command | Beschreibung |
|---|---|
| `/wartung` | Öffnet den Wartungs-Dialog (Spieler, 1.21.6+) |
| `/playerlimit reload` | Config neu laden |
| `/wartung an` | Netzwerk-Wartung an (kickt ohne Bypass) |
| `/wartung aus` | Netzwerk-Wartung aus |
| `/wartung server lobby an` | Nur Server `lobby` in Wartung |
| `/wartung server lobby aus` | Server-Wartung aus |
| `/wartung status` | Aktueller Status |

Alias: `/maintenance`

## Permissions

| Permission | Beschreibung |
|---|---|
| `townity.playerlimit.bypass` | Join trotz Player-Cap |
| `townity.playerlimit.reload` | Config reload |
| `townity.wartungssystem.bypass` | Wartung umgehen |
| `townity.wartungssystem` | Wartung steuern (Command + Dialog) |

## Wartung

Beim Aktivieren:
1. Zustand wird nach `plugins/townityplayerlimit/maintenance-state.yml` geschrieben
2. Alle Spieler ohne Bypass werden gekickt (Netzwerk bzw. betroffener Server)
3. Neue Joins / Server-Wechsel ohne Bypass werden blockiert
4. Optional: Maintenance-MOTD in der Serverliste

Nach einem Proxy-Neustart bleibt der Wartungsstatus erhalten.
