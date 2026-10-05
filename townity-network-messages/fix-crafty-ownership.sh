#!/bin/bash
# Auf dem Linux-Host als root ausführen (SSH / Konsole):
#   bash fix-crafty-ownership.sh
#
# Setzt Besitz der Server-Dateien zurück auf den Crafty-User.

set -euo pipefail

OWNER="${1:-crafty:crafty}"
TARGET="${2:-/home/servers}"

echo "chown -R $OWNER $TARGET"
chown -R "$OWNER" "$TARGET"

# Optional: auch network-proxy lang
if [[ -d /home/network/proxy/lang ]]; then
  echo "chown -R $OWNER /home/network/proxy/lang"
  chown -R "$OWNER" /home/network/proxy/lang
fi

echo "Fertig. Prüfen:"
ls -ln "$TARGET"/*/spigot.yml 2>/dev/null || true
ls -ldn "$TARGET"/*/lang 2>/dev/null || true
