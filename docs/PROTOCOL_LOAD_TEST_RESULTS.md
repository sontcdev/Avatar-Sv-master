# Protocol load results — M2.7

[Chắc chắn] These are real staging TCP sessions with distinct synthetic accounts. Login is serialized; gameplay runs concurrently. This is a low-rate capacity scenario, not saturation or login-burst acceptance.

| Active clients | Continuous full-active sample span (s) | Full samples | Completed ops in window | Ops/s | Failed ops | Unexpected disconnects |
|---:|---:|---:|---:|---:|---:|---:|
| 1 | 310.268 | 63 | 63 | 0.203 | 0 | 0 |
| 3 | 310.314 | 63 | 237 | 0.764 | 0 | 0 |
| 5 | 310.263 | 63 | 395 | 1.273 | 0 | 0 |
| 10 | 310.252 | 63 | 790 | 2.546 | 0 | 0 |

Active means a completed matched operation within30seconds plus current authenticated session and live connection. Counts are sampled every5seconds; every full-active sample in each reported window must match the requested level with no gap≥6.5seconds. Throughput counts exact matched54/55/61 responses and peer private-chat deliveries; context/login are excluded. Raw timelines: `evidence/milestone-2-7/authenticated-active-counts.csv` and `protocol-N.jsonl`.

## Handler processing milliseconds

These timings exclude queue/network/rendering. Percentiles use nearest-rank with sample counts; sparse p99 values are descriptive order statistics, not reliable population tail estimates.

| Clients | Cmd | n | p50 ms | p95 ms | p99 ms | max ms |
|---:|---:|---:|---:|---:|---:|---:|
| 1 | -1 | 31 | 0.244 | 0.315 | 0.335 | 0.335 |
| 1 | 50 | 15 | 0.399 | 0.510 | 0.510 | 0.510 |
| 1 | 54 | 15 | 0.198 | 0.261 | 0.261 | 0.261 |
| 1 | 55 | 16 | 0.134 | 0.232 | 0.232 | 0.232 |
| 1 | 61 | 32 | 0.268 | 6.796 | 7.154 | 7.154 |
| 3 | -6 | 48 | 0.147 | 0.290 | 1.806 | 1.806 |
| 3 | -1 | 93 | 0.219 | 0.409 | 1.344 | 1.344 |
| 3 | 50 | 45 | 0.416 | 0.883 | 1.235 | 1.235 |
| 3 | 54 | 45 | 0.269 | 0.587 | 1.112 | 1.112 |
| 3 | 55 | 48 | 0.193 | 0.581 | 0.906 | 0.906 |
| 3 | 61 | 96 | 0.155 | 0.391 | 0.786 | 0.786 |
| 5 | -6 | 80 | 0.129 | 0.599 | 2.408 | 2.408 |
| 5 | -1 | 155 | 0.158 | 0.642 | 1.138 | 1.302 |
| 5 | 50 | 75 | 0.398 | 0.851 | 2.019 | 2.019 |
| 5 | 54 | 75 | 0.338 | 1.084 | 2.486 | 2.486 |
| 5 | 55 | 80 | 0.259 | 0.457 | 0.943 | 0.943 |
| 5 | 61 | 160 | 0.123 | 0.280 | 0.844 | 0.851 |
| 10 | -6 | 160 | 0.109 | 0.330 | 1.299 | 3.236 |
| 10 | -1 | 310 | 0.133 | 0.697 | 1.968 | 6.464 |
| 10 | 50 | 150 | 0.449 | 1.767 | 5.357 | 6.676 |
| 10 | 54 | 150 | 0.382 | 3.068 | 11.404 | 13.222 |
| 10 | 55 | 160 | 0.339 | 3.420 | 8.854 | 9.492 |
| 10 | 61 | 320 | 0.105 | 0.449 | 0.916 | 1.941 |

## Correlated response RTT milliseconds

RTT is measured with the protocol client monotonic clock from send to exact matching response. It includes the owned SSH tunnel and network. Context -1 can only match its acknowledgement because operations are sequential; movement matches account/coordinates/facing, park chat unique marker, farm response owner. State-cache polling is excluded. Private-chat peer delivery is separate in summary JSON and is not sender RTT.

| Clients | Cmd | n | p50 ms | p95 ms | p99 ms | max ms |
|---:|---:|---:|---:|---:|---:|---:|
| 1 | -1 | 31 | 51.827 | 94.019 | 107.479 | 107.479 |
| 1 | 54 | 15 | 28.307 | 115.363 | 115.363 | 115.363 |
| 1 | 55 | 16 | 29.593 | 112.137 | 112.137 | 112.137 |
| 1 | 61 | 32 | 58.102 | 124.401 | 136.999 | 136.999 |
| 3 | -1 | 93 | 52.085 | 123.416 | 176.434 | 176.434 |
| 3 | 54 | 45 | 33.367 | 112.378 | 132.609 | 132.609 |
| 3 | 55 | 48 | 57.438 | 108.970 | 117.479 | 117.479 |
| 3 | 61 | 96 | 54.208 | 146.156 | 189.350 | 189.350 |
| 5 | -1 | 155 | 53.817 | 157.443 | 527.442 | 527.539 |
| 5 | 54 | 75 | 57.950 | 147.072 | 164.572 | 164.572 |
| 5 | 55 | 80 | 55.118 | 142.384 | 443.785 | 443.785 |
| 5 | 61 | 160 | 40.382 | 73.517 | 118.893 | 127.582 |
| 10 | -1 | 310 | 54.578 | 135.470 | 158.176 | 160.158 |
| 10 | 54 | 150 | 60.758 | 99.256 | 145.244 | 152.326 |
| 10 | 55 | 160 | 61.946 | 130.346 | 157.200 | 157.948 |
| 10 | 61 | 320 | 56.853 | 96.405 | 138.977 | 158.709 |

## Resource and integrity observations

| Clients | JVM CPU % one core | Heap max MiB | RSS max MiB | GC count/ms | Java threads max | Pool active/wait max | DB lock waits/ms delta | Host load1 max | Available MiB min |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 1.55 | 18.33 | 115.56 | 0/0 | 67 | 0/0 | 0/0 | 0.50 | 2465 |
| 3 | 2.98 | 15.34 | 116.85 | 0/0 | 72 | 0/0 | 0/0 | 0.55 | 2456 |
| 5 | 4.24 | 16.34 | 119.71 | 0/0 | 77 | 0/0 | 0/0 | 0.61 | 2460 |
| 10 | 5.84 | 22.34 | 117.66 | 0/0 | 85 | 0/0 | 0/0 | 1.01 | 2456 |

Detailed OS threads, JVM IO, GC, pool, host disk deltas, production sampled CPU/RSS and DB query counters are in `protocol-summary.json`, `jvm-metrics.csv`, `server-host-metrics.jsonl`, `m27-db-queries.jsonl`. Counters include observer queries; performance_schema=OFF, so no per-query latency is claimed. Production /proc IO permission denial is preserved. Host disk is shared, so its bytes are not attributable exclusively to staging. Production PID presence and active/restart state were checked throughout; no unrelated workload SLO was measured.

The20 player/farm rows match the initial pre-run snapshot, ignoring only revision counters. Each successful level also has before/after snapshots. Final cleanup removes exactly nine owned friend-fixture rows after JVM writers stop and retains backups outside Git. No sale/harvest/reward/minigame operation was generated.

## Failed attempts retained

- Native5 stopped on71 unchanged reverse movement; held-key preparation later lost authentication before input. No native10 run.
- Initial state transport delayed valid samples; persistent SSH tail replaced sequential polling. This failed window is excluded.
- Pre-fix three-client load exposed ConcurrentModificationException during park join serialization; deterministic regression and membership snapshot fix followed.
- First five-client simultaneous-login burst exhausted Hikari pool4 for~30seconds: loadData holds one connection while loadFarmData requests another. The final ramp serializes login without changing pool size or response deadlines. This backend burst-login defect remains unresolved.
- All final1/3/5/10 levels use the same final artifact; earlier failed/passing windows are not spliced into these results.

Reconnect evidence consists of real socket close between levels and new authenticated session generations on later levels. No reconnect-under-load or native rendering acceptance is claimed. Historical client95 disconnect remains unresolved.
