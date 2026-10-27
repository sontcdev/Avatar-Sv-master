#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 && -f /etc/avatar2d/managed ]] || { echo 'Run with sudo after setup'; exit 1; }
umask 077
backup=/var/backups/avatar2d/$(date -u +%Y%m%dT%H%M%S)-$$
mkdir "$backup"
# Stop only Avatar so multi-table player writes cannot straddle the snapshot.
was_active=false
if systemctl is-active --quiet avatar2d; then was_active=true; systemctl stop avatar2d; fi
trap 'if $was_active; then systemctl start avatar2d; fi' EXIT
mariadb-dump --single-transaction --routines --events avatar_2x | gzip > "$backup/database.sql.gz"
cp /etc/avatar2d/database.properties "$backup/"
readlink -f /opt/avatar2d/current > "$backup/release.txt"
gzip -t "$backup/database.sql.gz"
echo "$backup"
