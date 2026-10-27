# Performance and stability

No verified player-capacity claim. No real player sessions have been measured.

Baseline homelab:3880MiB total RAM,2874MiB available,99MiB swap used,239GiB free HDD. After package installation, observed2793MiB available and119MiB swap used; timing/workloads differ, so this is not attributed solely to MariaDB. One later load sample0.07/0.05/0.01.

Initial limits: Java Xms128MiB/Xmx512MiB; cgroup MemoryMax768MiB, TasksMax256, CPUWeight20. DB pool10 max/2 idle. New MariaDB buffer pool expected128MiB, pending direct SQL measurement. These are conservative starting settings, not benchmark results.

Stages: one real client functional first;3–5 clients next;10 only after stability.20 is optional and not attempted.20-session connection cap is a resource safeguard, not a20-player guarantee. Raw TCP/handshake tests are not active players.

During real tests collect `systemctl show avatar2d -p MemoryCurrent -p TasksCurrent -p CPUUsageNSec`, `ps -L -p MAINPID`, `free -m`, `vmstat 1 30`, `cat /proc/loadavg`, and `iostat -xz 1 30` if already available. Root DB diagnostics: SHOW GLOBAL STATUS for Threads_connected/Threads_running/Slow_queries; SHOW VARIABLES for innodb_buffer_pool_size. Do not enable shared global slow logging blindly. JVM GC/latency metrics and per-action response/disconnect rates remain unmeasured.

Stop load if unrelated endpoints degrade, swap-in/out rises persistently or memory approaches limits. NPC/boss threads and HDD database latency are plausible constraints, not measured bottlenecks. Existing on-demand crawler jobs must not be started for benchmarking. User manager already has two failed units (bulk-vercel-build-bd7facba.scope, novel-ingestion-e2e-app.service); do not reset or restart them as part of Avatar work.

## Verified runtime snapshot after backup/restart

2026-10-27: Java service NRestarts=0, MemoryCurrent102584320 bytes (~97.8MiB),87tasks, CPUUsageNSec10848169000 cumulative startup CPU. MariaDB buffer pool128MiB,4connections/1running, Slow_queries0. These are startup snapshots, not a gameplay/load benchmark or CCU claim. All8 baseline user services remained running; system failed units0. Raw sanitized evidence: evidence/homelab-finish-verification.log and evidence/homelab-runtime.txt.
