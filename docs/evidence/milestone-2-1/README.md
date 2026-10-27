# Milestone 2.1 evidence

Captured 2026-10-28 during isolated staging work from Git baseline `79e939f48403feb054e6ce2ac509bf45d3b04929`. Overall acceptance remains PARTIAL. See [final report](../../MILESTONE_2_1_FINAL_REPORT.md).

- `test-results.json`: 47 distinct passing cases across default package (32 passed/15 skipped) and two separate opt-in DB groups (15 passed). Console logs are adjacent; expected injected errors are described in the report. They contain no private DB config or Surefire system properties.
- `runtime-identity.txt`: final staging process command, cwd, artifact SHA256 and read-only production identity, captured before teardown.
- `authenticated-packets.log`: 78 selected authenticated gameplay trace rows across intermediate and final staging runs. This is a selection, not a complete network capture. `RECV` means decoded incoming request; `QUEUED` does not prove delivery or rendering. Login credentials are excluded.
- `final-restart-packets.log`: 21 selected rows after the final process's known `processCpuNs=12020000000` probe sample in the private append log. Both 71 and 92 sent context 9. No mutual private-chat request is present; Admin ID 1 messages do not prove private chat between players.
- Three PNGs: same running MIDlet disconnect/relogin around final JVM replacement, plus separately relaunched MIDlet 92 in a game map. These do not prove completed social/reward/farm scenarios.
- `db-after-final-restart.tsv`: synthetic player/farm state after restart; same snapshot was read before restart. Friendship count 0, attendance NULL and score 0 are negative evidence for the still-unexecuted flows.
- `teardown.txt`: verified owned resources stopped after capture. Existing data and unrelated services were retained.

Raw staging logs/logcat, DB credentials, synthetic test password, client JARs and database dumps remain private outside Git. No production backup or active-client capacity claim is made. `SHA256SUMS` covers every file here except itself.
