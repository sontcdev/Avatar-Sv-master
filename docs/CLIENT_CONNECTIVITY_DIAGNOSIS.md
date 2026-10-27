# Client connectivity diagnosis — Milestone 2.1

## Findings

WORKING (regression tested): boss/bot sessions now use an internal actor session with no TCP connection, outbound queue or player logout persistence. Previously `ServerManager.run` spawned 14 bosses before accepting players; `Boss.createSession` connected each to the player listener. With staging max clients 10, these unauthenticated self-connections could occupy all slots until the 120-second read timeout. A test creating 14 bosses against a real listening socket failed before the fix and passes after it; the listener receives no boss connections. No increase to max clients was used. The offline bot factory had the same hardcoded-loopback pattern and now shares the internal factory.

WORKING (regression tested): initial city context 8 no longer unconditionally calls `zone.leave` when zone is null. The error was captured from authenticated client 71 (`-1`, payload 08) and reproduced by `initialCityHandlerWorksBeforeJoiningAnyZone` before adding the null guard.

PARTIAL: real-client reconnect. Same running MIDlet on emulator 5554 authenticated again after staging JVM replacement, selected Bảo Bình, entered city and then park (authenticated `-1` payload 09). Fresh MIDlet on emulator 5556 also authenticated during the investigation. Complete gameplay/restart persistence acceptance is tracked separately; neither login nor two visible characters proves chat/friends/attendance/farm completion.

## Layer checks

ADB 5037 already belonged to the user's environment (PID 68690); it was not killed or replaced. No listener 5038 was needed. Explicit serials 5554/5556 and explicit `-P5037` avoid cross-device actions. Both devices had `reverse tcp:19128 tcp:19128`. Emulator 5556 later disappeared across an interrupted session; only the owned AvatarMilestone2 AVD was relaunched with its existing data, no wipe. This does not establish why its process exited.

Owned SSH forwarding PID 70683 maps local 19128→remote 127.0.0.1:19129 and local 13317→remote13317. After the boss fix, a direct local handshake `e5 00 00` returned `e5 00 18 17 ...`, a valid command-27 key response. Thus this route and final server handshake worked; no blanket attribution to ADB is justified. Server listens only loopback 19129. Cached MIDlet cities differ: choose Xứ Sở Thần Tiên/Bảo Bình (19128); default 14444 and Sư Tử 18128 do not use this tunnel. Do not erase RMS to hide a reconnect defect.

## Repeatable isolated startup / teardown

1. Verify SSH host, `/home/sontc/avatar2d-gameplay-stage` ownership, private config: game bind 127.0.0.1/19129/max 10, DB 127.0.0.1/13317/catalog avatar2d_gameplay_stage/pool4/min1. Refuse production paths/endpoints. Record original production MainPID/NRestarts read-only. Do not use deploy scripts.
2. Start the existing user-owned MariaDB datadir only if no matching instance/listener is running; command uses socket mysql.sock, pid-file mysql.pid, log mysql.log, bind 127.0.0.1, port13317, buffer32M/max6/performance-schemaOFF. Never initialize or overwrite an existing datadir. Verify its PID cwd/cmdline and port.
3. Build with Corretto17, run default and separate opt-in test groups using their exact test DBs; fixtures are destructive within those test DBs. Hash JAR locally and remotely after copying to staging. Compile GameplayProbe with that JAR. Launch `java -Xms32m -Xmx128m -XX:ActiveProcessorCount=2 -Davatar.protocol.trace=true -cp m21-server.jar:. GameplayProbe` with cwd staging, private log and recorded PID. Do not set a sales catalog. Record `/proc/PID/cmdline`, cwd, sha256 and actual listener. Trace contains synthetic chat; never enable on production.
4. Inspect current adb servers/devices/reverses and local tunnel listeners before creating anything. Reuse a verified owned tunnel; otherwise start `ssh -oExitOnForwardFailure=yes -fN -L13317:127.0.0.1:13317 -L19128:127.0.0.1:19129 homelab` and record its PID. For each explicit serial, inspect reverse then add only missing 19128 mapping. Keep original mappings for restoration.
5. Use existing Avatar Stage installation; for a second owned AVD use AvatarMilestone2/port5556 with `-no-window -no-audio -no-snapshot -memory1024 -cores2`. Never wipe 5554 or reset the shared ADB server. Verify boot completion, then launch J2ME Loader and select the stage MIDlet and correct city. Capture before/after UI plus authenticated packets and DB snapshots.
6. For restart, verify PID cwd and cmdline exactly match staging; TERM only that PID, await exit, hash/start the same JAR again, record new PID. Test same MIDlet first; then independently test fresh launch. Preserve failures and timing.
7. Teardown: close only test clients/MIDlets, stop the verified staging JVM and owned DB PID with TERM and await exit. Remove only reverses introduced by this run, close only recorded owned SSH PID and stop only owned 5556 emulator. Preserve original 5554 and ADB 5037. Keep private DB/log/artifacts; no `pkill java`, `killall`, production systemctl mutations or deletion of existing databases.

Production was observed at MainPID 313665/NRestarts 0 during this work; observation does not constitute production backup readiness.

## Final artifact restart observed

The final package was reinstalled and restarted as PID 327395, hash `9ef00532c343e4dfd44db8aace5fcf67702b70e913a95f39c3150fedb8106008`. Same MIDlet 71 displayed disconnect, then authenticated again through its cached city selector and entered map 9/zone 0. See the two screenshots and runtime identity in [evidence](evidence/milestone-2-1/). This proves that restart/relogin path once; full feature persistence and sustained reconnect acceptance remain PARTIAL. Both clients also disconnected during idle/interrupted work. The listener configures a 120-second socket read timeout; the final cause of each disconnect was not captured and must not be attributed solely to that timeout.

## Milestone 2.2 follow-up

M2.2 captured authenticated map9/23 and graceful-restart reconnect. NPC850/map25 navigation remains blocked; screenshots, raw coordinate mapping and exact manual attendance acceptance/capture procedure are supplied. See [current report](MILESTONE_2_2_REPORT.md) and [client evidence](MILESTONE_2_2_E2E_EVIDENCE.md). Historical failures remain evidence of the earlier artifact.
