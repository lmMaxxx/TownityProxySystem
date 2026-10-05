# Velocity – Townity `lang/`

Inhalt nach `<proxy>/lang/` kopieren und Proxy **neu starten**.

Alle Locale-Dateien sind **identisch** (deutsch + Townity).

**Nicht anlegen:** `messages_en.properties`, `messages_en_US.properties`,
`messages_de.properties` usw. – die kollidieren mit `messages.properties`
bzw. `messages_de_DE.properties` und lassen Velocity mit
`Translation already exists … for en_US` abstürzen.
