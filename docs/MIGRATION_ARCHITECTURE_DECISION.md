# Migration architecture decision

Ngày: 2026-10-28. Trạng thái: **chọn A, selective implementation; chưa cho production cutover**.

[Chắc chắn] Target thực tế là baseline local đã giữ ở commit `be61e59`, dựa trên upstream `d8d5f26688465724d9955dc5045e9545a0dba91b`. Worktree gốc không bị reset/clean. Các donor được ghim đúng SHA trong GAMEPLAY_MIGRATION_MATRIX.

## A — Tích hợp có chọn lọc

Giữ dispatcher/context, client2.5.8, session lifecycle, deploy và schema target; sửa tại service/User hiện có. Dùng donor làm tài liệu truy vết hành vi, viết độc lập khi chưa có license grant, không copy class/data. Đối chiếu byte layout với client trước khi bật response.

Bằng chứng ủng hộ:

- Target có các sửa deployment/session/farm quan trọng chưa có trong upstream; thay server sẽ làm mất giả định đã triển khai.
- Java17 target build/test đạt; social -6, invite -21 và harvest85 xác minh được bằng decoder client và packet tests mà không đổi framing.
- Farm persistence target một farm_data JSON row; thêm clock vào cùng upsert giữ được atomic harvest+stock, không cần scheduler/thread theo player. Phù hợp HDD và1–10users.
- Attendance dùng một conditional UPDATE `players(chests,scores,attendance_date)` thay vì reward rồi ghi ngày riêng. Ngày theo Asia/Ho_Chi_Minh; UNIQUE user identity/one active session vẫn là giả định target.
- Bổ sung2column additive, không rename/drop bảng hay đưa donor player IDs vào target.

Rủi ro còn lại: các path tiền/inventory legacy không có transaction đồng bộ toàn dự án; friends accept/delete, farm catalog/price, clan, minigame, auction và marriage còn thiếu contract/implementation/E2E. Các sửa này không tự biến toàn server thành transaction safe. Không hỗ trợ hai instance cùng ghi một DB; unique daily claim không bảo vệ toàn bộ inventory khỏi writer legacy ở instance khác.

## B — Chạy donor như server thay thế ở staging

[Chắc chắn] Primary donor ghim build package được bằng Java17 trong môi trường local, `mvn -q -DskipTests package`; đây là bằng chứng compile, **không có bằng chứng runtime với client target hoặc migrate player data**. Có nhiều implementation thật: FarmManager, quest families, ongame state, clans/procedures, AuctionManager. Cũng có stub (HomeService.doJoinHouse4, wizard dialog) và constants không có handler (stealing/store/finish-starfruit).

Chi phí/rủi ro lớn hơn:

- P User/model.Item/Npc/Board/LandCell/FarmProduct không cùng target `avatar.item.Item`, `BoardInfo`, `avatar.Farm.*`; target handler package `message`, P `handler`, context routing khác.
- Cùng command number có nghĩa theo context. Ví dụ63 ở target còn alias GET_CARD; farm phải là sell product. P friends dùng popup/customlist, không thể ghép với native response thiếu UTF.
- P farm catalog5tables và `farm_data(level_store,capacity_store,barn_size,pond_size,level_starfruit,lasted_get_starfruit,cooking,...)`; target và secondary có tên/shape/currency khác. Không có converter đã kiểm chứng cho players/chests/quests/clans/marry.
- NPC761 khác nghĩa (target cửa hàng, P MeowMeow); P shop783/888/889 không ánh xạ1:1. Port theo enum name hoặc integer đơn thuần làm sai cửa hàng/phần thưởng.
- P auction giữ bids RAM và charge/award không có durable settlement ledger đã chứng minh; compile được không loại bỏ crash/replay risk. Nhiều game/scheduler cần đo thread và memory trên T6670.
- Không tìm thấy license grant tại SHA ghim. Điều này chặn copy/deploy donor như replacement cho đến khi xác minh quyền.

## Quyết định và gate

[Có khả năng cao] A có phạm vi thay đổi và rollback nhỏ hơn trong môi trường hiện tại; căn cứ là các khác biệt source/schema/contract đã xác minh, chưa phải benchmark so sánh2server chạy cùng tải.

1. Mỗi tính năng: failing regression → sửa owner hiện có → packet test → DB failure/concurrency/replay → client thật → logout/restart.
2. Không seed catalog/giá/phần thưởng chưa kiểm chứng. Quy tắc hồi khế độc lập của nhánh: giữ local capacity180, hồi3/phút tối đa60phút; không tuyên bố đây là bảng level chuẩn của donor.
3. Staging loopback19129, DB instance riêng13317, DB avatar2d_gameplay_stage; không dùng script deploy production cũ. Fixture test avatar2d_starfruit_test ở cùng instance mới, không phải DB production.
4. Production backup chưa được thực hiện: SSH user không có quyền đọc deployment root hoặc sudo không tương tác. Vì vậy **không sửa schema, restart hay thay JAR production**. Backup staging không được quảng cáo là backup production.
5. Production cutover chỉ sau đủ client E2E cho tính năng được bật, backup production có restore test, artifact hash và rollback được kiểm tra; mọi blocker chưa giải quyết nằm trong MIGRATION_REPORT.
