#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 && $# == 1 ]] || { echo 'Run with sudo: verify-deployment.sh RELEASE_DIR'; exit 1; }
release=$(realpath -- "$1")
bash "$(dirname -- "$0")/deploy.sh" "$release"
if [[ ! -f /etc/avatar2d/test-account-avatartest.txt ]]; then
  python3 /opt/avatar2d/current/scripts/create-test-account.py avatartest
fi
# Permit the SSH operator to use this dedicated non-admin account for client QA.
install -m 600 -o sontc -g sontc /etc/avatar2d/test-account-avatartest.txt /home/sontc/avatar2d-staging/test-account.txt
/opt/avatar2d/current/scripts/backup.sh
systemctl restart avatar2d
for attempt in {1..30}; do
  if /opt/avatar2d/current/scripts/healthcheck.sh; then break; fi
  sleep 2
done
/opt/avatar2d/current/scripts/healthcheck.sh
printf '\nService runtime metrics:\n'
systemctl show avatar2d -p ActiveState -p SubState -p MainPID -p MemoryCurrent -p TasksCurrent -p CPUUsageNSec -p NRestarts
printf '\nDatabase validation (no credentials):\n'
mariadb avatar_2x -Nse "SELECT VERSION(); SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='avatar_2x'; SELECT COUNT(*) FROM settings; SELECT COUNT(*) FROM users WHERE username='avatartest' AND role=-1 AND active=1; SELECT COUNT(*) FROM users WHERE username IN ('admin','ad') AND active<>0; SHOW VARIABLES LIKE 'innodb_buffer_pool_size'; SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Threads_running','Slow_queries');"
printf '\nRecent startup log:\n'
tail -n 100 /var/log/avatar2d/console.log
printf '\nOther workload state:\n'
systemctl --failed --no-pager
printf '\nProvision/deploy/backup/restart checks finished. Android gameplay remains pending.\n'
