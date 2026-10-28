# Friend farm — bằng chứng client thực M2.4

[Chắc chắn] Tất cả ảnh `final-*`, chat, friends và attendance trong thư mục M2.4 dùng cùng JAR SHA256 `ce491e4c159591c540128f1cea38e9e3b373766fa257949f6265cf5b0159f268`, Avatar2.5.8 chạy trong J2ME Loader Android, tài khoản staging71/92. MIDlet chỉ đổi endpoint loopback19128; không thay decoder. Trace `QUEUED` chỉ chứng minh enqueue; ảnh mới chứng minh render.

## Farm acceptance

| Ca | Kết quả | Bằng chứng |
|---|---|---|
| A:71 farm riêng | PASS sau restart cùng JAR | [Ảnh](evidence/milestone-2-4/final-71-own-farm.png), [trace restart](evidence/milestone-2-4/restart-gameplay-packets.log),61/self71 |
| B:71 thăm92 online | PASS | [Ảnh](evidence/milestone-2-4/final-71-visits-online-92.png),-81=`0000000101000002`,61 owners71→92 |
| C:71 thăm92 offline | PASS sau force-stop92 và reconnect71 | [Ảnh](evidence/milestone-2-4/final-71-visits-offline-92.png), offline list và61 owner92 |
| D/H:đảo vai92 thăm71 online | PASS | [Ảnh](evidence/milestone-2-4/final-92-visits-online-71.png),61 owners92→71 |
| E/F:visitor harvest bị chặn, owner không đổi | PARTIAL | Unit guard PASS; chưa capture được actual85 từ UI visitor. DB giữ nguyên stock/anchor, nhưng không được coi là sau một unauthorized request thực |
| G:reconnect/restart | PARTIAL | Reconnect71 thăm offline92 và own71 sau JVM restart PASS; chưa lặp lại friend visit sau restart |

[Trước visit](evidence/milestone-2-4/farm-before-visitor.tsv), [sau restart](evidence/milestone-2-4/db-after-restart.tsv): farm71 quantity0,anchor1793123779887,stock195 item215; farm92 quantity180,anchor0,stock[]. Revision tăng do save/logout; không có inventory/currency writer mới. Quantity hiển thị180 cho71 là getter tái sinh theo anchor cũ, không phải DB bị ghi lại180.

## A/B wire và decoder

[Root cause và bảng byte](FRIEND_FARM_EOF_ROOT_CAUSE.md). [Trace final](evidence/milestone-2-4/final-gameplay-packets.log) chứa native -81, resource bootstrap, online/offline/reverse. [Trace restart](evidence/milestone-2-4/restart-gameplay-packets.log) chứa own-farm bootstrap sau restart.

Own: context10 ACK →51(resource version) →55 metadata/56 definitions và54 requested images →60 inventory →native50/map25 và61/self →61 owner71 dài38. Chạm cổng FARM dùng preload đã có để dựng farm riêng. Friend: native-81 chọn owner →context10 ACK →cùng native bootstrap →61/self →61/selected owner ngay sau đó. Đợt cache nóng chỉ yêu cầu hai54; cache lạnh có thêm resource requests. Không đòi tất cả client có một danh sách image requests cố định. Payload ảnh lớn chỉ ghi prefix512 byte; không tuyên bố dump ảnh đầy đủ.

61 owner71:
`0000004706ffffffffffff000a0a000a002b002e00b400000000000001010101010100030005`

61 owner92:
`0000005c06ffffffffffff000a0a000a002b002e00b400000000000001010101010100030005`

Không có thay đổi counts/conditional fields/signed widths giữa hai serializer. Native decoder đọc đúng38 byte của fixture sáu ô trống, zero animal. EOF29 byte park55 được tái hiện trong regression; không sửa layout61 hay catch-and-ignore.

## Navigation

[Chắc chắn] Đường tự nhiên: world map mặc định “Khu giải trí” →RIGHT “Khu mua sắm” →DOWN “Nông trại” →FIRE. [Mục được chọn](evidence/milestone-2-4/natural-worldmap-farm-selection.png), [map25](evidence/milestone-2-4/natural-map25.png). Client tự chọn context10,bootstrap51/54/60,sau đó gửi50=`1900ffffffff`,61=`00000047`. Không dùng fixture cho71 trong JVM restart này; fixture chỉ nhắm92 và đã restore. “Khu ngoại ô” là map17 (`11ffffffffff`), không phải25. Cổng FARM trên map25 mở farm24 đã preload. NPC850 nhìn thấy trong map25; tương tác điểm danh dưới đây được ghi nhận riêng là fixture, chưa dùng ảnh natural map làm bằng chứng claim tự nhiên.

## Same-artifact social/attendance

- Chat hai chiều thật: [71 nhận92](evidence/milestone-2-4/chat-71-receives-92.png), [92 nhận71](evidence/milestone-2-4/chat-92-receives-71.png). Actual-6 chứa sender71/stage71 và92/stage92; không tính Admin welcome. Reconnect repeat còn chưa có bằng chứng mới.
- Invite UI thật: [92 nhận lời mời](evidence/milestone-2-4/friends-invite.png), actual-21. Không có-19 accept ở lần này; không gọi accept PASS dựa trên dữ liệu fixture.
- List online/offline: final trace -81 chứa đúng71/92 và Online/Offline. Delete -81=`0000000101000001`, [rỗng sau xóa](evidence/milestone-2-4/friends-empty-after-delete.png); [rỗng sau restart](evidence/milestone-2-4/friends-empty-after-restart.png), DB count0.
- Attendance92 fixture: [menu](evidence/milestone-2-4/attendance-npc-menu.png), [nhận thưởng](evidence/milestone-2-4/attendance-success.png), [repeat](evidence/milestone-2-4/attendance-repeat.png), [repeat sau restart](evidence/milestone-2-4/attendance-after-restart.png). [Trace claim](evidence/milestone-2-4/attendance-gameplay-packets.log), [trace restart](evidence/milestone-2-4/restart-gameplay-packets.log). Actual-61 target2000000850 và-59=`773597520000`; DB92 scores0→20,chests[]→593quantity1,dateNULL→2026-10-28. [Before](evidence/milestone-2-4/db-before-attendance.tsv), [after](evidence/milestone-2-4/db-after-attendance.tsv), [repeat](evidence/milestone-2-4/db-after-attendance-repeat.tsv), [restart](evidence/milestone-2-4/db-after-attendance-restart.tsv). Repeat giữ20,1vé,date. Fixture ngoài JAR,chỉ thay vị trí live92,map25/0 x90y65; restore trước shutdown/restart, không sửa vị trí DB.

## Error scope

[Baseline stack](evidence/milestone-2-4/baseline-client-errors.log) có EOF trong gb.d. [Log client cuối](evidence/milestone-2-4/final-client-errors.log) vẫn có lỗi Vector/main-loop cũ; không khẳng định mọi exception client đã được sửa. Farm đã render đúng trong ba ca online/offline/reverse; đó là phạm vi đã chứng minh.
