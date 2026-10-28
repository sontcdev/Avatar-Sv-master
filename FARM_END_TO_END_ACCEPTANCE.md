# Farm end-to-end acceptance

Current artifact: no fresh native client or disposable-database acceptance. No gameplay loop is marked complete.

Harvest capacity regressions: new product at 127 types rejected without writes; existing starfruit entry accepts harvest. Duplicate starfruit rows now reject harvest without changing stock/tree. Unit fixtures override saveFarmData, so these are not persistence proofs.

Native63 remains default off; replay-after-replenishment characterization and concrete negotiated operation-ID/inventory-revision proposal are in docs/NATIVE63_REPLAY_ANALYSIS.md. No protocol extension implemented.

## Skipped test prerequisites

36 cases remain skipped in the default run. FarmPersistenceTest and AttendancePersistenceTest require avatar.test.isolatedFarmDb and disposable avatar2d_starfruit_test. FarmSalePersistenceTest, CookingPersistenceTest, FriendshipPersistenceTest and the database quiz case require avatar.test.isolatedSalesDb and their asserted disposable schema. Inspect each setup before enabling: they create/reset tables. Use private avatar.database.config, never production configuration.

FarmCookingCatalogTest requires avatar.test.farmCookingCatalog pointing to the verified farm_info.dat asset. CookingServiceTest requires res/data/farm_info.dat. Neither absence is a pass. CustomCropPersistenceTest uses an in-memory fixture by default; optional MariaDB requires avatar.test.customCropMariaDb.
