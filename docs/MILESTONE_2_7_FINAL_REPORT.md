# Milestone2.7 — final report

[Chắc chắn] Backend TCP activity and native acceptance remain separate. Production readiness is BLOCKED.

| Gate | Result |
|---|---|
| NATIVE_5_ACTIVE | FAIL |
| NATIVE_10_ACTIVE | NOT_RUN |
| PROTOCOL_10_ACTIVE | PASS |
| BACKEND_LATENCY | MEASURED |
| PRODUCTION_READINESS | BLOCKED |

## Tested identity and checks

- Worktree: `/Volumes/sontc/sources/others/avatar2d-gameplay-migration`, branch `codex/gameplay-migration`; original checkout unchanged by this work.
- Baseline HEAD `292c1f166f86b5773c95916fcc3d93af3a6a41af`:63 unique tests PASS using Corretto17.
- JAR build/test source `459d266fc3c63b3e304c405ed54b39f164b71b4d`:64 unique tests PASS (63 baseline plus park snapshot regression). Package, sales opt-in and disposable farm groups all PASS; manifest deduplicates class+method rather than summing cumulative Maven XML.
- Final harness commit `e21cf2e7c5ab4a3ec64a54980a2dbfcb5275bec4`; subsequent changes are evidence/docs only. Python protocol8 checks and native driver checks PASS.
- Artifact SHA256 `f3bae8def8c2dd0fed2682b4e64f3a76b72d4db2a7fc033dffdf1bccd1a09400`; verified locally and on staging. Runtime PID390152, cwd `/home/sontc/avatar2d-gameplay-stage`, JDK17, Xmx128MiB, ActiveProcessorCount2, poolmax4, isolated DB13317, clientlimit10. Staging-only trace/telemetry enabled, sales property absent/OFF.

## Native findings

Account94’s original fixed three-pixel scene test failed after camera movement despite changed world coordinates. Whole-scene calibrated palette plus fresh authenticated park/context/zone evidence fixes that specific false negative; synthetic translated images and cached screenshots verify it. This is scene corroboration, not proof of sprite movement or farm rendering.

Account71’s left key21 changed348→258 at y156. Reverse key22 sent the identical command54 payload `0102009c020000`, including old facing2; server directly assigned and broadcast the requested unchanged position. No server collision rejection occurred for that packet. Client bytecode sets right facing0 before collision checks, so pure right collision does not explain old facing2. [Có khả năng cao] The50ms client update and short DOWN/UP timing can serialize unchanged state; actual loader/frame flags remain unverified. Map9 live tile geometry was not captured; no guaranteed reverse path is claimed.

The first five-client gate stopped at71’s failed leg. A later preparation authenticated all five and reached park, then lost authentication before a single controlled held-key calibration; the driver rejected input. No300second native active plateau exists. No ten-native attempt followed. FARM_RENDERED remains uncalibrated. Historical95 disconnect remains unresolved.

See [native diagnosis](NATIVE_MOVEMENT_ROOT_CAUSE.md).

## Backend findings and scope

Real TCP handshake/XOR/login/context commands, exact response matches and independent registered-session sampling establish gameplay completion; no User/Session injection or DB-written simulated outcomes. Scenarios include movement, park chat, own farm, authorized friend farm and private-chat recipient delivery. Socket close/new sessions between levels verify reconnect outside the active plateau.

The initial concurrency run found a real park join race. Snapshotting synchronized membership fixes inconsistent packet serialization and missing joining membership; deterministic regression failed before and passed after the fix. A later simultaneous-login burst exposed nested connection acquisition and pool exhaustion. That defect remains open. Final capacity runs serialize login then align concurrent gameplay, with unchanged failure deadlines and pool size.

All four final levels maintained full authenticated/active samples for at least300seconds. Ten active protocol sessions are therefore supported by this low-rate scenario; native reliability, burst login, saturation and production traffic mix remain unverified. See [protocol design](PROTOCOL_LOAD_HARNESS_DESIGN.md) and [measured results](PROTOCOL_LOAD_TEST_RESULTS.md).

## Safeguards and teardown

[Chắc chắn] Initial/final20-row gameplay snapshots match excluding revision counters. Sales remained OFF. No production SQL writes, service changes, deployment or cutover occurred. Read-only production sampling retained JavaPID313665 and MariaDBPID302726; service active, NRestarts0. Monitoring does not establish every unrelated workload’s latency SLO.

Owned native clients/emulators, workload workers, staging JVM/DB, observers and tunnel were stopped; shared ADB and unrelated emulators were untouched. Staging backups remain mode600, including before, pre-fix and final dumps. Exactly nine owned friend-fixture rows were removed after writer shutdown, restoring baseline zero. Cleanup evidence: `evidence/milestone-2-7/cleanup.json`.

Evidence includes SHA256SUMS, test manifests, artifact identity, authentication/activity timelines, filtered packet/handler telemetry, JVM/host/DB metrics and integrity snapshots. Raw credentials, DB backups and raw server logs remain private outside Git.

Production readiness stays BLOCKED pending separate backup, compatibility and deployment gates, native reliability and the unresolved burst-login defect. No push was performed.
