# Milestone 2.2 report — 2026-10-28

## Result

**P0 stale-session persistence fixed within the writer scope below. P0 attendance real-client acceptance remains PARTIAL.** No gameplay flow is promoted to WORKING on unit/integration tests alone. Sales remain default OFF and operationally BLOCKED. No production cutover or load test.

Started from clean `98d85d7acd5940fb9e6d1f20f0f5edf9a98d3363` on `codex/gameplay-migration`. Preserved the original dirty checkout and existing emulator data. Baseline47 unique tests passed (32 unit +15 DB). Corretto17.0.18 is the build/runtime-compatible toolchain.

Final artifact: `target/Avatar2D-1.0-SNAPSHOT.jar`, SHA256 **18df157280dc1ee2915a4a17442a6282c31d5eb3edbbbc385e119016c8d5750a**. The staged copy is `m22-server-final.jar`; checksum/runtime records are in the evidence directory. Earlier map navigation/restart evidence used intermediate artifacts and is explicitly labelled.

## Fixed vulnerabilities

- Stale farm autosave/logout restoring stock after committed sale/harvest: farm CAS revision rejects the old snapshot.
- Stale logout overwriting committed chest/score even with sales OFF: player CAS revision plus one player/farm transaction prevents partial overwrite.
- Direct legacy TopPhao counter writes failing to invalidate old snapshots: conditional revision update; overflow checked before DB write.
- Duplicate login unlocking/notifying/kicking the current owner: incoming login rejected, owner retained in both existing branches.
- Persistence exception skipping session cleanup: cleanup in finally.
- Lost commit acknowledgement permitting a later stale save: fail closed until reconnect; rejected logout snapshot retained privately for manual reconciliation.
- Review-discovered JDBC close failure after attendance/counter commit: RED reproduced both paths; outer SQL failure marks uncertain so logout cannot overwrite committed values before RAM synchronization.

See [root cause, caller scope, reconciliation and limitations](STALE_SESSION_ROOT_CAUSE.md). Schema is additive: player/farm BIGINT revisions DEFAULT0. It was applied twice only on stopped isolated staging, after preserving a private pre-DDL backup. All writers must use this protocol; old binaries or external SQL without revision increments can bypass it. This is not a complete rewrite of legacy reward/session ownership.

## Reproduced but not fixed

**Native63 replay after newly harvested stock remains exploitable when synthetic test sales are enabled.** Product short alone has no durable operation identity; existing characterization remains in the test suite. Depleted-stock duplicate rejection is not durable replay safety. No client extension was introduced, and no production prices were invented or approved. Live sales stay OFF/BLOCKED.

Legacy RAM-only rewards can still be lost on process death before save. Counter increment and legacy RAM currency are not claimed as a single durable reward transaction. Conflicts retain rejected proposals for operator reconciliation; no automatic full-snapshot merge/import. Journal filesystem failure is explicit FATAL, and hard-power-loss journal durability is unverified.

## Verification

**60 distinct passing tests: 33 unit +27 isolated DB (24 sale/persistence +1 friendship +1 farm +1 attendance)**. Net13 tests above baseline47. Default package run discovers60, skips27 opt-in DB tests and passes33. Then the DB profiles execute the27. Counts are distinct methods, not summed reruns. Final XML-derived counts and logs are in [evidence/milestone-2-2](evidence/milestone-2-2/).

RED-before-fix evidence covers stale farm snapshot, duplicate lock/owner, rejected snapshot retention, direct counter revision, cleanup exception and both resource-close failures. DB fault injection executes real queries/transactions but simulates JDBC boundary failures; it is not an OS crash or network partition test. Concurrent writers have one DB winner; rejected proposals remain in RAM/journal. Fresh User reloads exercise committed truth.

An earlier overlapping fixture run and a later farm-profile rerun without clearing disposable tables produced fixture setup errors; neither is acceptance evidence. Final farm profile was rerun after dropping only its three disposable fixture tables. The independently isolated staging gameplay DB and production DB were not dropped or reset. Scoped independent code review found the resource-close issue; it was reproduced, fixed and reviewed again with no further Important finding in that review scope.

## Real-client status

| Feature | Classification | Actual evidence / gap |
|---|---|---|
| Attendance NPC850 | PARTIAL | Authenticated71/maps9/23 captured; NPC map25/menu/claim not reached. Ticket593/+20/date/double-claim/reward restart not verified on client. |
| Private chat71→92 | PARTIAL | Not attempted because attendance gate did not pass; Admin messages excluded. |
| Friend invitation/accept/delete/list | PARTIAL | Existing implementation +DB coverage; no new real-client acceptance, chat gate not passed. |
| Farm visits online/offline | PARTIAL | No new real-client acceptance, gates not passed. |
| Reconnection/persistence | PARTIAL | Same MIDlet reauthenticated and rendered after graceful restart; existing stock/currency retained. Reward/social restart acceptance missing. |
| Starfruit gameplay | PARTIAL | Existing DB harvest/reload and concurrency coverage; no complete new real-client harvest acceptance. |
| Native63 | PARTIAL; live BLOCKED/OFF | Validation/atomicity tests retained; replay after replenishment intentionally reproduced. |
| Production backup readiness | PARTIAL | Existing admin procedure/previous isolated restore evidence retained; no new production backup or restore in M2.2. |
| Load testing | UNVERIFIED; gate BLOCKED | Not performed; no new LOAD_TEST_REPORT. |

Exact screenshots, packets vs static wire expectations, database snapshots, input-mapping diagnosis and the reproducible human attendance procedure are in [MILESTONE_2_2_E2E_EVIDENCE.md](MILESTONE_2_2_E2E_EVIDENCE.md). No STUB/MISSING implementation was created; historical matrix classifications for unrelated features remain unchanged.

## Production and rollback

Production service remained MainPID313665, NRestarts0 at recorded checks; no production SQL/deploy/restart script was run. Own staging used MariaDB127.0.0.1:13317 and game127.0.0.1:19129 through local tunnel19128, with distinct fixture databases. Pre-DDL staging backup SHA256 `3e165157943db279cf18ed34891fb85d3e3fe52daa37ff2fb9ed04ec7e4e7ee1` remains private on homelab. Existing backups were preserved; no production privilege bypass.

Original checkout binary diff hash remained `c31dac0e498aab840a22f09b499a101ec76b8fff66300b21bfde936f8c66ddbf`. Existing emulator5554/shared ADB were preserved. Owned JVM332532 and MariaDB328627 were stopped; tunnel277 and the added reverse19128 were removed. Checks found no remaining staging13317/19129 listeners; shared ADB68690 and emulator5554 remained. See teardown-before.txt/teardown-after.txt. No user emulator data was reset.

Rollback requires stopped writers and reconciliation; retaining additive columns alone does not make old binaries safe. Do not run old/new writers together or restore an old backup over newer rewards. See [migration/rollback procedure](../database/migrations/README.md) and [production backup readiness](PRODUCTION_BACKUP_READINESS.md). Production approval remains outside this milestone; stop before cutover.
