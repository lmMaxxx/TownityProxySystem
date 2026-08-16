# TownityPlayerLimit

Velocity-Plugin (4.1.0-SNAPSHOT), das die maximale Spieleranzahl auf dem Proxy capped.

## Features

- Einstellbares Player-Cap (`max-players`)
- Live-Reload per `/playerlimit reload` (Aliases: `/plimit`, `/playercap`)
- Bypass-Permission für Joins bei vollem Server
- Konfigurierbare Kick-/Feedback-Nachrichten (MiniMessage oder Legacy)
- Optionale MOTD-Überschreibung inkl. Export von MOTD-Websites (`&`/`§`, MiniMessage, JSON)
- Zeigt das Cap in der Serverliste (`update-ping-max-players`)

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
mvn -B package
```

Fertiges JAR: `target/TownityPlayerLimit-1.0.0.jar`

## Installation

1. JAR nach `plugins/` legen
2. Velocity starten
3. `plugins/townityplayerlimit/config.yml` anpassen
4. `/playerlimit reload` ausführen

## Permissions

| Permission | Beschreibung |
|---|---|
| `townity.playerlimit.bypass` | Join trotz vollem Server |
| `townity.playerlimit.reload` | Config neu laden |

## MOTD aus Websites

1. MOTD auf einer Creator-Seite bauen (z. B. Minecraft Tools / MCTools)
2. Export mit `&`- oder `§`-Codes, MiniMessage oder JSON kopieren
3. Entweder in `motd.lines` einfügen oder in eine Datei legen:

```yaml
motd:
  enabled: true
  format: auto
  file: motd.txt
  lines: []
```

`motd.txt` im Plugin-Ordner – eine Zeile = eine MOTD-Zeile. Platzhalter: `%online%`, `%max%`, `%remaining%`.

## Commands

- `/playerlimit reload` – lädt `config.yml` und MOTD-Datei neu
