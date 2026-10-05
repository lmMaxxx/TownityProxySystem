# TownityProxySystem

Velocity-Plugin für Player-Cap, MOTD, Regelwerk-Akzeptanz und persistente Wartung.

**Design:** [`DESIGN.md`](DESIGN.md) – abgestimmt auf TownityCitybuildSystem `docs/DESIGNSPRACHE.md`

## Features

- Player-Cap live per Command (`/proxysystem set <zahl>`) – einzige Config-Option in `config.yml`
- MOTD, Messages, Regelwerk-Texte: **hardcoded** in `TownityMessages`
- Regelwerk-Akzeptanz beim Join (`https://regelwerk.townity.de`)
- Wartung (Dialog + Commands, persistent in `maintenance-state.yml`)
- `/lobby` / `/hub` → Server `Lobby`

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
mvn -B package
```

JAR: `target/TownityProxySystem-1.2.0.jar`

## Commands

| Command | Beschreibung |
|---|---|
| `/proxysystem set 80` | Slots live setzen (gespeichert) |
| `/proxysystem status` | Aktuelle Slots |
| `/proxysystem reload` | `max-players` neu laden |
| `/slots 80` | Kurzform |
| `/lobby` / `/hub` | Zur Lobby |
| `/regeln` | Regelwerk anzeigen / akzeptieren |
| `/wartung` | Wartungs-Dialog (1.21.6+) |

## Permissions

| Permission | Beschreibung |
|---|---|
| `townity.proxysystem.set` | Slots setzen |
| `townity.proxysystem.status` | Status |
| `townity.proxysystem.reload` | Reload |
| `townity.wartungssystem` | Wartung steuern |
| `townity.wartungssystem.bypass` | Wartung umgehen |

## Config

Nur noch:

```yaml
max-players: 100
```

Alles andere (Messages, MOTD, Regelwerk, Lobby-Name) steht in Code (`TownityMessages`).
