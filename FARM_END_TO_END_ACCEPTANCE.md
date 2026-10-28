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

## MariaDB gate recovered

A fresh user-owned instance was provisioned on the previously documented homelab, isolated datadir `/tmp/avatar-seed-db.xtk1f9Bk`, loopback13328, fresh schema `avatar2d_seed_purchase_test`, SSH local13329. MariaDB version 10.11.14-MariaDB-0ubuntu0.24.04.1. No production configuration or data used. SeedPurchasePersistenceTest now supports an explicit loopback-only test URL with fixed disposable schema; default remains H2.

Two MariaDB invocations passed (one test each, no skips); the second includes real FarmMsgHandler command62 dispatch and full response decoding. Synthetic request: short5, byte2, byte1. Decoded response: short5, byte2, int100, byte1, int60, int50, int0, no trailing bytes. These are handler-level packets, not captured native-client traffic.

The same test exercised quantity/currency/ID/capacity/duplicate validation, both currencies, concurrent purchases, native planting/harvesting with fixture mature time, and rollback after a deliberately stale farm revision. Before and after a MariaDB process restart: player7 xu20/luong30, seed5 quantity4, product5 quantity10. Final player revision3/farm revision8 (includes intentional conflict fixture). The restart query is independent of the Maven JVM; it proves database durability, not full game-server restart acceptance. Final default suite:106 discovered/70 passed/36 skipped/zero failures or errors. The separate MariaDB execution reruns an already-counted test, not an additional distinct case.

Server and tunnel created for this run were shut down; private datadir retained. Full client login, visual watering/fertilizer/growth, logout and full server restart remain unverified. Original care/economy rules remain unresolved per existing crop provenance documents. Native63 stays disabled by default and its unchanged request remains ambiguous after replenishment. Native crop feature remains PARTIAL.
