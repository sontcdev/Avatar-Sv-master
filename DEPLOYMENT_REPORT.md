# Avatar 2D deployment report — 2026-10-27

**Status: private server and playable Android emulator client verified, including character creation, park movement and character persistence after reconnect. Physical phone and broader gameplay remain untested.**

Repository: https://github.com/okebanei/Avatar-Sv-master
Upstream commit: `d8d5f26688465724d9955dc5045e9545a0dba91b`.
Deployed base commit: `d8d5f26688465724d9955dc5045e9545a0dba91b` plus local repairs, identified by the server JAR SHA256 below. Workspace contains uncommitted repairs on that upstream commit; no commit/push performed.
Local workspace: `/Volumes/sontc/sources/others/avatar2d`.

## Current public APK deployment — supersedes private-only instructions below

Signed standalone APK: https://avatar.memora.io.vn/Avatar-Homelab.apk (package `vn.io.memora.avatar`, version 2.5.8). Build 102 starts in sensor landscape with an adaptive canvas filling the available display (camera cutout safe area retained). The legacy MIDlet fails to resize reliably mid-session, so live portrait/landscape switching is disabled. Existing account/RMS data survives upgrades; display and virtual-key layout migrate once. No separate loader or Tailscale is required. Release rebuilt successfully through `android/build-apk.sh`; v1/v2 signatures verified. Published APK SHA256: `98bf9ccfb62f6ef7e9e4f1a16953ba93bdbcd1760da422c8ed131d41c6406d86`.

Existing Cloudflare tunnel retains its prior API/upload routes and adds only Avatar hostname to loopback `127.0.0.1:18728`. The dedicated user bridge forwards binary WSS `/game` to the private game socket. Public HTTPS download was retrieved and its hash compared; full WSS handshake, login and park movement passed on Android 17 arm64 emulator. Evidence: `docs/evidence/android-apk-public-world.png`, `android-apk-public-moved.png`. Bridge tests cover byte ordering, binary-only input, per-IP limit and cleanup. Game service remains active with zero restarts; existing checked workloads remain active.

Dedicated test password was rotated through authenticated CHANGE_PASS after it appeared in an operator input screenshot. Current credentials are in `/home/sontc/avatar2d-staging/test-account.txt` (owner-only) and local `.secrets/test-account.txt`; the root `/etc/avatar2d/test-account-avatartest.txt` copy is now stale. Do not use or rerun the older password-repair script until that root copy is synchronized. APK suppresses legacy credential stdout. Temporary bridge packet-header diagnostics were removed.

Limits: six public sessions total, two per public IP; physical phones and sustained multiplayer load untested. Android 17 runs inherited native libraries in 16 KiB compatibility mode. See `android/README.md` for build and operations.

## Verified results

- Baseline source compiles on local Java17, but original main manifest is wrong.
- Repaired Java17 Maven clean package succeeds; three JUnit tests pass with real sockets (disconnect cleanup, protocol-key negotiation and Boss listener selection).
- Missing DB config produces exit1 with contextual error.
- API9 compilation passes before final reload4j update; deployment targets Java17. No Java9 runtime test.
- Resource archive path checks pass;48,411 entries extracted, approximately192MiB, full res tree preserved.
- Original J2ME client preserved; separate private copy passes ZIP CRC/manifest/javap structural checks. Installed successfully in J2ME Loader 1.8.2 on Android emulator; gameplay pending.
- Homelab reachable over SSH/Tailscale100.112.0.62. Ubuntu24.04.5; Java17.0.20.1 and MariaDB10.11.14 installed after user ran privileged setup. MariaDB listener127.0.0.1:3306.
- Recheck confirms avatar2d.service is loaded; repaired deployment now active, dedicated OS user/directories exist. Earlier not-found observation was taken before setup completed. Clean release staged remotely; JAR SHA256 matches local artifact.
- No public listener, domain, router mapping, firewall rewrite, reboot or unrelated service restart performed by agent.

## Repairs and files

POM: correct main class, explicit Java17 compiler, reload4j1.2.27, remove unused Commons IO, JUnit tests. DbManager/database.properties: correct password key, bounded10/2 pool, external secret config, actionable failures. ServerManager/config.properties: private bind,20-session limit, synchronization/null guards, correct initialization order. Session: concurrent bounded queue, idempotent cleanup and pre-handshake EOF handling. Avatar: nonzero fatal startup exit. Npc: chat-list publication. log4j.properties/deploy logrotate: restricted log growth. database/private-compat.sql: additive missing tables. scripts/: setup/deploy/health/backup/rollback/test-account/client preparation/protocol smoke. deploy/: systemd/logrotate. docs/: phase reports and raw local test evidence.

## Proposed deployment and operation

Native avatar2d.service under dedicated nologin user. /opt/avatar2d/releases + current/previous; /etc/avatar2d/database.properties outside source; /var/lib/avatar2d; /var/log/avatar2d; root-only /var/backups/avatar2d.

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

These commands apply only after successful provisioning/deployment. First install has no previous-release rollback; stop/disable Avatar and retain data instead. No destructive database restore/delete automated. Backup restoration must first be rehearsed against a new scratch schema; replacing live data requires explicit approval.

## Connection and playing

Intended endpoint: dynamically discovered Tailscale IPv4, observed100.112.0.62:19128. Server defaults to loopback until deployment sets the private IP. Access depends on existing tailnet policy; phone connectivity pending.

1. Privileged provisioning is complete. Preserve the dedicated schema and credentials.
2. Deploy the staged, verified release using reviewed scripts/deploy.sh; verify actual SQL counts and startup logs.
3. Create non-admin test account using scripts/create-test-account.py; retrieve credentials locally from its root-only file. Registration is not implemented.
4. On Android enable Tailscale on same authorized tailnet; install official J2ME Loader; import local artifacts/Avatar258-private.jar into a fresh profile.
5. Select Vietnamese `Xu So Than Tien` or `Thanh Pho Bao Binh` (confirmed19128 entries); login/create character, enter map, move/chat/interact with NPC/shop.
6. Confirm inventory/state persists after logout/login and normal service restart. Record actual evidence.

See docs/05_CLIENT_SETUP.md for exact client analysis and instructions. Do not use client update/payment/SMS features. No proprietary client/assets published.

## Capacity, limits and open issues

Initial JVM128–512MiB, memory ceiling768MiB, tasks256, pool10/2. No measured CCU, gameplay latency, GC, DB active load or disk bottleneck. Baseline3880MiB RAM,2874MiB available,99MiB swap used,239GiB disk available. Later2793MiB available/119MiB swap is a snapshot, not an attributable benchmark.

Missing upstream farmitems seed data; farm economy unverified. marry schema derived from callers but relationship flow untested. Legacy MD5 authentication, Connector/J8.0.27/protobuf3.11.4, game collection concurrency, NPC/boss timers and non-atomic player-save writes remain risks. No exhaustive vulnerability scan. No source/asset license grant found, so private noncommercial testing does not establish redistribution rights.

System baseline failed units0; user manager already has two unrelated failed units. Preserve those and all eight running user workloads; do not start on-demand crawler workers. Final post-deployment workload comparison remains pending.

Phase evidence and pending checks: docs/01_SOURCE_AUDIT.md through docs/07_PERFORMANCE_REPORT.md. No passwords or private keys included.

## Artifact identity

Staged server JAR SHA256: `c9f8f06c1fc8c125bef066bf8c0e9dd0af938dc46ca5934d407f1b5c3f2b68ce`.
Original client SHA256: `34bda0739fc7854f1f8b925a18f98dbdfd99424b852a7140c1f2c2042b64314e`.
Private client SHA256: `516b26135a326e37de7ecc7546af444d45e268947a493c21a2c33287f8c50851`.

## First deployment failure and repair

Actual Ubuntu startup failed: map_item lacked map_id, and Boss.createSession hard-coded localhost:19128 while the listener used Tailscale. Added nullable map_id without assigning the ambiguous seed row; Boss now connects to the actual listener address/port with a timeout. Regression test and Java17 package pass. Repaired release staged; privileged redeployment pending. Repaired service is active with NRestarts=0; actual Tailscale handshake passes. Root verification stopped because ss reports an IPv4-mapped IPv6 endpoint. Updated healthcheck accepts that exact endpoint while rejecting wildcard/loopback. Test account, restart and gameplay checks still pending.

## Successful privileged verification

User ran finish-verification.sh: backup /var/backups/avatar2d/20261027T095440-306559; restart passed; -27 handshake passed twice; 28 NPC, 5496 items, 34 tables; test account normal/active count1; bundled admin/ad active count0. MariaDB10.11.14 buffer pool128MiB, slow queries0, connections4/running1 at snapshot. JVM MemoryCurrent102584320 bytes (~97.8MiB),87tasks,NRestarts0 after restart. No failed system services. Evidence: docs/evidence/homelab-finish-verification.log.

Client authentication follow-up: actual MIDlet reaches private server and receives login failure. In-memory comparison confirmed punctuation is dropped by client text input (20 intended /19 sent). Dedicated test-password correction staged; no auth bypass or admin account used. Bundled MIDlet logs login fields to Android logcat: use a unique test password; raw client logs are not stored as evidence.

## Final actual-client result

Password repair applied. Real Avatar2.5.8 in J2ME Loader1.8.2 logged in as the normal account, created a male avatar, entered Công viên and moved. After a disconnect, reconnect loaded the same character without recreating it. See docs/06_GAMEPLAY_VERIFICATION.md and its screenshots for authoritative current results; earlier pending notes above describe intermediate stages. The client disconnected during a pause; cause unknown, service remained active with NRestarts=0. Final workload/listener snapshot is docs/evidence/homelab-final-runtime.txt. No physical phone, multi-player, economy or CCU claim.

Final release reconnect after password rotation: PASS, existing AVATARTEST character re-entered a world map through public WSS. Evidence: `docs/evidence/android-apk-public-reconnect.png` (repository-relative). The final signed APK was installed as an upgrade before this check.

Display follow-up: installed the signed update over the existing release on the Android emulator, cold-launched from a portrait device posture into landscape, logged in through the public tunnel, entered an area, held the right arrow to move, and opened the in-game menu. Evidence: `docs/evidence/android-apk-landscape-world.png` and `android-apk-landscape-moved.png`. Socket connection interception covers every original host/port; remaining bundled HTTP URLs are Teamobi terms/FAQ/update links and disabled server-list fetching, not gameplay sockets. The user-reported failed action remains unspecified; NPC/shop/minigame completeness and physical-device controls have not been established.


### Gameplay repair batch deployed — 2026-10-27

Backend fixes for fruit/crop harvest, farm persistence, menu/NPC feedback, house chest packets and nonblocking fishing are built and staged on homelab. Java 17 package and 21 ordinary tests passed; one additional isolated MariaDB integration test passed. User completed privileged deployment; backup `/var/backups/avatar2d/20261027T120131-313570`. Follow-up SSH service checks report active/running with NRestarts=0, and the public WSS handshake passes. Android login, farm entry and the new unsupported-registration feedback were observed. A client disconnect occurred during manual checking; reconnect succeeded and the service did not restart. Actual Android fruit harvest/repeat/reload verification remains pending. APK build 102 remains compatible. Details and scope limits are in `docs/06_GAMEPLAY_VERIFICATION.md`.
