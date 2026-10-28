# Milestone 3.4 — gameplay

**INCOMPLETE.** [Chắc chắn] No native crop lifecycle or new playable farm feature was completed. Client bytecode establishes the visual crop duration unit and incoming 78/79 layouts, but not the server-side economy required to safely restore harvesting. See [catalog recovery](CROP_CATALOG_RECOVERY.md), [crop E2E](CROP_NATIVE_E2E.md), and [next feature](NEXT_FARM_FEATURE_IMPLEMENTATION.md). Zero catalog rows were migrated.

[Chắc chắn] Starting HEAD was `781a267`; branch `codex/gameplay-migration`. Corretto 17 `mvn -B clean package` succeeded: 87 discovered, 35 skipped, hence 52 actually passed in that run. The separate asset-backed `FarmCookingCatalogTest` ran 4 tests with zero skips: one of those is additional to the 52, for 53 unique passing tests this attempt. The 33 isolated DB tests were not run. `python3 scripts/test-tools.py` passed. The locally built JAR SHA-256 is `38279ac0f9b271aeef5c2ef3db6d8f250e7a7d40fad74aef2d74f6d7af190aea`; it was not run in native staging.

[Chắc chắn] No new native smoke tests, DB modifications, staging processes, migration, production changes, push, merge or deployment were performed. The previous milestone's native cooking and starfruit evidence remains historical, not a fresh regression result. Native63 sales remain OFF in the default configuration. The unrelated untracked `logs/null_log.txt` was preserved.

Independent backlog remains: cooking 91 and Native63 replay after replenishment, missing authoritative starfruit level (JOIN still hardcodes 10), burst-login DB pool exhaustion, native emulator automation reliability, and production backup/restore authorization. None is resolved by this investigation.
