# Milestone 2.5 — final report, 2026-10-28

[Chắc chắn] **Milestone2.5 tổng thể PARTIAL**: functional acceptance đạt trong phạm vi fixture đã nêu; tải1/3 có cửa sổ native hoạt động trên5phút, mức5 giữ auth322giây nhưng vòng UI chưa đủ tin cậy, mức10 dừng khi auth10→8. **Chưa xác nhận10 active CCU**; p95 chưa đo được. Không dùng số kết nối để thay acceptance hoạt động.

[Chắc chắn] Fresh friend UI acceptance/delete xuyên restart, chat mới hai chiều sau reconnect, online/offline/restart/reverse friend farm và authenticated server authorization negative đã có bằng chứng trên cùng final artifact. Attendance fresh93 nhận đúng reward và giữ qua restart, trong phạm vi natural map25 + NPC spawn fixture riêng. Kết quả tải được ghi riêng trong [load report](LOAD_TEST_REPORT.md); không suy diễn production capacity từ test staging.

## Git, build and artifact

Worktree `/Volumes/sontc/sources/others/avatar2d-gameplay-migration`, branch `codex/gameplay-migration`, baseline HEAD `5076fb65d9d3c0080994a8315a153e9639e76d42`. Delivery commit: `git log -1 --format=%H -- docs/MILESTONE_2_5_FINAL_REPORT.md` (commit chứa báo cáo này). Original dirty worktree `avatar2d` không sửa. Không push/merge/deploy production.

Corretto17.0.18 build. `target/Avatar2D-1.0-SNAPSHOT.jar` và isolated remote `m25-final2.jar` có cùng SHA256:

`2c93ede1f6082474b7ab3a419301e9542e5969e745cf961322f71d3e8019f2bf`

Các artifact trước final2 chỉ là chẩn đoán. JVM349580,350581,351182,352392 đều dùng final2;351182 có fixture ngoài JAR. JVM353561 dùng bootstrap metrics ngoài JAR cho tải; không sửa JAR, không fixture position/security trong tải.

## Scope and tests

Fix duy nhất: `Zone.enter` gửi weather-1 thay weather2 để clear native effects và tránh native2.5.8 empty-vector timed-effect AIOOBE làm treo điều khiển. Thêm regression park entry -1, vẫn add player. Không thay social parser, farm writer, packet format, schema, giá hay replay policy.

Baseline61 và regression mới được chạy: **62 unique PASS =35 unit +27 isolated DB**,0fail,0error,0skip trong tập kết quả executed hợp nhất. [Per-test manifest](evidence/milestone-2-5/final/test-manifest.tsv). Package mặc định chạy35unit và skip27DB opt-in; chạy DB riêng rồi chỉ đếm mỗi testcase một lần:

- `mvn -q package` với Corretto17.
- Sales/friendship25: `-Davatar.test.isolatedSalesDb=true -Dtest=FarmSalePersistenceTest,FriendshipPersistenceTest test`.
- Farm/attendance2: `-Davatar.test.isolatedFarmDb=true -Dtest=FarmPersistenceTest,AttendancePersistenceTest test`.

Private config trỏ disposable `avatar2d_sales_test` và `avatar2d_starfruit_test` trên13317. Backup trước reset duy nhất test catalog; không chạy persistence tests vào gameplay/prod. Private logs `/tmp/avatar2d-m25/final-{package,sales,farm}.log`; RED weather regression ở `weather-red.log`, sau fix GREEN. Giữ coverage park55/farm, stale writer, atomic rewards, rollback/replay và unauthorized visitor.

## Functional acceptance

| Gate | Kết quả | Evidence |
|---|---|---|
|Fresh invite71→92 và actual UI accept|PASS|native-21/-19 accepted01, canonical71/92, cả hai UI lists|
|Friend restart/reconnect, UI delete, restart empty|PASS|hai bộ screenshots lists và empty; SQL count0 trước load fixtures|
|Private chat before restart và new messages hai chiều sau reconnect|PASS|authenticated-6 đúng sender71/92,name,payload,recipient UI; không Admin1|
|Friend farm online/offline/restart/reverse|PASS|render đủ, owner61/name đúng, native bootstrap, không farm-context park55|
|Unauthorized harvest UI|UNREACHABLE by native owner check|visitor menu không có harvest; không giả injection thành UI85|
|Authenticated server negative85|PASS|real visitor session, rejection UI, exact DB snapshots unchanged, no product after logout|
|Fresh attendance93|PASS within stated fixture scope|+20,593×1,date,duplicate UI rejected,restart SQL unchanged|
|Complete natural-route attendance|NOT PROVEN|chỉ natural route map25 được chứng minh; NPC interaction dùng live spawn fixture|

[Client E2E report](MILESTONE_2_5_CLIENT_E2E.md) có ảnh, exact wire examples, chronology, fixture limitations và DB comparison. [Packet export](evidence/milestone-2-5/final/packets.txt), [DB invariants](evidence/milestone-2-5/final/db-invariants.txt), [observations](evidence/milestone-2-5/final/functional-observations.txt).

Pre-E2E backup so với post-functional: farm71/92 giữ toàn bộ gameplay fields, player currencies/chests/scores/date giữ nguyên. Revision tăng do save hợp lệ. Visitor71 stock215×195/tree0 và owner92 stock[]/tree180 vẫn tách biệt. Attendance93 là fresh synthetic, không reset71/92 hay production clock.

## Isolation, sales and preservation

Server loopback19129; isolated MariaDB13317, buffer32MiB/max12, JVM Xmx128MiB/2processors. Private backups và DB directories được giữ. Native63 live **OFF**: không đặt `avatar.farm.sales.catalog`, không thay sales code/prices/policy. Fixtures guard cwd/endpoint/catalog/JDBC và salesOFF; position fixture93 đã restore marker trước restart; class ngoài artifact.

Production read-only identities: Java313665, MariaDB302726, systemd avatar2d active/NRestarts0; không sửa credential/permission/schema hay restart production. Safe teardown và kiểm tra cuối được ghi trong [cleanup evidence](evidence/milestone-2-5/cleanup.txt).

## Remaining scope

Không gọi broader migration complete. Backlog NPC,19 farm commands,minigames,economy,housing,marriage vẫn theo [migration matrix](GAMEPLAY_MIGRATION_MATRIX.md). Complete natural-route claim, repeated standalone own-farm load và production capacity không được suy diễn từ các kết quả trên. Tải5/10 cần sửa/kiểm chứng driver và trạng thái client trước một lần chạy mới; không coi auth plateau là active acceptance. Không cutover.
