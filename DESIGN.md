# Townity.DE – Designsprache (Proxy)

Abgestimmt auf **TownityCitybuildSystem** `docs/DESIGNSPRACHE.md`.  
Gilt für Chat, MOTD, Disconnect, Dialoge und Plugin-Texte in TownityProxySystem.

---

## Chat-Nachrichten

| Rolle | Code | Verwendung |
|-------|------|------------|
| Prefix | `&6&lTOWNITY.DE &7⇒` | Vor jeder normalen / Erfolgs-Nachricht |
| Wichtiges | `&6` (Gold) | Namen, Statusworte, Zahlen, Hervorhebungen |
| Rest | `&7` (Grau) | Fließtext, Satzzeichen, Füllwörter |
| Fehler | `&c` (Rot) | **Immer ohne Prefix** |

### Beispiele

```text
✓ &6&lTOWNITY.DE &7⇒ &7Slots live auf &6100 &7gesetzt. &7(&642&7/&6100&7)
✓ &6&lTOWNITY.DE &7⇒ &7Wartung für &6Lobby &7wurde &6aktiviert&7.
✓ &cDazu hast du keine Berechtigung!

✗ Fehler mit Prefix
✗ &aErfolg / bunte Wildwuchs-Farben
✗ &l im Message-Body (Bold nur im Prefix / Dialog-Titel)
```

### Code

- Klasse: `de.townity.proxysystem.util.Messages` / `TownitySounds`
- Info: `Messages.send(...)`
- Erfolg: `Messages.sendSuccess(...)` → Prefix + Orb-Pickup
- Fehler: `Messages.sendError(...)` → nur `&c`, kein Prefix + Villager-No
- Texte: `TownityMessages` (hardcoded)

---

## Disconnect / Kick (zwei Zeilen)

```text
Zeile 1: &6&lTOWNITY.DE
Zeile 2: Body mit &6 / &7
```

Kein Pfeil auf dem Disconnect-Screen.

---

## MOTD

Zwei Zeilen – Banner + Small-Caps-Tags (`&e✦` / `&e•`).

```text
&6■■■■■■■■■■■■ &e✦ &6&lTOWNITY.DE&r &e✦ &6■■■■■■■■■■■■
&6       ⚙ &6ᴄɪᴛʏʙᴜɪʟᴅ &e• &6ᴇᴠᴇɴᴛꜱ &e• &6ᴄᴏᴍᴍᴜɴɪᴛʏ ⚙
```

Wartung: Tags `ᴡᴀʀᴛᴜɴɢ • ʙᴀʟᴅ • ᴢᴜʀüᴄᴋ`. Brand in der MOTD mit `&6&l` (danach `&r`).

---

## Dialoge

Native Minecraft-Dialoge (1.21.6+, PacketEvents vom Proxy).

| Element | Vorgabe |
|---------|---------|
| Titel | `&8» &6&l…` (wie Citybuild-GUIs), z. B. `&8» &6&lRegelwerk`, `&8» &6&lWartung` |
| External Title | `&6&lTOWNITY.DE` |
| Body / Buttons | `&7` Fließtext, `&6` wichtig, **kein** `&l` im Body |
| Menü-Sound | nur Chat-Fallback / Command; Client-Dialog-Klick schon vorhanden |

Regelwerk erscheint in der **Configuration-Phase** (Login). Ablehnen = Disconnect.

---

## Permissions

```text
townity.<plugin>.<aktion>
```

Beispiele: `townity.proxysystem.set`, `townity.wartungssystem`, `townity.wartungssystem.bypass`  
Kein Regelwerk-Bypass.

---

## Checklist

- [ ] Chat = Prefix **mit** `⇒`
- [ ] Fehler = `&c` **ohne** Prefix
- [ ] Erfolg = Prefix + optional Success-Sound
- [ ] Disconnect = 2 Zeilen, Prefix **ohne** `⇒`
- [ ] Body: unwichtig `&7`, wichtig `&6`
- [ ] `&l` nur Prefix / Dialog-Titel
- [ ] Brand immer `TOWNITY.DE`
- [ ] Dialog-Titel `&8» &6&l…`
