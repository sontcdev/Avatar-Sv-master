# Milestone 3.8.1 — acceptance evidence

Baseline source commit: `0be717a5fbf2e84da04ed24b2ffae3e4568bc8f8`. This report's commit is identified by `git log -1 --format=%H -- docs/MILESTONE_3_8_1_ACCEPTANCE_REPORT.md`. Final clean-build shaded JAR SHA-256: `66d470ed3f22a2e55433eb49753dca84a6dad2da3192e2ef8c12ba221d60d93c`. Source was unchanged by this milestone. The previous milestone recorded a different JAR hash; no reproducible-build conclusion is made. The isolated TCP server ran the newly built JAR with the same verified hash.

| Acceptance item | Status | Scope |
| --- | --- | --- |
| CUSTOM_NPC_645_CODE | VERIFIED | Existing implementation and 94 distinct regression tests on baseline source; this does not prove native behavior. |
| CUSTOM_NPC_645_PROTOCOL | NOT_RUN | The complete requested fallback matrix was not run; partial authenticated TCP observations are below. |
| CUSTOM_NPC_645_NATIVE | UNVERIFIED | No working authorized client on an isolated device. |
| BUY_ANIMAL_71_NATIVE | UNVERIFIED | No native purchase UI, rendering or reconnect check. |
| ANDROID_EMULATOR | UNRESOLVED | Recreated isolated AVD stayed offline; the exact `mprotect` permission failure remains unexplained. |
| ORIGINAL_NPC_645_RECOVERY | BLOCKED | This custom quiz does not restore original gameplay. |

## Tests and configuration

JDK 17 `mvn -B clean package` discovered 94 tests: 58 passed and 36 were skipped pending isolated DB/assets. Subsequent runs on the same source passed 2 isolated farm/attendance tests, 36 isolated sales/cooking/friendship/quiz tests, and 13 asset-dependent catalog/cooking tests, all with zero skips. Accounting for overlap with the baseline, all **94 distinct tests passed**. Logs: `/private/tmp/avatar2d-m381-build.log`, `/private/tmp/avatar2d-m381-farm-test.log`, `/private/tmp/avatar2d-m381-sales-test.log`, and `/private/tmp/avatar2d-m381-assets-test.log`.

The quiz flag `avatar.customEnglishQuizV1` is false by default and was enabled only for the isolated protocol run. Native63 remains default OFF; BUY_ANIMAL 71 remains blocked. The quiz state is session memory and its implementation does not persist answers or scores; its isolated database regression also passed. The isolated TCP run did not independently prove the absence of every database or balance change. Existing deployment scripts exist; no production deployment or configuration was changed in this milestone.

## Authenticated TCP observations — partial, not native acceptance

On loopback-only isolated staging, authenticated test account 71 reached map 6 at (150,100). With the flag on, request `-61` for NPC 645 received `-59` with the approved menu text; callbacks yielded the three approved questions and a `3/3` result. A repeated old menu ID and a future ID produced no new quiz menu in the observation window; exit prevented a further callback and reopening produced the initial menu. In farm context command 71 produced the temporary-unavailable message and no command 71 response in the one-second observation window. On a separate server restart without the flag, the same account did not receive the custom quiz menu. Logs: `/private/tmp/avatar2d-m381-protocol.log`, `/private/tmp/avatar2d-m381-protocol-extra.log`, `/private/tmp/avatar2d-m381-protocol-off.log`.

These traces do not establish client rendering, all invalid callback cases, concurrent-session isolation, reconnect cleanup, or independent database/currency invariance over TCP. Those parts of the requested protocol matrix remain unrun, so the aggregate status is NOT_RUN rather than PROTOCOL_VERIFIED. The native application, screenshots, actual menu decoding, purchase UI and reconnect behavior remain unverified. Session-state and mutation cases also have regression tests, but tests do not substitute for these missing packet or native observations.

## Environment and safety

See [emulator diagnosis](ANDROID_EMULATOR_DIAGNOSIS.md). Isolated staging and the two isolated test databases were backed up before use and restored from hash-checked dumps after stopping the owned game server; the owned database and SSH tunnels were stopped. The temporary resource symlink was removed. The unrelated `logs/null_log.txt` was preserved. No production data, push, merge or deployment was touched.

The user authorized cleanup under `/private/tmp/avatar2d-m38-private`, but an earlier cleanup command was rejected by automatic safety review. That restriction was not bypassed. The directory remains mode 0700 and its private files mode 0600; credentials and private dump contents are not included here.
