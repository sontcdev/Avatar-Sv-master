# Public standalone APK verification

Android 17 arm64 emulator: signed release installed; public WSS login, existing character, Công viên entry and right-arrow movement/camera scroll passed. Evidence: `evidence/android-apk-public-world.png`, `evidence/android-apk-public-moved.png`. No separate loader import or emulator Tailscale connection used by this APK. Physical phone and full game systems remain untested.

Earlier private-client verification follows.

# Actual gameplay verification — 2026-10-27

Private server: 100.112.0.62:19128. Client: Avatar2.5.8 private JAR, J2ME Loader1.8.2, read-only Pixel_10_Pro Android17 arm64 emulator. Mac host reaches homelab through Tailscale. A physical phone VPN path is not tested.

| Check | Result |
|---|---|
| Java17 build / socket regression tests | PASS, 3 tests |
| Ubuntu service / private listener / protocol | PASS, NRestarts=0 |
| DB/resources | PASS, 34 tables, 28 NPC, 5496 items; real world rendered |
| Normal account login | PASS, avatartest after alphanumeric password repair |
| Character creation | PASS, male default avatar |
| Map entry / movement | PASS, Công viên, right-arrow movement and camera scroll |
| Reconnect character persistence | PASS, same account and appearance, no repeat creation screen |
| Backup / normal service restart | PASS before character creation; see privileged verification log |
| Character persistence across service restart | Not tested |
| Two real players / chat / economy / farm / minigames | Not tested |
| Physical Android / load capacity | Not tested |

Evidence: evidence/android-character-create.png, android-world-before-move.png, android-world-after-move.png, android-world-reconnect.png. Reconnect preserves character identity/appearance; no claim about position, inventory mutation or currency transactions. During a pause the client displayed Mất kết nối; reconnect worked and server remained active with zero restarts. Cause not established.

Account creation via admin script is required: upstream registration is unimplemented. No admin privileges or authentication bypass used. Passwords stay in restricted files; raw client logcat is excluded and cleared because the MIDlet logs login fields.

Final release reconnect after password rotation: PASS, existing AVATARTEST character re-entered a world map through public WSS. Evidence: `docs/evidence/android-apk-public-reconnect.png` (repository-relative). The final signed APK was installed as an upgrade before this check.


## Gameplay repair batch — 2026-10-27 (deployed; Android harvest verification pending)

Server JAR SHA-256: `c889aa1b3f991cf813e74ebce50d44f91ab74190fea890c4b6a42243b254818e`.
The APK remains build 102; this batch changes backend behavior only.

| Flow | Verified defect and repair |
| --- | --- |
| Starfruit | Command 85 was missing; farm always advertised 180 fruit and inventory fabricated 680. Harvest now moves the actual initial 180 into persisted stock, refreshes the farm/inventory, reports the result, and prevents repeat credit. No regrowth rule is invented. |
| Crop harvest | Previously cleared land without crediting stock and reduced yield modulo 100. Valid configured, mature crops now credit stock and save before acknowledgment; failed saves restore state. |
| Farm persistence | Fixed self-referential fertilizer/produce JSON arrays, lost special stock on reload, and parsing the existing `not_planted` timestamp sentinel. |
| Seed shop / planting | Validate absent seed/config, quantities and ownership; use selected currency, merge stock, reset planting time. PartManager now reads the same farmitems column names as GameData. Authentic farmitems seed rows are still absent. |
| Menus / NPCs | Reject invalid menu indexes, register current menu actions, clear old actions when opening legacy menus, execute menu actions once, and report unavailable menu leaves / NPC functions rather than silently ignoring clicks. Exit menu items still close quietly. |
| House inventory / purchases | Fix packet entry count after excluding item 40, initialize home chest, reject missing transfer sources, propagate SQL failures to client feedback, and charge furniture only after insert success. |
| Fishing | Preserve the 12–18-second bite delay using a scheduled task instead of blocking the packet collector; prevent overlapping pending casts and cancel on disconnect/finish. Failed casts no longer wait or generate bites. |
| Incomplete commands | Cooking, animal sale/harvest, starfruit upgrades, store upgrades, stealing and in-game registration now explicitly report unsupported status. They have not been implemented as complete gameplay features. |
| Errors | Shared action-failure reporting covers farm, home, park, avatar and casino dispatch; casino forwards common commands to the shared handler. Unrecognized background probes are not turned into popup spam. |

Validation: Java 17 Maven package passed 21 ordinary tests. The opt-in persistence test was skipped by Maven and then passed separately against a disposable MariaDB on homelab loopback port 19307 (22 successful tests total). The isolated test covers additive migration repeat application, both farmitem readers, real JDBC crop/starfruit save/reload, no duplicate harvest after reload, fertilizer/produce/special stock, empty land timestamps, and an injected furniture SQL failure without currency loss. The disposable MariaDB was stopped after the test; the production database was not used.

Regression tests reproduced the old missing responses, negative-index menu exception, malformed chest packet, packet-collector fishing delay, and missing crop credit before the corresponding repairs. Current public WSS handshake still passes. Evidence: `artifacts/gameplay-build.log`, `artifacts/gameplay-mariadb-test.log`, and `target/surefire-reports/` (local build outputs).

Deployment: user ran the root deploy script successfully; backup `/var/backups/avatar2d/20261027T120131-313570`, service started at 12:01:37 UTC. Saved output: `evidence/gameplay-deploy.log`. Follow-up SSH checks confirmed active/running and NRestarts=0; full public WSS handshake passed. Installed JAR checksum could not be read by the SSH user because of directory permissions.

Android follow-up: APK build 102 logged in with the dedicated normal account through public WSS, entered the farm, and received the new explicit unsupported-registration dialog. During the manual farm check, the client displayed a disconnect; the service remained active with zero restarts and the same account reconnected successfully. Starfruit harvest, repeat harvest and persistence across reconnect have NOT yet been verified in the actual Android UI. The server socket has a 120-second read timeout, but the cause of this particular disconnect was not established. Full minigame/economy coverage and complete implementation of upstream placeholders are not established by this batch.


## Android keypad toggle — build 103

Signed APK SHA-256: `a53c490d7fc1f34b3441171e2d5f041e1d187b31a36fdf6844d1f76f71303630`. Build passed and installed as an update on the Android emulator. Verified hidden default, show/hide toggle and hidden preference after relaunch/update. Evidence: `docs/evidence/android-keyboard-hidden.png`, `docs/evidence/android-keyboard-visible.png`. Public APK download checksum matches the local build. Physical phone verification remains pending.
