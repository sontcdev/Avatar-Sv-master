# Farm end-to-end acceptance

Current artifact: no fresh native client or disposable-database acceptance. No gameplay loop is marked complete.

Harvest capacity regressions: new product at 127 types rejected without writes; existing starfruit entry accepts harvest. Duplicate starfruit rows now reject harvest without changing stock/tree. Unit fixtures override saveFarmData, so these are not persistence proofs.

Native63 remains default off; replay-after-replenishment characterization and concrete negotiated operation-ID/inventory-revision proposal are in docs/NATIVE63_REPLAY_ANALYSIS.md. No protocol extension implemented.

## Skipped test prerequisites

36 cases remain skipped in the default run. FarmPersistenceTest and AttendancePersistenceTest require avatar.test.isolatedFarmDb and disposable avatar2d_starfruit_test. FarmSalePersistenceTest, CookingPersistenceTest, FriendshipPersistenceTest and the database quiz case require avatar.test.isolatedSalesDb and their asserted disposable schema. Inspect each setup before enabling: they create/reset tables. Use private avatar.database.config, never production configuration.

FarmCookingCatalogTest requires avatar.test.farmCookingCatalog pointing to the verified farm_info.dat asset. CookingServiceTest requires res/data/farm_info.dat. Neither absence is a pass. CustomCropPersistenceTest uses an in-memory fixture by default; optional MariaDB requires avatar.test.customCropMariaDb.

## Transactional seed purchase continuation

Production User.buyFarmSeed now commits players balance/revision and farm snapshot/revision in one transaction. Player CAS includes persisted balances, preventing spending unpersisted or stale balances. The synchronized User lock serializes same-session purchases; persisted revisions reject stale other writers. Existing logout CAS remains in use. Commit/rollback uncertainty blocks subsequent saves; service closes uncertain sessions. Success response follows commit and RAM update.

Verified against local baseline d8d5f26: signed quantities, currency mismatch and duplicate appending existed there. At 8242ad5 signed quantity/currency checks and stacking were already fixed. Missing atomic persistence and response-before-inventory mutation remained. Current farmSnapshot adds JSONObject entries correctly; no self-reference found. No original economy table was added or modified.

H2 JDBC fixture (not MariaDB): 100xu/50luong -> buy2 at fixture20 =60/50, seeds2; buy1 with luong =60/30, seeds3; plant1 -> seeds2; mature fixture clock -> harvest10; reload retains seed/product stocks. Two simultaneous purchases2 at20 allow exactly one:20xu,seeds4. Induced farm revision conflict after player UPDATE rolls back debit and stock mutation. Unknown ID, invalid currency, negative/zero/100 quantities, insufficient balance, full stack and duplicate seed rows reject. Fixture time/cost/yield are synthetic, not evidence of original rules.

No native packet capture, real login/logout or process restart acceptance performed. Packet construction retains existing 62 layout: short ID, byte quantity, int previousXu, byte currency, int committedXu, int committedLuong, int lockedLuong. Target decoding/semantics still require actual-client verification. No MariaDB binary was found; Docker daemon socket and standard Docker.app are absent. H2 is not a substitute for the requested MariaDB gate. New test has no opt-in production connection.
