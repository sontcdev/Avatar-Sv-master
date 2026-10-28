# Milestone 2.4 — friend farm loading, 2026-10-28

[Chắc chắn] Đã sửa hai nguyên nhân độc lập của EOF/loading và chứng minh farm riêng, friend online/offline, đảo vai bằng client thật trên một JAR. Acceptance toàn milestone vẫn **PARTIAL** do các ca UI còn thiếu dưới đây; không cutover hay load test.

## Provenance

Workspace `/Volumes/sontc/sources/others/avatar2d-gameplay-migration`, branch `codex/gameplay-migration`, baseline clean `37c89dd06c31626c1b36c1857f12733962b972ba`. Delivery commit là commit chứa báo cáo này: `git log -1 --format=%H -- docs/MILESTONE_2_4_FINAL_REPORT.md`. Không push/merge production.

Corretto17.0.18: `/Volumes/sontc/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home`. Final `target/Avatar2D-1.0-SNAPSHOT.jar` SHA256:

`ce491e4c159591c540128f1cea38e9e3b373766fa257949f6265cf5b0159f268`

Remote `/home/sontc/avatar2d-gameplay-stage/m24-final3.jar` trùng SHA; JVM344088 chạy trực tiếp, JVM345749 và346677 restart cùng JAR qua fixture ngoài classpath chỉ nhắm synthetic92. Ảnh final/chat/social/attendance M2.4 đều cùng SHA này. Baseline và final1/final2 là chẩn đoán, không dùng để nâng acceptance final. Fixture71 attendance-repeat ở final2 bị loại khỏi acceptance này.

## Root cause và sửa tối thiểu

[Phân tích byte và stack](FRIEND_FARM_EOF_ROOT_CAUSE.md): session farm vẫn nhận park55, native farm image decoder đọc sender2000000001 thành imageCount30517 và hết buffer29 byte. TCP vẫn còn hoạt động. Một lỗi thứ hai là gửi JOIN61 trước native resource bootstrap; chỉ trì hoãn/chuyển owner của preload cũng chưa dựng farm vì cờ native `bf` cònfalse. Fix rời park trước ACK, giữ selected owner riêng session, đợi native61/self rồi phát61/friend một lần. Không thêm padding, thay layout hay nuốt EOF.

Affected: `Session.java`, `ParkService.java`, `FarmService.java`; regression `FarmVisitTest.java`, `FriendshipPersistenceTest.java`. Trace whitelist51/55/56 opt-in, defaultOFF, authenticated-only,prefix512. Không đổi schema/writer/giá/sales63. Native-81 giữ `int listId,byte page,short index,byte action`.

## Test verification

Baseline **60 unique PASS (33unit+27DB)**; final **61 unique PASS (34unit+27DB)**,0fail,0skip trong isolated execution đầy đủ. [Manifest](evidence/milestone-2-4/test-manifest.tsv). Final `mvn -q package` chạy34unit,27DB opt-in skip theo mặc định; DB được chạy riêng và manifest lấy kết quả executed, không đếm skip/rerun. Sales25: `FarmSalePersistenceTest,FriendshipPersistenceTest`, `-Davatar.test.isolatedSalesDb=true`; farm/attendance2: `FarmPersistenceTest,AttendancePersistenceTest`, `-Davatar.test.isolatedFarmDb=true`; private `avatar.database.config` trỏ disposable catalogs trên MariaDB13317. Catalog test được backup trước reset; không chạy trong gameplay/prod. Raw logs private `/tmp/avatar2d-m24/final3-{package,sales,farm}.log`, reports `final3-reports/`. Fresh delivery `mvn -q test` chạy lại34 PASS và27 opt-in SKIP; không cộng thêm vào61.

Regression RED park-context được giữ trong evidence; GREEN trên final. Existing stale writer/concurrent/replay/rollback/uncertain-commit/visitor rejection được giữ nguyên. Không giả unit/packet test là real-client E2E.

## Same-artifact acceptance

| Flow | Trạng thái | Phạm vi đã chứng minh |
|---|---|---|
| Own71 | WORKING | Farm render, đúng stage71 sau restart |
|71→92 online/offline | WORKING | Render đúng stage92, client71 reconnect trước offline case |
|92→71 online | WORKING | Render đúng stage71 |
| Visitor unauthorized starfruit E/F | PARTIAL | Guard unit PASS; chưa actual85 từ UI, không gọi DB snapshot là sau unauthorized attempt |
| Farm reconnect/restart G | PARTIAL | Offline visit sau reconnect, own sau restart; friend visit sau restart chưa lặp |
| Attendance fixture92 | WORKING trong fixture scope | NPC UI,-61/-59,+20scores,593×1,date,repeat và restart rejection |
| Natural navigation map25 | WORKING trong route scope | World map Nông trại →context10/bootstrap→50map25,không fixture71; chưa claim tự nhiên |
| Private chat | PARTIAL | Hai chiều actual-6 và render đúng ID/name; reconnect repeat còn thiếu |
| Friends | PARTIAL | Invite,-81 list online/offline,delete,empty UI+DB sau restart; accept mới trên final chưa chứng minh |
| Native63 | Live BLOCKED/OFF | Không đổi replay policy/prices,không dùng cooldown/hash |

[Ảnh, wire, DB, restart và hạn chế](FRIEND_FARM_E2E_EVIDENCE.md). DB71 giữ stock195item215/tree0/anchor1793123779887;92stock[]/tree180/anchor0. Getter theo thời gian có thể hiển thị180 ở cây71, không phải persisted overwrite. Attendance92 scores0→20,ticket593quantity1,date2026-10-28; repeat/restart giữ nguyên reward. Quan hệ fixture71/92 đã xóa qua UI,DB count0 sau restart. Không dùng tên snapshot cũ hoặc lời mời làm bằng chứng accept.

## Persistence, production và load gate

Không có persistence writer mới. Join chỉ đọc online owner dưới lock hoặc load snapshot offline bằng cùng serializer; inventory60 vẫn của visitor. Touched social action đổi dispatch,không thay SQL accept/delete. Existing player/farm writers tiếp tục expected revision,atomic transaction,fail-closed rejection. Logout revisions tăng hợp lệ; không merge snapshot stale.

Staging loopback19129,isolated MariaDB13317(32MiB,max6),forward19128 và13317,hai Android clones read-only5556/5558. Backups private `m24-before.sql` và `m24-test-catalogs.sql` giữ nguyên. Fixture92 kiểm tra cwd/ports/catalog/JDBC URL/sales guard, giữ vị trí live theo object identity,restore marker trước restart; không packaged vào JAR. Original dirty checkout `avatar2d` không sửa.

Production read-only checks vẫn Java313665,systemd `NRestarts=0`,MariaDB302726; không stop/restart,SQL write,migration hoặc config production. Bằng chứng shutdown cuối được ghi riêng trong [cleanup](evidence/milestone-2-4/cleanup.txt).

Load test **chưa chạy** vì acceptance chưa đủ. Kế hoạch sau gate: 1→3→5→10 authenticated clients trên isolated staging,cùng JAR; mỗi nấc kiểm tra login/own/visit/chat/attendance repeat rồi đo5phút throughput,p95latency,heap/GC,threads,DBconnections/locks và protocol errors. Chỉ tăng nấc khi không EOF/loading,không disconnect ngoài dự kiến,không thay sai reward/owner/revision. Dừng ngay khi lỗi integrity; giữ trace và DB snapshot. Đây là kế hoạch có điều kiện,không phải kết quả tải hay chứng minh production capacity.
