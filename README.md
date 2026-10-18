# TownityProxySystem

Velocity-Plugin für Player-Cap, MOTD, Regelwerk-Akzeptanz, persistente Wartung und Rangshop.

**Design:** [`DESIGN.md`](DESIGN.md) – abgestimmt auf die netzwerkweite Townity-`DESIGN.md` / Designsprache (Citybuild, Lobby, Vanish).

## Features

- Player-Cap live per Command (`/proxysystem set <zahl>`) – einzige Config-Option in `config.yml`
- MOTD, Messages, Regelwerk-Texte: **hardcoded** in `TownityMessages`
- Regelwerk-Akzeptanz beim Join (`https://regelwerk.townity.de`)
- Wartung (Dialog + Commands, persistent in `maintenance-state.yml`)
- `/lobby` / `/hub` → Server `Lobby`
- **Rangshop** (`/vrangshop`): Spielzeitpunkte → LuckPerms-Ränge Ritter → Baron → Graf (Dialog, 1.21.6+)

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
mvn -B package
```

JAR: `target/TownityProxySystem-1.3.0.jar`

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
| `/vrangshop` | Rangshop-Dialog (Spielzeitpunkte) |

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

Alles andere (Messages, MOTD, Regelwerk, Lobby-Name, Rangshop) steht in Code (`TownityMessages` / `ShopRank`).

## Rangshop

Voraussetzung: **LuckPerms auf Velocity**, Gruppen `ritter`, `baron`, `graf` angelegt.

| Rang | Kosten (Punkte) | Hinweis |
|---|---|---|
| Ritter | 180 | ~3 Std. Netzwerk-Spielzeit |
| Baron | 540 | nur nach Ritter |
| Graf | 1080 | nur nach Baron |

- 1 Minute Online im Netzwerk = 1 Punkt
- Maximal verdienbar: Summe aller Kosten (1800)
- Reihenfolge Pflicht; Rechte werden im Dialog angezeigt
- Höherer Rang (z. B. Team) → Hinweis im Dialog, keine weiteren Punkte
- Persistenz: `rangshop-points.yml`
