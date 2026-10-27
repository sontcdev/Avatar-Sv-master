# Milestone 2.1 — verification report

**Overall: PARTIAL. Milestone 2 is not complete. Production unchanged; live sales OFF.**

## Baseline and final artifact

Continued existing `codex/gameplay-migration` worktree from clean HEAD `79e939f48403feb054e6ce2ac509bf45d3b04929`. No reset, new worktree, migration restart or production cutover. The original checkout's unrelated tracked binary diff remains SHA256 `c31dac0e498aab840a22f09b499a101ec76b8fff66300b21bfde936f8c66ddbf`.

Corretto 17.0.18 `mvn -q package` passed. Exact JAR SHA256: `9ef00532c343e4dfd44db8aace5fcf67702b70e913a95f39c3150fedb8106008`. This same artifact ran in isolated staging, including final restart as PID 327395, cwd `/home/sontc/avatar2d-gameplay-stage`, game loopback 19129; isolated MariaDB PID 325129/loopback 13317. [Runtime identity](evidence/milestone-2-1/runtime-identity.txt) records actual command, hash and production MainPID 313665/NRestarts 0. No sales catalog property was supplied.

## Verified implementation and test scope

**47 distinct tests passed: 32 unit + 15 opt-in DB tests**, not a sum of reruns. Default package discovers 47 and skips the 15 opt-in cases. Separate JVM runs against disposable DBs passed farm/attendance 2 and sale/friendship 13 (12 sale cases, 1 friendship). [Machine-readable counts](evidence/milestone-2-1/test-results.json) and adjacent final logs record the split. One final farm invocation initially referenced a nonexistent config path and failed before DB access; corrected invocation passed. Expected injected failures in test logs are not unhandled suite failures. Characterization tests below pass by asserting the vulnerable behavior; they are not replay-safety passes.

Two focused server fixes:

- Boss and offline bot factories now use internal sessions without player TCP sockets, outbound queues or player logout persistence. Previously 14 boss connections were created before the accept loop, exceeding staging's 10-client limit until socket timeout. Regression test failed before the change and passes after; a real listener accepts no boss connections. Existing callers retain initialized services and handler context.
- Initial city context 8 tolerates a null zone. Authenticated client packet `-1:08` previously caused an NPE; a regression test reproduced it and passes with the guard.

Five new real-DB sale tests reproduce replay after real harvest, stale snapshot restoration, commit-before-delegate failure, successful commit with lost acknowledgement, and rollback failure. JDBC fault proxies preserve actual SQL, locks and DB commits; no runtime fault switches or dependencies were added. Existing duplicate/concurrency, validation, overflow, rollback and reload tests also passed. These tests do not simulate power loss or prove sale→JVM restart→real-client reload.

## Required client acceptance

| Flow | State | Actual evidence and missing acceptance |
|---|---|---|
| Private chat | PARTIAL | Authenticated clients 71/92 reached game maps. Captured `-6` messages are Admin ID 1 login notifications, not mutual private chat. No incoming mutual `-6` or recipient-render proof; sender ID/name/message E2E still missing. |
| Friends | PARTIAL | Invite/accept/list/delete persistence tests pass. No real-client invite/accept/list/delete sequence or friendship preserved across logout/JVM restart. Staging friendship count remains 0. |
| NPC 850 attendance | PARTIAL | Atomic reward/double-claim DB test passes. No client claim; both players still score 0, empty chests and NULL attendance after final restart. Ticket 593/+20 score E2E and restart rejection remain missing. |
| Farm visits online/offline | PARTIAL | Existing packet/authorization checks pass. No completed real-client online then offline owner visit; required UI, owner state and restart evidence missing. |
| Reconnection | PARTIAL | Same running MIDlet 71 displayed disconnect after final JVM replacement, authenticated again and entered map 9/zone 0. Fresh MIDlet 92 launch was exercised separately. Full repeated reconnect/feature persistence acceptance remains incomplete; idle disconnect root cause not conclusively captured. |
| Starfruit harvest | PARTIAL | Prior Milestone 1 client harvest evidence is historical. Persistence regression passed; Milestone 2.1 does not add a complete client harvest/sale/restart sequence. |
| Product sales | BLOCKED | Native 63 is a product short only. Saved `00 d7` after sale and real replenishment sells new stock; stale session save also restores sold stock. No safe compatible client operation ID/revision or approved production catalog. Sales stay OFF. |
| Production backup | PARTIAL | Prior staging restore of 35 tables/checksums is retained. Administrator production backup/isolated restore still absent; approved runbook and rollback instructions retained. |
| Progressive load test | BLOCKED | Critical functional gate above has not passed. No 1/3/5/10 active-client load test and no LOAD_TEST_REPORT.md. Incidental probe samples are not capacity measurements. |

No requested gameplay flow was promoted to WORKING from unit tests, queued packets or two visible avatars. No new MISSING implementation is claimed in this milestone; older unrelated migration gaps remain in the matrix.

## Packet, UI and database evidence

[Selected authenticated packet records](evidence/milestone-2-1/authenticated-packets.log) contain decrypted payload hex and command/context/player identity. `QUEUED` means queued, not delivered. The file spans the intermediate boss fix and final city fix runs; it is not evidence that every row came from the final process. The final restart packet subset is stored separately. Wire layouts are retained in [Milestone 2 evidence](MILESTONE_2_EVIDENCE.md); a verified layout is not an executed client scenario.

[Fresh MIDlet 92](evidence/milestone-2-1/fresh-midlet-92.png), [same MIDlet disconnect](evidence/milestone-2-1/same-midlet-disconnected.png) and [reconnect](evidence/milestone-2-1/same-midlet-reconnected.png) were captured around PID 327395 restart. Synthetic accounts only. Raw logs/logcat and DB credentials remain private outside Git; selected records contain no login password. [DB after final restart](evidence/milestone-2-1/db-after-final-restart.tsv): both players xu 20000/score 0/attendance NULL; player 71 item 215 stock 195 and persisted anchor 1793123779887; player 92 stock empty/tree 180; friendships 0. Same values were read before restart. This proves preservation of that existing snapshot, not completion of unexecuted reward/sale/social flows.

## Economy decision and remaining work

[Replay analysis](NATIVE63_REPLAY_ANALYSIS.md) documents 100→240→380 at synthetic test-only 7/unit, stale writes and failure outcomes. No production prices were invented. Transport XOR cursors are not authenticated durable request IDs. A compatible extension needs verified capability negotiation/client permission, durable operation ledger and inventory revision checks across every writer; no speculative packet extension was shipped. A private-server residual-risk exception was not authorized.

[Connectivity diagnosis](CLIENT_CONNECTIVITY_DIAGNOSIS.md) separates ADB, tunnel, cached city, server admission and client state, and provides isolated startup/teardown. Remaining work is actual client navigation/packet capture for chat, friends, attendance and visits, plus feature persistence after logout/restart. This is an evidence gap, not proof those handlers work E2E. Production backup requires administrator execution of [the existing procedure](PRODUCTION_BACKUP_READINESS.md). Load testing remains gated on functional acceptance. Stop before production cutover.

## Teardown

After evidence capture, the verified staging JVM, isolated MariaDB, owned SSH tunnel and owned emulator 5556 were stopped; run-owned reverse mappings were removed. Original emulator 5554, shared ADB 5037, production PID 313665/NRestarts 0 and all data were preserved. [Teardown record](evidence/milestone-2-1/teardown.txt). Runtime identity above describes the captured run, not a currently running staging service.
