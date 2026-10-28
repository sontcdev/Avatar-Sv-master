# Gameplay readiness

NOT READY. This continuation adds a fail-closed duplicate-starfruit inventory check and an automated index of 127 dispatcher candidates. It does not restore a complete farming or NPC journey.

Verification: JDK17 mvn -B test: 105 discovered, 69 passed, 36 skipped, zero failures/errors. Duplicate-row regression failed before the production fix and passed afterward. Capacity tests from fc49db5 remain green. No fresh DB, native client, restart or multiplayer acceptance.

Known remaining work and prerequisites are recorded in GAMEPLAY_IMPLEMENTATION_BACKLOG.md and FARM_END_TO_END_ACCEPTANCE.md. Existing readiness report and logs remain untouched. Separate master worktree untouched.
