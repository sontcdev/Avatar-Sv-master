# Milestone 2.5 — controlled native-client load, 2026-10-28

## Milestone 2.6 update

**Five-native readiness FAIL; ten native NOT ATTEMPTED;10 active CCU UNVERIFIED.** Five real accounts authenticated for21samples/span100.074seconds; only92/93/95 movement completed.71 submitted unchanged coordinates;94 moved but camera broke image classification. Stopped before farm/chat readiness or a300second active window; no ramp10. M2.5 user95 cause remainsUNRESOLVED. Handler timing is now available from103dispatches,with command/sample counts; it is not client RTT or capacity evidence. [M2.6 result](MILESTONE_2_6_FINAL_REPORT.md),[per-client evidence](LOAD_TEST_10_CCU_EVIDENCE.md),[latency/limits](PERFORMANCE_LATENCY_REPORT.md).

## Milestone 2.5 historical run


Final artifact SHA256 `2c93ede1f6082474b7ab3a419301e9542e5969e745cf961322f71d3e8019f2bf`. Isolated Ubuntu staging, Core2Duo T6670 /2cores/~3.8GiB/HDD; Java17 Xmx128MiB, Hikari max4/min1, MariaDB13317 buffer32MiB/max12. Native63 live sales OFF. No production deployment.

Ten read-only Android emulator clones on a separate Mac run actual Avatar2.5.8 through J2ME Loader. Accounts71,92,93,94–100; no bot or idle TCP socket is counted. External WiFi is disabled; ADB reverse/SSH connects only staging19129. Full-mesh45 friendship rows are an explicit load fixture created **after** real UI delete/restart-empty acceptance. They do not prove fresh invitation acceptance.

Native UI cycles move, open friends, visit selected friend farm, move, exit to world map and return to park. Actual native61/self bootstrap precedes61/selected owner, resource requests and rendered friend farm. Own-farm bootstrap is exercised; repeated standalone own-farm rendering is not established by this loop. Private chat is sent at active multiclient levels using direct text events; no latency is inferred from automation sleeps. Account71 visits92; others visit71. Client UI actions are driven by ADB, not injected gameplay packets.

Metrics bootstrap/observer run outside the unchanged final JAR, sample every5seconds. Authenticated count checks registered users with live logged-in sessions; baseline0 despite NPC actors, then corroborated against native account packet IDs, screenshots and UI activity logs. JVM CPU is process CPU delta / wall time:100% means one core. Heap/RSS/threads/pool are sampled maxima, not exact transient peaks. GC and DB lock counters use window deltas. Pool active0 in a sample does not imply no queries. Host disk I/O includes unrelated workloads; `/proc` production process I/O is permission denied. Production checks cover PID identity, service state/restart count, CPU/RSS and host pressure; no unrelated-service application SLA probe.

## Qualified windows and measurements

**Outcome: PARTIAL; 10 active CCU acceptance NOT achieved.** Levels1/3 have verified native gameplay windows longer than five minutes. Level5 maintained five authenticated users for322seconds, but client93 automation drifted into park menus and required manual recovery; do not promote that connection plateau to a fully verified five-active-player workload. Level10 reached ten authenticated users during ramp, then fell to eight (93 deliberately force-stopped for input recovery,95 disconnected). No qualified five-minute10-active window. Load was stopped; no increase beyond10, no attempt to hide the disconnect or fill the count with bots.

The first3-client attempt was also invalidated: two clients idled beyond the server120second socket timeout before the worker began; an initial worker termination pattern missed the full Python interpreter path. Explicit PIDs were then stopped. That interval and its misdirected UI chat are excluded. The subsequent3-client window has three continuously authenticated users and verified rendered friend farms/chat. Later automation logs describe intended actions, **not assertions of successful UI completion**. At higher concurrency emulator System UI ANRs, slow startup and UI state drift occurred; server-vs-client root cause of the final95 disconnect is not established. No native p95 can separate them.

|Level|UTC window (2026-10-28)|Seconds|Auth samples|JVM CPU % of one core|Heap max MiB|RSS max MiB|GC count/ms delta|Java/OS threads max|Pool active/idle/wait max|
|---|---|---:|---|---:|---:|---:|---|---|---|
|1|04:43:09–04:48:27|317.8|[1] (64 samples)|2.18|30.17|109.71|1/18|67/82|0/2/0|
|3|04:58:28–05:05:20|411.0|[3] (82 samples)|2.96|22.11|114.39|0/0|71/86|1/2/0|
|5|05:07:52–05:13:14|322.3|[5] (64 samples)|4.17|18.51|114.18|0/0|77/92|0/2/0|

Level5 metrics describe its authenticated plateau, with the activity limitation above. Ten-user ramp samples are retained raw but have no acceptance metric row. All qualified/plateau windows: DB new lock waits0, lock-wait time delta0, sampled current waits0; baseline cumulative6/147ms came from earlier integration tests. Slow queries0. JVM heap has no monotonic runaway within these short windows; no long-term claim.

|Level|JVM disk read/write bytes delta|Host HDD read/write bytes delta|Host HDD I/O ms delta|Host load1 max|Host available memory min MiB|Production Java CPU % / RSS max MiB|Production DB CPU % / RSS max MiB|
|---|---|---|---:|---:|---:|---|---|
|1|0/303104|1835008/11872768|9327|0.64|2481.8|0.33/132.54|0.042/119.9|
|3|0/561152|151552/11166208|12172|0.47|2477.39|0.301/132.54|0.039/119.9|
|5|0/561152|4096/10186240|8964|0.78|2480.31|0.395/132.66|0.038/119.9|

Production PIDs313665/302726 stayed unchanged; avatar2d ActiveState=active,NRestarts=0 throughout samples and after teardown. No significant pressure was observed in sampled host metrics; this is not an application latency/SLA verification for every homelab service. Full staging log has zero EOF/Exception/ERROR and zero FarmMsgHandler55; command/owner evidence remains distinct from UI automation success. Unplanned/intentional preparation disconnects and final graceful shutdown are visible in the filtered trace; no exact per-window disconnect count is inferred from a log without packet timestamps.

Evidence: [raw JVM metrics](evidence/milestone-2-5/load/jvm-metrics.csv), [host metrics](evidence/milestone-2-5/load/host-metrics.jsonl), [window summaries](evidence/milestone-2-5/load/window-summary.jsonl), [phase events](evidence/milestone-2-5/load/phase-events.jsonl), [filtered packet trace with original line numbers](evidence/milestone-2-5/load/packets.txt). 3-client chat marker `M25_LEVEL3_ACTIVE_CHAT` is actual71→92 native-6; screenshots `load/3-*` show each client rendering the correct friend owner. Level5 screenshots show71→92 and92/93/94/95→71 at observed points; they do not prove an uninterrupted five-player loop.

After graceful JVM stop, all10 players' currencies/chests/scores/attendance_date match pre-load snapshot. Existing farms71/92/93 preserve every compared gameplay field;94/95 first persist on logout, with no pre-row equivalence claim. Revisions increase on legitimate saves. No harvest/attendance claim is performed in load. [Integrity evidence](evidence/milestone-2-5/load/integrity.txt), [post players](evidence/milestone-2-5/load/players-after.tsv), [post farms](evidence/milestone-2-5/load/farms-after.tsv). New96–100 farms are not covered by that existing-farm comparison. Owned45 friendship fixtures removed after stop; total friendship count0. Private backups retained; [safe cleanup](evidence/milestone-2-5/cleanup.txt).

## Limits

Accurate gameplay p95 latency is **unavailable**: neither native request correlation nor client frame timing was instrumented. ADB action duration contains deliberate sleeps and host scheduling, so it is not gameplay latency. Short controlled staging windows cannot establish production capacity, long-term memory behavior, economy safety under writes, minigame throughput or all backlog acceptance. Attendance is not reset or claimed repeatedly during load. Complete natural-route attendance remains separately unproven.
