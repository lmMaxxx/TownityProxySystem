# Townity Network Messages

Fertige Nachrichten für **Velocity (Proxy)** und **Paper (Unterserver)** –  
deutsch, Townity-Design (`&6` / `&7`), für alle Client-Sprachen.

```
townity-network-messages/
├── README.md                 ← du bist hier
├── velocity/
│   └── lang/                 ← alle Locale-Dateien (identisch, deutsch)
└── paper/
    ├── README.md
    ├── spigot-messages.yml   ← in jede Paper-spigot.yml
    └── restart-pro-server.yml
```

## 1) Velocity (Proxy)

1. Inhalt von `velocity/lang/` nach `<proxy>/lang/` kopieren (überschreiben).
2. Proxy **neu starten**.

Damit sehen **alle** Clients dieselben deutschen Texte (EN/DE/FR/… egal).

Beispiele:
- Proxy runter → `TOWNITY.DE` / „Das Netzwerk fährt gerade herunter.“
- Unterserver weg / Failover → Verbindungstrennung im Townity-Stil
- Chat (`/server`, `/glist`, …) → `TOWNITY.DE ⇒ …`

## 2) Paper (Lobby, Citybuild, …)

1. Auf **jedem** Paper-Server in der `spigot.yml` den Block `messages:`  
   durch `paper/spigot-messages.yml` ersetzen.
2. Optional: serverbezogenen `restart`-Text aus `restart-pro-server.yml`.
3. Paper-Server neu starten.

Beispiele:
- Restart → `TOWNITY.DE` / „Der Server startet gerade neu.“
- Whitelist / voll / veralteter Client → gleicher Stil

Details: `paper/README.md`

## Zusammenspiel

| Situation | Wer zeigt die Nachricht? |
|---|---|
| Ganzer Proxy stoppt | Velocity `lang/` → Shutdown |
| Citybuild `/restart` | Paper `spigot.yml` → restart |
| Citybuild crasht / disconnect, Failover | Velocity `lang/` → Verbindungsfehler / getrennt |
| Wartung / Player-Cap | TownityProxySystem-Plugin (eigene Config) |

Alles nutzt dasselbe Design: **TOWNITY.DE** gold+fett, Body grau, Wichtiges gold.
