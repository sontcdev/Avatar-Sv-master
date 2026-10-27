# Source audit — 2026-10-27

Upstream: https://github.com/okebanei/Avatar-Sv-master
Commit: `d8d5f26688465724d9955dc5045e9545a0dba91b`.
Workspace: `/Volumes/sontc/sources/others/avatar2d`.

## Confirmed findings

| Severity | Location | Evidence and treatment |
|---|---|---|
| High | pom.xml | Manifest points at nonexistent `com.kitakeyos.MainClass`; use `avatar.server.Avatar`. Conflicting Java 8 properties / Java 9 compiler options; source calls `List.of`. Baseline compiles on JDK17, API9 compilation passes; standardize deployed bytecode on Java17. |
| High | db/DbManager.java | Reads `passeword` although configuration supplies `password`. Fix spelling; close config stream; throw contextual failure instead of swallowing pool initialization errors. |
| High | network/Session.java, server/ServerManager.java | Unauthenticated disconnect dereferences `cl.user`; sessions start collector before services and list registration; cleanup skipped if disconnect throws. Fix ordering, null guard, finally cleanup, idempotent close, and pre-handshake EOF cleanup. |
| High | SQL users seed | Dump includes active privileged accounts with weak password hashes and personal/contact data. Disable seeded accounts and randomize hashes only in newly provisioned dedicated schema; never distribute dump publicly. |
| High | ServerManager.start | Original wildcard TCP bind exposes game on every interface. Default to loopback; deployment explicitly binds current Tailscale IPv4. No firewall/router changes. |
| Medium | Session.Sender | ArrayDeque accessed from multiple threads; unbounded output queue, no session ceiling, stalled pre-handshake peers. Use bounded concurrent deque, 20-session ceiling and 120-second socket timeout. Session ceiling is not measured player capacity. |
| Medium | log4j.properties, Utils.java | 500MB x100 backups and Windows-style path. 5MB x4 rolling backups plus active file; rotate console and per-user text files separately. Console/text rotation is scheduled, not a hard byte quota. |
| Medium | pom.xml | Log4j1 is EOL. Replace with API-compatible reload4j1.2.27. Remove unused Commons IO2.7 (no source imports). Connector/J8.0.27/protobuf3.11.4 remain legacy dependencies requiring follow-up security review; no clean bill of health claimed. |
| Medium | GameData/PartManager, SQL | Missing `farmitems` table. Add empty schema from actual consumed columns; no invented seed economy. Farm shop unavailable until authentic seed data is supplied. |
| Medium | User.loadData, GlobalHandler, ParkService | Missing `marry` table. Add schema matching idNam/idNu/level/perLevel callers, initially empty. Marriage flow not tested. |
| Medium | User.login | MD5 password protocol/storage and login locks persist across restart. Preserve compatibility; clear locks when exclusively starting this private instance. No public access; no authentication redesign. |
| Medium | UserManager, Zone, NPC/Boss/TaiXiu | Existing mutable game collections and dedicated NPC/boss threads remain. NPC chat list gets volatile publication. Broader synchronization and gameplay timers need measured multi-player verification. |
| Low | Avatar.getFile | Missing resources return null. Validate actual archive layout; handlers still vary in missing-resource behavior. |

## Startup and protocol trace

`Avatar.main → Avatar.start → ServerManager.init → DbManager.start → loadConfigFile → loadSettings → GameData/PartManager/FoodManager → maps/NPCs → reset players online → boards → ServerManager.start`.
TCP: signed command byte + unsigned 16-bit payload length; command90 has special outbound framing. Initial command `-27` negotiates a chained XOR key; later frames XOR command/header/body. Collector dispatches MessageHandler then game-specific handlers. Login `-2` reads UTF username/password/version, queries users using prepared statements and MD5, loads/creates players and initializes services. Registration `regMessage` reads credentials but inserts nothing. Character state saves on User.close; multi-table writes are not atomic.

Source searches found no active URL/HTTP client, shell exec, or ProcessBuilder in backend Java. This is a scoped static inspection, not a malware guarantee. J2ME client contains official update/FAQ URLs and SMS permissions; private copy disables remote server-list URLs, not every external feature. Do not use SMS/payment/update features.

Resources include `res/data`, `res/map`, `res/hd`, `res/medium`; preserve all, not just image folders. Client258 is MIDP2/CLDC1 with obfuscated Java classes. Unity archive is an extracted/decompiled project targeting Unity5.6.4f1, not a ready Android build.

No top-level license or asset permission grant was found in tracked files. Repository availability does not establish redistribution/modification rights. Keep artifacts private; seek rights-holder permission before distribution.

## External references

- [Apache Log4j1 EOL](https://logging.apache.org/log4j/2.x/migrate-from-log4j1.html)
- [reload4j](https://reload4j.qos.ch/)
- [Commons IO advisory: CVE-2024-47554](https://commons.apache.org/proper/commons-io/security.html). Removal avoids retaining an unused vulnerable component; exploitability was not established.
