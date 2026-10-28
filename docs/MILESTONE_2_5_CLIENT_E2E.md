# Milestone 2.5 — native client E2E, 2026-10-28

[Chắc chắn] Các ca dưới đây dùng Avatar 2.5.8 trên Android clones5556/5558, synthetic71/92 và fresh93, isolated port19129/DB13317. Final JAR SHA256 `2c93ede1f6082474b7ab3a419301e9542e5969e745cf961322f71d3e8019f2bf`. Không dùng kết quả artifact cũ để nâng acceptance.

## Evidence provenance

Ảnh acceptance nằm trong [final/](evidence/milestone-2-5/final/). Ảnh ở thư mục cha và `diagnostic-old-artifact-db.tsv` là chẩn đoán artifact trước, không phải acceptance final. [Packet export](evidence/milestone-2-5/final/packets.txt) chỉ chứa whitelist gameplay của authenticated sessions; không xuất login/password. Export giữ nguyên thứ tự trong từng log, gồm trường hợp người kiểm tra bấm từ chối trước khi gửi lại lời mời thành công. Không gọi lời mời đầu tiên là accepted.

## Friends: fresh UI acceptance, restart, UI delete, restart

71/92 đăng nhập; xóa quan hệ cũ qua UI, DB rỗng. 71 gửi lời mời qua menu nhân vật. Lần đầu bấm phím F ảo đã gửi `-19 0000004700` (reject); các ảnh `friend-new-invite.png` và `friend-incoming.png` thuộc lần này. Gửi lại qua UI, 92 dùng phím chọn nhận lời: `-19 0000004701`. Server trả -19 với accepted=true cho cả hai; [friend-accepted.png](evidence/milestone-2-5/final/friend-accepted.png) hiển thị “Đã kết bạn với stage71”. SQL quan sát ngay sau đó có đúng canonical `(71,92,2026-10-28 04:13:58 UTC)`.

Hai client hiển thị online: [71](evidence/milestone-2-5/final/friend-list-71.png), [92](evidence/milestone-2-5/final/friend-list-92.png). Graceful restart JVM349580→350581 cùng final JAR, reconnect và cả hai vẫn có quan hệ: [71](evidence/milestone-2-5/final/friend-list-after-restart-71.png), [92](evidence/milestone-2-5/final/friend-list-after-restart-92.png).

Sau các ca farm/security, 71 xóa qua menu native; [empty UI](evidence/milestone-2-5/final/friend-deleted-ui.png), DB count0. Graceful restart351182→352392 chạy trực tiếp JAR, reconnect: [empty71](evidence/milestone-2-5/final/friend-empty-after-restart-71.png), [empty92](evidence/milestone-2-5/final/friend-empty-after-restart-92.png), DB count0. Packet -81 vẫn decode `int listId, byte page, short index, byte action`; ví dụ `0000000101000002` = list1,page1,index0,visit2. Không đổi parser trong M2.5.

Sau khi kết thúc acceptance này, phase tải tạo quan hệ fixture riêng và ghi rõ trong load report; các fixture đó không phải bằng chứng lời mời UI.

## Private chat with new authenticated sessions

| Thời điểm | Request authenticated | Response recipient | UI |
|---|---|---|---|
| Trước restart |71→92, `M25_FINAL_71_PRE`|recipient92: sender71/name stage71|[pre](evidence/milestone-2-5/final/chat-before-restart.png)|
| Sau restart/reconnect |71→92, `M25_FINAL_71_POST`|recipient92: sender71/name stage71|[post71](evidence/milestone-2-5/final/chat-post-71-to-92.png)|
| Sau restart/reconnect |92→71, `M25_FINAL_92_POST`|recipient71: sender92/name stage92|[post92](evidence/milestone-2-5/final/chat-post-92-to-71.png)|

Wire -6 recipient payload bắt đầu int sender, UTF name, UTF text: `00000047 0007 73746167653731` cho71 và `0000005c 0007 73746167653932` cho92. Native font vẽ dấu gạch dưới gần giống khoảng trắng; packet giữ underscore. Handler context là ParkMsgHandler trên session mới đã xác thực; không dùng Admin1/login announcements làm bằng chứng chat. Export lọc các tin có marker M25, giữ request/response hai phía.

## Friend farm lifecycle and persistence

| Case | Actual UI | Wire owner61 |
|---|---|---|
|71→online92|[online92](evidence/milestone-2-5/final/farm-online-92.png)|92 (`0000005c`)|
|92 disconnect,71→offline92|[offline92](evidence/milestone-2-5/final/farm-offline-92.png)|92|
|Restart/reconnect71→92|[after restart92](evidence/milestone-2-5/final/farm-after-restart-92.png)|92|
|Reconnect92→71|[reverse71](evidence/milestone-2-5/final/farm-reverse-71.png)|71 (`00000047`)|

Mỗi ảnh dựng cảnh farm, bảng tên đúng owner. Context farm bootstrap native51/54 và JOIN61; không có `context=FarmMsgHandler cmd=55` trong export các final E2E logs. Native bootstrap61/self trước61/selected-owner là chủ ý của fix M2.4. Không thấy EOF/loading timeout trong các ca render này; kiểm tra source/unit giữ guard park55.

[DB invariants](evidence/milestone-2-5/final/db-invariants.txt) đối chiếu private backup trước E2E03:46UTC với sau toàn bộ chức năng: toàn bộ gameplay fields của farm71/92 giống nhau; player currency/chests/scores/date không đổi. Farm71 stock215×195/tree0/anchor1793123779887;92 stock[]/tree180/anchor0. Revision tăng hợp lệ do logout/save (71farm24→35,92farm16→26), không coi revision tăng là corruption. Snapshot [before](evidence/milestone-2-5/final/farm-before.tsv)/[after](evidence/milestone-2-5/final/farm-after.tsv) hẹp hơn, bắt đầu sau online visit đầu tiên; backup comparison bổ sung phần trước đó. Inventory60 thuộc visitor, owner selection không thay tài khoản đang xác thực.

## Unauthorized harvest: UI unreachable, authenticated server negative PASS

Native client `go.m()` so sánh local player ID với farm owner `go.b`, return ngay khi khác nhau, trước khi tạo action12 gửi85. [Visitor menu](evidence/milestone-2-5/final/visitor-menu-no-harvest.png) không có harvest của owner. Vì thế không báo native UI unauthorized85 đã chạy.

Fixture ngoài JAR kiểm tra cwd, loopback19129, isolated catalog/13317, sales property OFF; chỉ khi71 thực sự authenticated và viewedFarmId92 từ UI mới dispatch empty Message85 qua FarmMsgHandler trên chính session đó. Đây là **authenticated server-dispatch injection**, không phải TCP packet capture85. Log `STAGING_AUTH_NEGATIVE PASS DB unchanged including revisions`; [real-client rejection dialog](evidence/milestone-2-5/final/security-authenticated-rejection.png). Snapshot toàn bộ hai farm và currency/inventory/attendance/revision hai players giữ nguyên. Logout/restart tiếp theo cũng không ghi thêm product vào visitor hay giảm tree/currency owner.

## Fresh attendance93 and scope

Fresh synthetic93: scores0,chests[],attendance_dateNULL. Fixture tạo account ban đầu thiếu level_main làm native login gặp division-by-zero; chỉ sửa dữ liệu fixture93 theo level1 của template, không reset attendance71/92 và không thay production code cho lỗi fixture này.

[World map → map25 tự nhiên](evidence/milestone-2-5/final/attendance-natural-map25.png), trace50 prefix1900. Sau đó fixture chỉ đặt live93 gần NPC850 tại90,65; không có persisted location fields bị sửa. Qua menu NPC thật: [claim success](evidence/milestone-2-5/final/attendance-success-93.png), scores0→20, ticket593×1, date2026-10-28. [Duplicate UI](evidence/milestone-2-5/final/attendance-duplicate-93.png) bị từ chối, reward không tăng.

Restore marker khôi phục live placement trước stop351182; direct final-JAR restart352392 vẫn giữ20/593×1/date. Không đổi clock. **Complete natural-route attendance chưa được chứng minh**: natural map25 và NPC fixture interaction là hai phạm vi riêng. Không dùng ảnh map25 làm bằng chứng tự đi tới NPC rồi claim. Không khẳng định có thêm UI duplicate93 sau restart; restart persistence được kiểm bằng SQL.

## Minimal compatibility fix

M2.5 phát hiện park entry gửi weather2: native `ir.a(2)` tạo `ba(2,0)` với vector rỗng; update timer xóa phần tử0 gây AIOOBE liên tục và điều khiển treo. `Zone.enter` gửi weather-1 để clear effects theo decoder native. Một regression mới chứng minh -1 thay2 và park vẫn add player; RED trước fix, GREEN sau fix. Không đổi packet ID/shape, không rewrite weather subsystem.
