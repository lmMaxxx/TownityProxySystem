# Paper – Townity-Nachrichten einbauen

## Was hier liegt

| Datei | Zweck |
|---|---|
| `spigot-messages.yml` | Fertiger `messages:`-Block für **alle** Paper-Unterserver |
| `restart-pro-server.yml` | Optionale, serverbezogene Restart-Texte |

Paper hat **kein** `lang/`-System wie Velocity. Die Standard-Kicks sitzen in der **`spigot.yml`**.

## Installation (pro Unterserver)

1. Paper-Server stoppen (Lobby, Citybuild, …).
2. `spigot.yml` öffnen.
3. Den bestehenden Block `messages:` komplett durch den Inhalt von `spigot-messages.yml` ersetzen  
   (oder die Werte Zeile für Zeile übernehmen).
4. Optional: in `restart` den Text aus `restart-pro-server.yml` für diesen Server nehmen.
5. Server starten.

**Beispiel fertig in der `spigot.yml`:**

```yaml
messages:
  whitelist: "&7Du stehst nicht auf der &6Whitelist&7."
  unknown-command: "&6&lTOWNITY.DE &7⇒ &7Unbekannter Befehl. Nutze &6/help&7."
  server-full: "&7Der Server ist aktuell &6voll&7."
  outdated-client: "&7Dein Client ist &6veraltet&7. Bitte nutze &6{0}&7."
  outdated-server: "&7Dein Client ist &6zu neu&7. Server läuft auf &6{0}&7."
  restart: "&7Der Server startet gerade &6neu&7."
```

## Wichtig: Restart vs. Stop

| Aktion | Nachricht |
|---|---|
| `/restart` (oder Restart-Script) | nutzt `messages.restart` → Townity-Design |
| `/stop` | zeigt oft noch Vanilla „Server closed“ |

Für schöne Kicks beim Runterfahren: **`/restart`** nutzen (mit Restart-Script), nicht nur `/stop`.

Bei Velocity mit Failover siehst du zusätzlich die Proxy-Texte aus `velocity/lang/`  
(z. B. „Verbindung zu Citybuild fehlgeschlagen“).

## Design hinter Velocity

- **Disconnect/Kick:** nur Body (`&7` / `&6`) – **kein** `TOWNITY.DE`-Titel  
  (sonst doppelt, weil Velocity den Titel schon setzt)
- **Chat** (`unknown-command`): Prefix `TOWNITY.DE ⇒` bleibt
- `&r` nach dem Prefix nur nötig, wenn der Titel auf dem Paper-Server selbst steht
