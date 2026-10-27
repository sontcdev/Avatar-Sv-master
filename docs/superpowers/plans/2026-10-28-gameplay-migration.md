# Gameplay recovery implementation plan

Goal: compare the preserved local deployment with the two pinned donors and implement compatible, independently verified repairs.
Architecture: selective changes to existing dispatchers, services and User persistence; no donor class transplant or production cutover without gameplay evidence.
Spec: user-supplied Avatar 2D Gameplay Feature Recovery and Migration, 2026-10-28.

- [x] Preserve original dirty checkout; create codex/gameplay-migration worktree containing local changes.
- [ ] Establish Java 17 test baseline and record deployment read-only snapshot.
- [ ] Inspect both pinned donor trees and licensing, actual farm/social/NPC/clan/minigame/home/auction implementation and schema.
- [ ] Write docs/GAMEPLAY_MIGRATION_MATRIX.md with command/payload/dependency/status evidence and architecture decision.
- [ ] Add failing regression tests for verified compatible defects, then implement minimal fixes in existing owners.
- [ ] Validate packet bytes, replay, ownership, concurrency and persistence using isolated database; do not treat unit tests as actual client evidence.
- [ ] Stage only on unused loopback port with isolated DB if credentials/access permit; preserve production and unrelated services.
- [ ] Record exact completed checks and unresolved legal/runtime/client blockers in docs/MIGRATION_REPORT.md; provide forward-only SQL and rollback guidance for any schema change.

Primary code boundaries: message/MessageHandler and ParkMsgHandler -> AvatarService/ParkService for social; FarmMsgHandler -> FarmService -> User.saveFarmData/loadFarmData for farms; NpcHandler/GlobalHandler for menus; constants/Cmd and bundled client for wire contracts. Tests under src/test/java; existing deploy scripts must not be run against production for staging.
