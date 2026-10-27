#!/usr/bin/env bash
set -euo pipefail
systemctl is-active --quiet avatar2d.service
private_ip=$(tailscale ip -4)
ss -lntH | awk -v ip="$private_ip" '$4 == ip ":19128" || $4 == "[::ffff:" ip "]:19128" {print $4; found=1} END {exit !found}' 
# Handshake diagnostics are protocol-aware, not a CCU or gameplay test.
python3 /opt/avatar2d/current/scripts/protocol-smoke.py "$private_ip" 19128
if [[ $EUID == 0 ]]; then
  mariadb avatar_2x -Nse 'SELECT COUNT(*) AS npc_count FROM npc; SELECT COUNT(*) AS item_count FROM items;'
fi
