# Milestone 3.8: custom NPC 645 English quiz

**CUSTOM_NPC_645_QUIZ: IMPLEMENTED / UNIT AND ISOLATED DB VERIFIED / NATIVE UNVERIFIED.** This is newly designed custom gameplay, not restoration of the original NPC. **ORIGINAL_NPC_645_RECOVERY: BLOCKED**, unchanged from milestone 3.7.

Starting commit: `e5e9dd5f01fa4a145cc18a2884a5bb183fa52f7e`. The commit containing this report and code is identified by `git log -1 --format=%H -- docs/MILESTONE_3_8_CUSTOM_NPC_REPORT.md`. Final shaded JAR SHA-256: `d037c013694502eceb48f5f0ebec86b6f5699f98ede97f3bc189a50facbe8a5e`. The final artifact was not deployed or run on a staging server.

Enable only on isolated staging with JVM option `-Davatar.customEnglishQuizV1=true`; absent or false is OFF. No repository configuration enables it. Native63 sales remain OFF and BUY_ANIMAL 71 remains blocked. No production change, deploy, push or merge was made.

`CustomEnglishQuiz` is session-owned and keeps step, score, context and menu sequence in memory. NPC 645 communication opens the custom menu only when enabled; -59 selection is routed before the generic menu handler. It checks connected authenticated session, session ownership, NPC ID and presence, map 6, same zone and distance within 55. It offers the approved three questions and answers, exit at each step, and the final X/3 message. Entering another NPC/menu, movement away, leaving the zone, closing the session and completion clear the state. No answer, result, currency, inventory or player score is persisted or modified by the quiz.

Each displayed menu uses a different signed-byte-compatible ID within the session, so old-step and duplicate callbacks with different IDs cannot advance it. After 128 menus the feature requests reconnect rather than reusing an ID. A callback from a previous connection or another context with the same NPC, menu ID and selection cannot always be distinguished by this protocol; no perfect replay protection is claimed. The NPC request has no custom sequence token. The existing client 2.5.8 `ce` -59 decoder statically reads menu labels, optional short IDs, NPC name, NPC text and flags, matching the existing `openUIMenu` serialization; this is not runtime acceptance.

## Verification

Corretto 17 `mvn -B clean package`, using the target `res` assets, discovered 94 tests (89 baseline plus 5 quiz tests), with 59 passing and 35 opt-in cases skipped. Separate isolated DB runs cover 2 farm/attendance cases and 31 sales/cooking/friendship cases, plus the quiz DB case; the asset-dependent catalog test was also run with the target catalog. These runs cover **94 distinct passing tests**, without adding overlapping reruns. The quiz tests exercise the three question packets, mixed correct/incorrect scoring, exit, duplicate/stale/out-of-order callbacks, wrong NPC and zone, distance, disabled flag, session cleanup/reconnect, separate sessions, and unchanged synthetic player/farm DB snapshots and in-memory money/inventory. They do not establish client rendering. `python3 scripts/test-tools.py` passed.

An intermediate combined DB run failed because the quiz snapshot test ran before the sales fixture created tables. The fixture was changed to create its own minimal tables and rows if absent; the final runs use the changed source. Expected SQL exceptions from negative persistence tests appear in logs but do not indicate test failures. Local logs: `/private/tmp/avatar2d-m38-confirm-{build,farm,sales,assets}.log`.

## Native and staging

Native acceptance is **UNVERIFIED**. Device diagnosis: host `kern.hv_support=1`; Android emulator 37.1.11 arm64 has hypervisor and JIT entitlements. The isolated existing `AvatarM37` AVD was retried on port 5562 with `-accel off`, without creating another AVD or using shared emulator 5554. It repeatedly logged `qemu_mprotect__osdep: mprotect failed: Permission denied` and remained offline in ADB; the cause of the permission failure is not established. No actual Avatar 2.5.8 menu, question, exit, scoring, packet capture, or staging DB before/after was observed. Logs: `/private/tmp/avatar2d-m38-emulator.log`.

The execution-owned isolated MariaDB and SSH tunnel were used only for test databases. Both databases were backed up before tests (`m38-tests-before.sql`, SHA-256 `198e9ea9acd9ee3a089b87520a87b80f5e4b45d7f41df226b4c49f8149e166e0`) and restored afterward. Execution-owned MariaDB, SSH tunnel and emulator were stopped; the temporary resource symlink was removed. Existing `logs/null_log.txt` was preserved.
