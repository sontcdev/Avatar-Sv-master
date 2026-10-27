#!/usr/bin/env bash
# Run as root only after reviewing this file. No existing DB is replaced.
set -euo pipefail
[[ $EUID == 0 ]] || { echo 'Run with sudo'; exit 1; }
cd -- "$(dirname -- "$0")/.."
[[ -f deploy/avatar2d.service && -f database/avatar_2x.sql ]]
if ! command -v mariadb >/dev/null; then
  [[ ! -d /var/lib/mysql/mysql ]] || { echo 'Existing MySQL data found; inspect manually'; exit 1; }
  export NEEDRESTART_MODE=l DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y --no-install-recommends openjdk-17-jre-headless mariadb-server python3 logrotate
fi
command -v java >/dev/null || { echo 'Install openjdk-17-jre-headless first'; exit 1; }
# Refuse to adopt an existing installation that has no Avatar ownership marker.
if [[ ! -f /etc/avatar2d/managed ]]; then
  [[ ! -e /opt/avatar2d && ! -e /etc/avatar2d ]] || { echo 'Avatar paths exist; inspect manually'; exit 1; }
  [[ $(mariadb -Nse "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='avatar_2x'") == 0 ]] || { echo 'avatar_2x exists; refusing to adopt'; exit 1; }
  [[ $(mariadb -Nse "SELECT COUNT(*) FROM mysql.user WHERE user='avatar2d'") == 0 ]] || { echo 'avatar2d DB account exists; inspect manually'; exit 1; }
  getent passwd avatar2d >/dev/null && { echo 'avatar2d OS account exists; inspect manually'; exit 1; }
  useradd --system --home-dir /var/lib/avatar2d --shell /usr/sbin/nologin avatar2d
  install -d -m 750 -o root -g avatar2d /etc/avatar2d /opt/avatar2d /opt/avatar2d/releases
  install -d -m 700 -o avatar2d -g avatar2d /var/lib/avatar2d /var/log/avatar2d
  install -d -m 700 /var/backups/avatar2d
  python3 - <<'PYCONFIG'
import secrets,pathlib
password=secrets.token_hex(24)
pathlib.Path('/etc/avatar2d/database.properties').write_text('driver=com.mysql.cj.jdbc.Driver\nhost=127.0.0.1\nport=3306\ndbname=avatar_2x\nusername=avatar2d\npassword='+password+'\nmax_connection=10\nmin_connection=2\n')
pathlib.Path('/etc/avatar2d/create-account.sql').write_text("CREATE DATABASE avatar_2x CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;\nCREATE USER 'avatar2d'@'127.0.0.1' IDENTIFIED BY '"+password+"';\nGRANT SELECT,INSERT,UPDATE,DELETE ON avatar_2x.* TO 'avatar2d'@'127.0.0.1';\n")
PYCONFIG
  chmod 600 /etc/avatar2d/create-account.sql
  chmod 640 /etc/avatar2d/database.properties
  chown root:avatar2d /etc/avatar2d/database.properties
  mariadb < /etc/avatar2d/create-account.sql
  rm /etc/avatar2d/create-account.sql
  mariadb avatar_2x < database/avatar_2x.sql
  mariadb avatar_2x -e "UPDATE users SET active=0,role=-1,password=MD5(UUID()),login_lock=0;"
  touch /etc/avatar2d/managed
fi
# Validate existing DB is loopback-only; never change a shared DB configuration.
if ss -lntH | awk '$4 ~ /:3306$/ {print $4}' | grep -Ev '^(127\.0\.0\.1|\[::1\]):3306$'; then
  echo 'Database has a non-loopback listener; refusing to start Avatar'; exit 1
fi
install -m 644 deploy/avatar2d.service /etc/systemd/system/avatar2d.service
install -m 644 deploy/avatar2d.logrotate /etc/logrotate.d/avatar2d
systemctl daemon-reload
printf '%s\n' 'Privileged setup complete. No game service started. Return to Codex for deployment.'
