#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 && -L /opt/avatar2d/previous ]] || { echo 'No previous release or not root'; exit 1; }
previous=$(readlink -f /opt/avatar2d/previous)
[[ $previous == /opt/avatar2d/releases/* && -f $previous/server.jar ]]
/opt/avatar2d/current/scripts/backup.sh
systemctl stop avatar2d
ln -sfn "$previous" /opt/avatar2d/current
systemctl start avatar2d
/opt/avatar2d/current/scripts/healthcheck.sh
