# Custom Crop v1 implementation status

- BACKEND_IMPLEMENTED; BACKEND_TEST_VERIFIED.
- STAGING_RUNTIME_BLOCKED: the startup gate is exercised, but the disposable database lacks the `settings` table, so a complete server startup was not demonstrated.
- REAL_MAPPING_UNVERIFIED; NATIVE_UNVERIFIED. Native gameplay remains disabled.

The backend implementation is commit `70ba2b3c8f9ca3aef010cf504b5b5f138b6a435f`. The runtime integration is the commit containing this report; its SHA is available via `git log`. The final shaded JAR SHA-256 is `6792609871aab3cd5c72df91fb654771c8f3549bf388ba8e72c31be4d2c65ef5`.

The feature defaults OFF. Enabling it requires an independently provisioned, owner-only identity file and a matching single-row marker in the explicitly named disposable `avatar2d_customcrop_test` database. Startup rejects missing or mismatched identity and mappings, and only `synthetic-test` mode is accepted. This is a fixture gate, not authorization for a native deployment; physical isolation still depends on how the test database and identity are provisioned. The file and marker contain a secret and must be provisioned independently of ordinary JVM flags. No production configuration, marker or credentials were changed.

A real server JVM with an unauthorized flag failed closed; with the fixture identity it passed the startup gate and reached database settings loading, then stopped because the disposable database had no `settings` table. Two separate JVMs used the runtime-loaded synthetic policy and actual command handlers: one planted and persisted a crop, the second reloaded it and harvested at a test-injected deadline, checking inventory and duplicate harvest. This is not a full server restart or authenticated TCP test. `scripts/test-tools.py` passed its existing framing tests; authenticated custom 64/66 protocol and native UI acceptance were not tested.

The 2.5.8 command 64 response reads `int, byte, byte` and uses the seed ID to find inventory and set the displayed crop ID. The ordinary command 60 inventory path reads the item identifier as a signed byte; the fixture therefore restricts its nonnegative product ID to 0–127. These observations do not establish real native seed or product mappings. Fixture IDs are synthetic only.

The rules consume one seed, persist health 100 and a four-hour server deadline, and grant two products without currency or XP. Farm snapshots preserve crop metadata and inventory together using the existing revision-checked transaction. Older server writers discard the added JSON fields: reconcile active custom crops before rolling back. Native packets lack a crop-instance operation ID, so a delayed harvest after a land cell is reused cannot be distinguished from a new request.

Corretto 17: 101 distinct tests passed across the regular build, isolated MariaDB groups and asset-dependent run; reruns are not counted. These include planting, harvesting, duplicate/concurrent actions, persistence, CAS, rollback, OFF behavior, identity validation, cooking, inventory and starfruit. The isolated MariaDB data and pre-reset backups were retained under `/tmp/avatar-customcrop-runtime.DpcLsM3N` on the test host. No production deployment or data was touched.
