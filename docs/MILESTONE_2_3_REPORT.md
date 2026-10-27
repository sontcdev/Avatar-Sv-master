# Milestone 2.3 — real client verification, 2026-10-28

## Provenance and acceptance

Baseline: `af048f6776ee630f78471d8a0b64b7aea867669b`, clean migration worktree, branch `codex/gameplay-migration`. This report is committed together with its implementation; the delivery commit is the commit containing this file (use `git log -1 --format=%H -- docs/MILESTONE_2_3_REPORT.md`). No production cutover. Milestone acceptance is **PARTIAL**.

Final Java17 JAR SHA256: `7fbfa1800e38496873f9a719108512a6bfc0dfbb5613f012603c839f5c1a57c7`. Local build and remote `m23-social-final.jar` matched. Actual final staging JVM336538 ran this artifact; JVM337656 repeated deletion after restart. Earlier attendance and chat evidence used intermediate SHA256 `8189d380223b94ac18563954c031e56198f78cf64014654ed15336f9934c86c9`. The final artifact additionally fixes friend custom-list field order; attendance/chat were not repeated in full on that artifact. Do not attribute all screenshots to the final artifact.

| Flow | Status | Evidence and limit |
|---|---|---|
| A: natural world navigation to NPC850 | PARTIAL | Maps9/23 reached historically; legitimate map25 route not established |
| B: NPC850 UI attendance with initial-position fixture | WORKING within fixture scope | Visible NPC, actual -61/-59/-9, reward, repeat denial, restart denial, matching DB |
| Attendance overall | PARTIAL | A remains unverified; B uses intermediate artifact |
| Private chat both directions and reconnect repeat | WORKING on intermediate artifact | Real clients71/92, correct sender IDs/names and recognizable A/B messages |
| Friend invite/accept | WORKING on intermediate artifact | Actual UI and canonical DB row |
| Friend list persistence through JVM restart | WORKING | Final artifact displays stage92 Online after restart, then Offline after disconnect |
| Delete friendship | WORKING | Final artifact actual -81, empty UI and zero DB rows; deletion restart repeat not performed |
| Friendship overall | PARTIAL | Required flows captured across two artifact versions; deletion restart repeat not performed |
| Online/offline friend farm visits | PARTIAL | Actual -81 reaches farm handler, response61 owner92; client loading/EOF blocks complete rendered-farm acceptance |
| Farm permission checks from real UI | UNVERIFIED | Could not complete farm UI loading; integration protections remain tested |
| Reconnect | WORKING in observed chat/list scenarios | Both players reauthenticated after graceful staging JVM restart; no guarantee for every game context |
| C: unit/DB packet simulation | WORKING in tested scope | 60 unique tests, separate from UI evidence |
| Native63 | PARTIAL; live BLOCKED/OFF | Replay-after-replenishment characterization retained, no approved production prices |
| Production backup readiness | PARTIAL | Previous administrator procedure unchanged; no production backup operation in M2.3 |

## Regression and changes

Java toolchain: Corretto17.0.18 at `/Volumes/sontc/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home`.

Baseline and delivered build passed **60 unique tests: 33 unit +27 opt-in MariaDB integration**, no duplicate counting for reruns. Final Surefire manifest has60 PASS,0 FAIL,0 SKIP: [manifest](evidence/milestone-2-3/test-manifest.tsv). `mvn -q package` runs33 and skips27 by design. Additional runs select `FarmSalePersistenceTest,FriendshipPersistenceTest` with `-Davatar.test.isolatedSalesDb=true` (25), and `FarmPersistenceTest,AttendancePersistenceTest` with `-Davatar.test.isolatedFarmDb=true` (2), using private `avatar.database.config` files. Disposable catalogs assert `avatar2d_sales_test` / `avatar2d_starfruit_test` on the isolated instance, never the staging gameplay catalog. Raw run logs remain private under `/tmp/avatar2d-m23/delivered-{package,sales,farm}.log`; rejected synthetic snapshots from fault tests are expected.

Changes are limited to:

- Correct native -81 parser order to `int listId, byte page, short index, byte action`. Actual UI packet `0000000101000002` previously failed because action/page were reversed. A targeted existing DB test first failed, then passed with literal captured bytes, owner92 assertion and forged-page rejection. Existing deletion passed before because page/action were both1. Client packet format unchanged.
- Extend existing defaultOFF authenticated diagnostic whitelist with -9,50,54 so UI dialog and navigation evidence can be recorded. Trace prefixes QUEUED mean enqueued, not proof of client rendering. Payload output is capped; large image packets are truncated.
- Standalone account71-only staging initial-position fixture outside the application JAR. Exact cwd/ports/catalog/live JDBC URL/sales guards; original live positions captured and restored. [Fixture self-check](evidence/milestone-2-3/fixture-self-check.txt). No new test methods: targeted coverage extended existing tests.

[Navigation/fixture evidence](NPC850_NAVIGATION_ANALYSIS.md), [social evidence](CLIENT_SOCIAL_E2E_EVIDENCE.md).

## Persistence and isolation

Attendance atomically updates chests/scores/date and increments player revision under expected-revision predicate in `User`; accepted claim revision5→6, repeat unchanged. Post-restart saves increase revisions without adding reward. Social parser changes no player/farm writer: friendship accept/delete affect only canonical friendship rows. Visiting reads the selected owner; subsequent logout uses existing revision-guarded atomic persistence. [Before visit/restart](evidence/milestone-2-3/db-farm-before-social-restart.tsv) and [after visits](evidence/milestone-2-3/db-after-visits.tsv) retain account71's195 harvested item215 in **nongsandacbiet**, tree0 and anchor1793123779887; account92 stock remains empty and tree180. Revision10→13 for71 and5→6 for92 includes graceful saves. No stale snapshot auto-merge or persistence guard bypass was added. Integration tests retain stale-write, uncertain-commit, concurrency and replay characterizations.

Staging used loopback game19129 and isolated MariaDB13317 (32MiB buffer pool,max6connections), SSH forwards local19128→remote19129 and local13317→remote13317, adb reverse19128 on both real Android J2ME-hosted Avatar2.5.8 clients. These were active authenticated game clients, not socket-count load evidence. No load test was performed because functional acceptance is incomplete.

Production read-only identity check: systemd `avatar2d` MainPID313665,NRestarts0 and MariaDB302726 remained the same. No production service restart, SQL write, migration or config change. Synthetic staging rewards/social changes are intentional test data. Staging fixture restoration markers/logs are retained privately; fixture is absent from final runtime. Original dirty checkout preserved separately.

## Remaining work

Establish and verify the natural map25 route; decode the native farm-loading failure using actual client parser/stack before changing server payloads; then rerun online/offline rendered owner identity and forbidden harvest actions. Repeat acceptance on one final artifact, including deletion persistence after restart. Keep salesOFF and no load/cutover until gates pass. Production backup approval/restore readiness remains governed by [existing procedure](PRODUCTION_BACKUP_READINESS.md).

## Cleanup and preservation

At completion, owned staging JVM337656 stopped gracefully, isolated MariaDB13317 shut down through its private socket; neither19129 nor13317 remained listening. Owned emulator5556 stopped, owned tunnel16897 removed and newly added adb reverse19128 removed from shared5554. Shared emulator5554/ADB server preserved. Final production check still MainPID313665,NRestarts0, MariaDB302726. Original checkout tracked binary-diff SHA256 remains `c31dac0e498aab840a22f09b499a101ec76b8fff66300b21bfde936f8c66ddbf`. Generated test null_log moved to private `/tmp/avatar2d-m23/generated-null_log.txt`; raw authentication logs and conflict snapshots excluded from commit.
