# Stale-session persistence — Milestone 2.2

## Reproduced vulnerability

Baseline `98d85d7acd5940fb9e6d1f20f0f5edf9a98d3363` allowed `User.saveFarmData` to upsert a full in-memory JSON snapshot without checking what had committed since load. A valid sale or harvest could commit a stock reduction; another User object could restore that stock through a later farm save. `User.saveData` also issued three independent player UPDATEs through a helper that logs/swallow SQL errors, then saved the farm separately. Thus a stale logout could replace chest/score/currency and a farm failure could leave only part of the snapshot committed.

This exists with sales OFF: another session's harvest changes farm JSON, and attendance changes chest/score. The new RED regressions exercise both, without enabling the live catalog. This is a database concurrency problem, not solved by synchronizing one Java object.

## Writer/caller scope and fix

| Path | Previous behavior | Current protection |
|---|---|---|
| `saveFarmData(int)`; harvestCrop/harvestStarfruit callers | Unconditional JSON upsert | Persisted farm revision checked by conditional UPDATE; new farm uses INSERT only, never replaces an existing row. Harvest restores its RAM mutation on failure. |
| `User.close` → protected `saveData` | Separate autocommit player/farm snapshots | One transaction checks player revision and farm revision, commits both or rolls both back. Conflict propagates and is logged. |
| Background autosave | No scheduled caller of saveData/saveFarmData found in this repository | Public saveFarmData is guarded even when called as an autosave; no scheduler was invented. |
| Native63 transaction | Player/farm row locks but no snapshot revision | Checks and advances both revisions under existing transaction/row locks. Live command remains OFF and replay remains BLOCKED. |
| claimDailyAttendance | Date condition only; full chest/score derived from RAM | Player revision and day condition in same transaction as reward; an already claimed day returns false, other stale changes reject. |
| updateTopPhaoXu/updateTopPhaoLuong | Direct counter UPDATE outside snapshot revision | Conditional counter update advances player revision; currency remains legacy RAM mutation saved at logout. Overflow is checked before DB write. |
| loadData/loadFarmData | No revision ownership | Reads persisted revisions. Unloaded player/farm revision starts at -1; it cannot replace an existing row. Reconnect creates a new User and loads current revisions. |
| Duplicate login | DB-locked branch unlocked/kicked owner; in-memory-owner branch scheduled an owner kick | Both reject the incoming login, roll back and preserve current owner. Test covers lock=1 and lock=0 with owner registered. |
| Session.close | Persistence RuntimeException skipped manager/disconnect cleanup | Cleanup moved into finally; failed logout no longer prevents normal removal/transport close. |

Player online/client/IP metadata and reward flags written separately in Session do not replace these economy snapshot columns. No new production session-ownership token or multi-JVM login service is claimed. Existing in-memory mutations use the surrounding legacy lifecycle; this work protects committed state from stale database writers, not every reward's durability before logout.

## Conflict and ambiguous commit policy

Each snapshot remembers its loaded revision. Conditional writes increment the database revision and update the local expectation only after successful commit acknowledgement. A conflict raises SQLState40001; no automatic full-snapshot merge/retry occurs. Player→farm lock order in logout matches sales. A missing-row INSERT racing another INSERT fails without upsert overwrite.

The existing uncertain-outcome flag now covers farm, attendance, counter and logout commits too. It is set before commit; failed acknowledgement/rollback prevents further snapshot writes from that object. Reconnect reads database truth; a successful commit with lost acknowledgement cannot be undone by logout. It is conservative: even a commit that actually failed can require reconnect.

Rejected logout/uncertain logout retains the attempted player and farm values in `logs/persistence-conflicts/<user>-<random>.json`, created with mode0600 and file contents forced to disk. The path is logged at ERROR; retention failures log FATAL. File contents are private and ignored by Git. This retains legitimate uncommitted proposals for operator review instead of pretending they were saved or silently overwriting the winner. Farm-only conflicts retain the proposal in RAM and report failure; if the session subsequently logs out, the journal captures it. Harvest rolls back only its attempted mutation.

Journal `player_values` follows this exact order: gender, friendly, crazy, stylish, happy, hunger, chest_slot, chest_home_slot, xu, luong, luong_khoa, xeng, level_main, exp_main, scores, xu_from_boss, TopPhaoLuong, TopPhaoXu, chests JSON, wearing JSON, chests_home JSON, user_id, expected player revision. `farm_values`: land_data, animal_data, hatgiong, phanbon, nongsan, nongsandacbiet, starfruit_quantity, starfruit_last_harvest_ms. The envelope also records user_id/player_revision/farm_revision.

Reconciliation: stop writers for the affected database/account; preserve the journal and current DB backup; compare current revisions and committed operation logs; determine only the legitimate missing delta; apply an approved delta in a conditional transaction and record its result. Never import the rejected full snapshot and never reset revisions. Do not commit journals to source control.

## Schema, rollback and limitations

`20261028_persistence_revisions.sql` adds BIGINT NOT NULL DEFAULT0 to players and farm_data. Applied twice on stopped isolated staging after a private pre-DDL backup, preserving JSON/currency/claim state. It is not applied automatically at boot. All writers must maintain revisions: mixed old/new JARs and external SQL updates without revision increments are unsupported. See [migration procedure](../database/migrations/README.md). Production was not migrated.

Rollback must keep writers/traffic stopped until reconciled. Old binaries ignore revisions and reintroduce this vulnerability even if additive columns remain. Do not restore a pre-change backup over newly committed rewards.

Known limits: this is not a durable operation ledger or native63 replay solution. It does not recover arbitrary RAM-only gameplay after process death, atomically couple all legacy reward flags with credits, provide automatic conflict merging, or establish safe mixed-version multi-JVM operation. A full disk/unwritable journal prevents retention (explicit FATAL); directory metadata is not fsynced, so power-loss durability of the journal is not claimed. UserManager/online metadata are still legacy lifecycle state. Real OS crash/network partition testing was not performed; JDBC boundary faults are identified as such.

## Verification

60 unique tests: 33 unit +27 real isolated DB tests. Added coverage includes stale farm autosave, stale logout after attendance with sales OFF, farm-conflict rollback of currency, current logout/repeated save, stale harvest RAM rollback, uncertain logout retry, duplicate login owner preservation, direct legacy counter conflict, concurrent farm writers and lost harvest commit acknowledgement. Existing transaction validation/overflow/concurrency/native63 replay characterization remain. Session cleanup regression is a unit test. Reload tests create fresh User objects; separate staging JVM restart evidence is documented independently, not inferred from these tests.

RED evidence includes stale-autosave assertion failure; duplicate-lock regression; rejected-snapshot retention failure; direct-counter regression; logout exception skipping cleanup; and duplicate-owner notification/kick regression. Final logs/counts are in [Milestone 2.2 evidence](MILESTONE_2_2_E2E_EVIDENCE.md). One overlapping pair of earlier DB test commands produced fixture errors; those runs are invalid evidence and were replaced with serial runs.

Review follow-up: JDBC Connection.close after acknowledged attendance/counter commit could skip RAM synchronization while trusting the advanced revision. Both cases were reproduced RED and now mark persistence uncertain on outer SQL failure, blocking logout until reconnect. These two DB regressions are included in the60 unique tests.
