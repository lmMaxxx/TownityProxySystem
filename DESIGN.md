# Townity Design System (Minecraft / Proxy)

Leitfaden für Chat, MOTD, Disconnect-Screens und Plugin-Texte.  
Ziel: alle Townity-Plugins und Messages wirken einheitlich.

---

## Marke

| Element | Wert |
|---|---|
| Name | **TOWNITY.DE** |
| Domain / Brand | `TOWNITY.DE` (immer so schreiben, groß) |
| Ton | kurz, klar, deutsch, freundlich-direkt |

---

## Farben (Legacy `&`)

| Rolle | Code | Bedeutung | Einsatz |
|---|---|---|---|
| **Primary / Accent** | `&6` | Gold | Wichtige Wörter, Brand, Hervorhebungen |
| **Secondary / Body** | `&7` | Grau | Fließtext, unwichtige Wörter |
| **Accent (MOTD only)** | `&e` | Gelb | Sterne `✦`, Trenner `•` in der MOTD |
| Bold | `&l` | Fett | **Nur** im Chat-/Disconnect-Prefix (`TOWNITY.DE`) |

### Fett-Regel

- `&l` **nur** in `prefix` und `disconnect-prefix`
- MOTD, Message-Bodies, Kick-Texte: **kein** `&l` / Bold

### Nicht verwenden (in Player-Messages)

- `&c` / `&a` / `&f` als Default-Message-Farben (außer bewusste Sonderfälle)
- bunte Rainbow-Spam-Messages
- MiniMessage-Gradients in Standard-Chat-Texten (MOTD optional ok)

---

## Prefixes

### Chat (eine Zeile)

```text
&6&lTOWNITY.DE &7⇒ 
```

- Immer vor Chat-/Command-Feedback
- Pfeil `⇒` nur im Chat
- Config-Key: `prefix`

**Beispiel**

```text
&6&lTOWNITY.DE &7⇒ &7Du wurdest &6erfolgreich &7in die &6Farmwelt teleportiert&7!
```

### Disconnect / Kick (zwei Zeilen)

```text
Zeile 1: &6&lTOWNITY.DE
Zeile 2: <Body mit &6 / &7>
```

- **Kein** Pfeil auf dem Disconnect-Screen
- Config-Key: `disconnect-prefix`
- Oben nur Brand, unten nur Text

**Beispiel**

```text
TOWNITY.DE
Das Netzwerk ist derzeit in Wartung.
```

Legacy:

```text
&6&lTOWNITY.DE
&7Das Netzwerk ist derzeit in &6Wartung&7.
```

---

## Message-Regeln

1. Body **ohne** Prefix speichern (Prefix wird im Code vorangestellt).
2. Fließtext = `&7`
3. Wichtige Wörter/Aktionen/Namen = `&6`
4. Satzzeichen am Ende wieder auf `&7` setzen, wenn danach noch Text kommt oder der Satz endet
5. Platzhalter wie `%server%`, `%online%`, `%max%` in `&6` einfassen, wenn sie „wichtig“ sind

### Muster

```text
&7Du wurdest &6erfolgreich &7in die &6Farmwelt teleportiert&7!
&7Der Server ist aktuell &6voll&7! &7(&6%online%&7/&6%max%&7)
&7Wartung für &6%server% &7wurde &6aktiviert&7.
```

### Anti-Muster

```text
&cFehler!          ❌ (falsche Farbe)
&aErfolg!          ❌
&7Erfolgreich...   ❌ (wichtiges Wort nicht hervorgehoben)
TOWNITY.DE ⇒ ...   ❌ (Prefix fehlt &-Codes / falsches Format)
```

---

## MOTD (Serverliste)

Zwei Zeilen. Zeile 1 = Banner, Zeile 2 = Tags (visuell zentriert unter dem Banner).

### Normal

```text
&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■
&6       ⚙ &6ᴄɪᴛʏʙᴜɪʟᴅ &e• &6ᴇᴠᴇɴᴛꜱ &e• &6ᴄᴏᴍᴍᴜɴɪᴛʏ ⚙
```

### Wartung

```text
&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■
&6          ⚙ &6ᴡᴀʀᴛᴜɴɢ &e• &6ʙᴀʟᴅ &e• &6ᴢᴜʀüᴄᴋ ⚙
```

### MOTD-Bausteine

| Element | Zeichen / Style |
|---|---|
| Rahmen | `■` in `&6` |
| Sterne | `&e✦` |
| Brand | `&6TOWNITY.DE` (**ohne** `&l`) |
| Trenner Tags | `&e•` |
| Icons | `⚙` |
| Tag-Text | Small Caps (Unicode), Farbe `&6` |
| Einrückung Zeile 2 | Spaces so wählen, dass optisch mittig unter Zeile 1 |

**Hinweis:** Minecraft-Font ist nicht monospaced. Nach Textänderungen Spaces in-game prüfen und nachjustieren.

---

## Small Caps (MOTD-Tags)

Für die zweite MOTD-Zeile Unicode-Small-Caps nutzen, z. B.:

`ᴀ ʙ ᴄ ᴅ ᴇ ꜰ ɢ ʜ ɪ ᴊ ᴋ ʟ ᴍ ɴ ᴏ ᴘ ǫ ʀ ꜱ ᴛ ᴜ ᴠ ᴡ x ʏ ᴢ`

Beispiele: `ᴄɪᴛʏʙᴜɪʟᴅ`, `ᴇᴠᴇɴᴛꜱ`, `ᴄᴏᴍᴍᴜɴɪᴛʏ`, `ᴡᴀʀᴛᴜɴɢ`

---

## Format-Support (technisch)

Plugins sollen Messages/MOTDs parsen können als:

| Format | Beispiel |
|---|---|
| Legacy `&` | `&6Hallo &7Welt` |
| Legacy `§` | `§6Hallo §7Welt` |
| Hex | `&#FFAA00` / `&x&f&f&a&a&0&0` |
| MiniMessage | `<gold>Hallo</gold>` (optional, nicht Default-Chat) |

Default-Stil für Townity-Texte bleibt **Legacy `&`**.

---

## Permissions-Naming

```text
townity.<plugin>.<aktion>
```

Beispiele:

- `townity.playerlimit.bypass`
- `townity.playerlimit.reload`
- `townity.wartungssystem.bypass`
- `townity.wartungssystem`

---

## Checklist für neue Messages

- [ ] Chat = Prefix **mit** `⇒`
- [ ] Disconnect = 2 Zeilen, Prefix **ohne** `⇒`
- [ ] Body: unwichtig `&7`, wichtig `&6`
- [ ] `&l` nur im Prefix, nirgends sonst
- [ ] Brand immer `TOWNITY.DE`
- [ ] Keine Random-Farben (`&c`/`&a`) im Standard-Flow
- [ ] MOTD: gleiche Banner-Zeile, Tags zentriert, Small Caps, **ohne** Bold
- [ ] In-game einmal gegengeprüft (besonders MOTD-Spaces)

---

## Dialoge (Wartung)

`/wartung` ohne Argumente öffnet einen nativen Minecraft-Dialog (1.21.6+, gleiche Client-Feature wie Paper Dialog API, vom Proxy gesendet).

- Titel = Disconnect-Prefix (`&6&lTOWNITY.DE`, fett nur hier)
- Body und Buttons: `&7` Fließtext, `&6` wichtig, **kein** `&l`
- Buttons: Netzwerk an/aus, je Unterserver an/aus
- Clients darunter: klickbares Chat-Menü im gleichen Farbschema

---

## Quick Copy

```text
# Chat-Prefix
&6&lTOWNITY.DE &7⇒ 

# Disconnect-Titel
&6&lTOWNITY.DE

# Chat-Body-Template
&7... &6WICHTIG &7...&7!

# MOTD Banner
&6■■■■■■■■■■■■ &e✦ &6TOWNITY.DE &e✦ &6■■■■■■■■■■■■
```
