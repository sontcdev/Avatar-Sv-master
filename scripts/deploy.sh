#!/usr/bin/env bash
# Root, on homelab: deploy.sh <reviewed-local-release-directory>
set -euo pipefail
[[ $EUID == 0 ]] || { echo 'Run with sudo on homelab'; exit 1; }
[[ $# == 1 && -f /etc/avatar2d/managed ]] || { echo 'Usage: deploy.sh RELEASE_DIR (after setup)'; exit 1; }
source_dir=$(realpath -- "$1")
[[ -f "$source_dir/server.jar" && -d "$source_dir/res/hd" && -d "$source_dir/res/medium" && -f "$source_dir/config.properties" ]]
private_ip=$(tailscale ip -4)
[[ $private_ip =~ ^100\.([0-9]{1,3}\.){2}[0-9]{1,3}$ ]] || { echo 'No Tailscale IPv4'; exit 1; }
release=/opt/avatar2d/releases/$(date -u +%Y%m%dT%H%M%S)-$(sha256sum "$source_dir/server.jar" | cut -c1-12)
[[ ! -e $release ]]
install -d -m 750 -o root -g avatar2d "$release"
cp -R -- "$source_dir/." "$release/"
sed -i "s/^server.bind=.*/server.bind=$private_ip/" "$release/config.properties"
rm -f "$release/database.properties"
ln -s /var/log/avatar2d "$release/logs"
chown -R root:avatar2d "$release"
chmod -R u=rwX,g=rX,o= "$release"
# Keep last known release; rollback never imports or downgrades player data.
if [[ -L /opt/avatar2d/current ]]; then
  old=$(readlink -f /opt/avatar2d/current)
  /opt/avatar2d/current/scripts/backup.sh
  ln -sfn "$old" /opt/avatar2d/previous
fi
systemctl stop avatar2d.service
mariadb avatar_2x < "$release/database/private-compat.sql"
mariadb avatar_2x -e "UPDATE users SET login_lock=0;"
ln -sfn "$release" /opt/avatar2d/current
systemctl enable avatar2d.service
systemctl start avatar2d.service
health_log=$(mktemp)
trap 'rm -f "$health_log"' EXIT
for attempt in {1..30}; do
  if "$release/scripts/healthcheck.sh" > "$health_log" 2>&1; then
    cat "$health_log"
    exit 0
  fi
  sleep 2
done
cat "$health_log"
systemctl status avatar2d --no-pager || true
tail -n 100 /var/log/avatar2d/console.log
exit 1
