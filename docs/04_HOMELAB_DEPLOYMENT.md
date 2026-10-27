# Homelab deployment

Read-only baseline: Ubuntu24.04.5 LTS, Linux6.8.0-146, 3880MiB RAM, 2874MiB available,99MiB swap used,239GiB disk available. Two PostgreSQL18 services and eight running user services predate Avatar. Crawler/dispatcher on-demand behavior must remain unchanged. Initial system failed-unit count0; user manager reports degraded (must compare failed-unit list separately). Tailscale IPv4 discovered as100.112.0.62; no public networking configured.

User executed staged privileged setup because SSH sudo requires a password. Java17.0.20.1 and MariaDB10.11.14 are now installed. This is an isolated native Java service; no containers or global JVM configuration. Do not reboot, change firewall defaults or restart unrelated services.

## Paths and commands

- /opt/avatar2d/releases/TIMESTAMP-HASH: immutable per-release files; current/previous symlinks.
- /etc/avatar2d: private database configuration and ownership marker.
- /var/lib/avatar2d: writable application state directory.
- /var/log/avatar2d: console, rolling application and per-user logs.
- /var/backups/avatar2d: root-only database/config backups; no automatic deletion.
- Dedicated nologin avatar2d user; avatar2d.service.

```sh
sudo systemctl start avatar2d
sudo systemctl stop avatar2d
sudo systemctl restart avatar2d
systemctl status avatar2d --no-pager
sudo tail -n 100 /var/log/avatar2d/console.log
sudo /opt/avatar2d/current/scripts/healthcheck.sh
sudo /opt/avatar2d/current/scripts/backup.sh
sudo /opt/avatar2d/current/scripts/rollback.sh
```

Deployment script takes one staged release directory, detects Tailscale address, caps sessions20, heap128–512MiB, cgroup memory768MiB and tasks256. This is an initial budget, not verified capacity. Healthcheck includes actual protocol handshake and DB table counts when run as root. Restart-on-failure15s, burst3/120s. Startup failure exit1 allows systemd to detect faults. SIGTERM invokes original player-save shutdown flow; forced timeout/kill can lose unsaved changes.

Application file logs:5MiB active plus4 backups. Console/text logs: daily rotation, maxsize5MiB evaluated on each logrotate invocation,4 compressed backups. This is not an instantaneous disk cap. Install supplied logrotate config; inspect retention and free space operationally.

## Backup, restore and rollback

backup.sh stops only Avatar for a consistent player snapshot, dumps avatar_2x with single transaction, validates gzip, records release and DB config, then restarts if previously active. Backups contain secrets; keep mode0700/0600. No destructive restore script is supplied.

For restore: stop Avatar; make a new backup; restore dump into a NEW scratch schema first and verify tables/row counts. Restoring over the live schema is destructive and requires explicit approval. Do not import a backup blindly into an existing schema. A fresh target must be provisioned and runtime configuration deliberately pointed at it. Restore has not been tested yet.

rollback.sh selects previous release after making backup; preserves DB/player state and additive compatibility tables. First deployment has no previous release, so rollback script refuses. To withdraw initial deployment, stop/disable only avatar2d; retain data and files for recovery. No automatic package/user/schema deletion.
