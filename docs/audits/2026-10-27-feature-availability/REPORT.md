# Rà soát tính năng Avatar 2D — 2026-10-27

[Chắc chắn] Nhiều tính năng có giao diện nhưng backend chưa triển khai hoặc chỉ trả dữ liệu mẫu. Không có một cờ cấu hình chung để mở đầy đủ chúng. Có một số tính năng có điều kiện tài khoản thật (auto câu cá); không nên đánh đồng với handler trống.

## Phạm vi và baseline

- Repo `/Volumes/sontc/sources/others/avatar2d`, branch `master`, HEAD `d8d5f26`; audit **working tree hiện tại**, gồm các sửa trước đó chưa commit. Không reset/stash, không sửa source/config, không deploy.
- SHA-256 tập source Java + private-compat.sql (ghép đường dẫn, NUL, nội dung theo thứ tự): `00b6eb8fafed943fa46c668b58777088275959cdcbc19942ad5ae8415badb019`.
- Android standalone build 103 sử dụng client J2ME 2.5.8 và transport WSS. Lượt này tập trung backend dispatcher → handler → service/state → DB/response, không giải mã lại toàn bộ UI client.
- Đọc các dispatcher chung/farm/park/home/casino, menu NPC/menu nhân vật, dịch vụ tương ứng, đường mua nhà/bạn bè, dữ liệu NPC/farm và các test sẵn có. Minigame, quà và hôn nhân được khảo sát theo entry point; chưa kiểm toàn bộ luật chơi.
- SSH chỉ đọc: `avatar2d.service` active/running, `NRestarts=0`; JAR staging SHA-256 `c889aa1b3f991cf813e74ebce50d44f91ab74190fea890c4b6a42243b254818e`, khớp bản backend đã giao deploy. Không đọc được config DB bằng user SSH; **không xác minh lại toàn bộ dữ liệu production hoặc checksum JAR đang chạy**. Không dùng dữ liệu tài khoản thật để thử giao dịch.

## Bảng trạng thái theo tính năng

Mọi trạng thái “có xử lý” chỉ xác nhận đường code tồn tại, không có nghĩa đã nghiệm thu trên điện thoại.

| Nhóm | Trạng thái tại source hiện tại | Bằng chứng / finding |
| --- | --- | --- |
| Đăng nhập, nhân vật, di chuyển/đổi khu, chat khu | Có xử lý; login/di chuyển đã từng quan sát trong các lượt triển khai trước | MessageHandler, ServerManager.joinAreaMessage; không kiểm lại Android lần này |
| Đăng ký / Chơi mới / email | Chưa triển khai, trả thông báo dùng tài khoản đã cấp | AUD-012 |
| Kho farm, trồng/thu hoạch cây | Có handler và test; phụ thuộc danh mục farmitems | AUD-004 |
| Thu hoạch khế | Có lưu kho và chống nhận lặp; **chưa có chu kỳ mọc lại**, không bán được nông sản | AUD-003,005 |
| Mở đất | Có handler nhưng thiếu giá, giới hạn, kiểm quyền và loại tiền | AUD-001 |
| Mua vật nuôi | Có handler nhưng nhận loại bất kỳ, không thu tiền; không có vòng đời nuôi hoàn chỉnh | AUD-001,003 |
| Nấu ăn / thu hoạch món / nấu nhanh | Chỉ thông báo chưa hỗ trợ | AUD-003 |
| Tưới/bón/chữa bệnh, thông tin ô đất/vật nuôi, bán/thu hoạch vật nuôi | Lệnh dùng vật phẩm/thông tin/bán/thu hoạch chỉ báo chưa hỗ trợ | AUD-003; chưa giải mã từng subtype của USING_ITEM |
| Nâng khế, nâng đất/kho, mở rộng chuồng/hồ | Chỉ thông báo chưa hỗ trợ/đang xây dựng | AUD-003 |
| Ăn trộm, kho trộm, lịch sử | Chỉ thông báo chưa hỗ trợ/đang xây dựng | AUD-003 |
| Thăm farm người khác | Trả nhầm dữ liệu farm của chính mình | AUD-006 |
| Câu cá thường | Có kiểm vé/cần/mồi, hẹn giờ và nhận cá; chưa nghiệm thu trọn vòng Android | ParkService; test không chặn collector đã pass |
| Auto câu cá | Có xử lý nhưng khóa theo `users.ThuongNapLanDau` | [src/main/java/avatar/network/Session.java:1226](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:1226); không tự bật quyền nạp |
| Quần áo, shop, nâng cấp/đổi vật phẩm | Có đường xử lý và dữ liệu; chưa nghiệm thu từng món/giá | Session.buyItemShop, handleBossShop, UpgradeItemHandler |
| Quay số | Có luồng; miễn phí lỗi khi không có vé, kiểm tiền Xu không nhất quán | AUD-013 |
| Nhà, nội thất, rương nhà | Có xử lý một phần; mua nhà trừ tiền trước INSERT, một entry vào nhà chỉ log | AUD-002,010 |
| Hội nhóm, danh sách/kết bạn | Thiếu xử lý chính; menu hiện nhưng backend chưa hoàn chỉnh | AUD-007 |
| Tin nhắn riêng | Có handler nhưng lấy tên người nhận làm người gửi | AUD-007 |
| Hẹn hò/cưới | Có một phần, hardcode ID đám cưới, thiếu phản hồi khi người nhận không online | AUD-011 |
| Tài Xỉu NPC | Có cược/lịch sử/trả thưởng; chưa audit toàn bộ tính nguyên tử, restart giữa ván | NpcHandler.Than_Tai_Xiu, GlobalHandler, minigame/TaiXiu.java |
| Bầu cua/casino, Oan Tù Xì | Có giao thức/sườn, nhiều kết quả/số dư mẫu; chưa phải gameplay hoàn chỉnh | AUD-009 |
| Caro/Tiến lên/Phỏm/Kim cương/đua pet | Không thấy đường triển khai độc lập đầy đủ; không đánh giá chỉ dựa tên class rỗng | AUD-009; cần đối chiếu từng màn client trước khi bật |
| NPC thường | 13/28 NPC seed chỉ vào fallback không có chức năng riêng | AUD-008 |
| Đấu giá, điểm danh, dự đoán xổ số/Tài Xỉu | Trả bảo trì/đang xây dựng/Comingsion | AUD-008,012 |
| Mã giới thiệu, diễn đàn | Menu không có action | AUD-012 |
| Gift code | Có xử lý; mã sai/hết hạn trả false nhưng caller bỏ kết quả, dễ thấy bấm không phản hồi | AUD-014 |
| Du lịch, Biển citylo/Locity | Menu thứ 4 không có nhánh chọn; cửa Locity báo đang xây dựng | AUD-012 |

## Finding đã xác minh

### AUD-001 — P1 — Mở đất/mua vật nuôi nhận yêu cầu sai và không thu tiền

[Chắc chắn, tái hiện local] `FarmMsgHandler` nối OPEN_LAND/BUY_ANIMAL thẳng vào service. `openLand` chỉ đọc ID và typeBuy rồi thêm đất; không kiểm ID farm, quyền sở hữu, mức tối đa, giá hoặc tiền. `Buy_ANIMAL` thêm Animal với loại client gửi, trả toàn bộ số dư 0 mà không cập nhật tiền thật. Có thể tạo đất/vật nuôi ngoài quy tắc thay vì một giao dịch hợp lệ.

Bằng chứng: [src/main/java/avatar/service/FarmService.java:154](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/FarmService.java:154), [src/main/java/avatar/service/FarmService.java:268](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/FarmService.java:268), [src/main/java/avatar/message/FarmMsgHandler.java:46](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/FarmMsgHandler.java:46). Probe tài khoản giả có 0 Xu, OPEN_LAND ID=99999/type=127 vẫn thêm đất; BUY_ANIMAL loại=127/type=127 vẫn thêm vật nuôi. Không ghi DB thật.

Sửa tối thiểu: một bảng giá/quy tắc đã chốt, validate chủ farm/loại/giới hạn/tiền, lưu thành công rồi mới trả kết quả/số dư; khi chưa có quy tắc thì trả chưa hỗ trợ. Test thiếu tiền, loại sai, giới hạn, gửi lặp, lưu lỗi.

### AUD-002 — P1 — Mua nhà có thể mất 1.000.000 Xu khi INSERT lỗi

[Chắc chắn, chứng minh source; chưa gây lỗi DB thật] NPC Chủ đầu tư → `Session.BuyHouse` kiểm tiền rồi `updateXu(-1000000)` trước INSERT `house_buy`. SQLException chỉ in stack trace, không hoàn tiền và không phản hồi thất bại; số dư trong memory đã giảm, lần lưu tài khoản tiếp theo có thể ghi số dư giảm đó.

Bằng chứng: [src/main/java/avatar/handler/NpcHandler.java:490](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/NpcHandler.java:490), [src/main/java/avatar/network/Session.java:2028](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:2028), [src/main/java/avatar/model/User.java:615](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/model/User.java:615). Đây là mua **nhà**, khác với sửa mua **nội thất** ở lượt trước.

Sửa tối thiểu: đảm bảo giao dịch nhà + tiền nhất quán, hoàn trạng thái nếu lưu lỗi, phản hồi rõ ràng. Test INSERT thất bại, mua lặp và reconnect.

### AUD-003 — P2 — Vòng gameplay farm còn thiếu nhiều mắt xích

[Chắc chắn, source] 19 lệnh gom vào một thông báo chưa hỗ trợ: COOKING, HARVEST_COOK, NAU_NHANH, USING_ITEM, PRICE_ANIMAL, SELL_ANIMAL, HARVEST_ANIMAL, INFO_CELL, INFO_ANIMAL, UPDATE_STARFRUIL, UPDATE_STARTFRUIT_BYMONEY, GET_INFO_STARFRUIT, FINISH_STARFRUIT, UPDATE_LAND, UPDATE_STORE, STEAL_INFO, STEAL, STEAL_STORE, LICH_SU_BI_TROM. Ngoài nhóm này, mở rộng chuồng/hồ và ăn trộm qua REQUEST_FRIENDLIST chỉ trả đang xây dựng; bán nông sản chỉ trả chưa hỗ trợ.

Bằng chứng: [src/main/java/avatar/message/FarmMsgHandler.java:94](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/FarmMsgHandler.java:94), [src/main/java/avatar/message/FarmMsgHandler.java:117](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/FarmMsgHandler.java:117), [src/main/java/avatar/service/FarmService.java:67](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/FarmService.java:67). Người chơi có thể có sản phẩm trong kho nhưng chưa có chu trình bán/chăm sóc/nâng cấp hoàn chỉnh. Sửa tối thiểu: ưu tiên trồng → chăm → thu → bán, xác định luật/giá trước khi thêm logic; không chỉ bỏ thông báo chặn.

### AUD-004 — P1 — Bộ dữ liệu phát hành thiếu danh mục farmitems

[Chắc chắn đối với dữ liệu trong repo; trạng thái bảng production chưa đọc lại] SQL gốc không có farmitems; private-compat chỉ tạo bảng trống, không seed. Cả PartManager và GameData đều nạp bảng này; mua hạt giống từ chối khi item null; thu cây cần time/quantity của danh mục. Farm chưa thể vận hành đầy đủ chỉ từ bộ SQL phát hành.

Bằng chứng: [database/private-compat.sql:1](/Volumes/sontc/sources/others/avatar2d/database/private-compat.sql:1), [src/main/java/avatar/item/PartManager.java:156](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/item/PartManager.java:156), [src/main/java/avatar/model/GameData.java:44](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/model/GameData.java:44), [src/main/java/avatar/service/FarmService.java:212](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/FarmService.java:212), [src/main/java/avatar/model/User.java:128](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/model/User.java:128). Test shop pass bằng **fixture tổng hợp**, không chứng minh shop production có hàng. Sửa tối thiểu: bổ sung danh mục xác thực từ nguồn game/quy tắc được chốt, kiểm ID với farm_info.dat; không bịa giá/tốc độ/sản lượng.

### AUD-005 — P2 — Khế chỉ thu một đợt, chưa có cơ chế mọc lại

[Chắc chắn, source] Giá trị ban đầu 180, thu hoạch đặt starfruitQuantity=0 và lưu; các đường đọc/ghi hiện có không có timer/mốc thời gian hoặc quy tắc tái sinh. Nâng cấp/hoàn thành khế cũng chưa hỗ trợ. Lượt sửa trước đã xử lý nhận/lưu, chưa triển khai chu kỳ.

Bằng chứng: [src/main/java/avatar/model/User.java:125](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/model/User.java:125), [src/main/java/avatar/model/User.java:154](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/model/User.java:154), [src/main/java/avatar/service/FarmService.java:449](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/FarmService.java:449). Sửa tối thiểu: chốt thời gian/sản lượng/cấp cây, lưu timestamp và tính thu hoạch theo thời gian server; test restart và thu lặp. Chưa khẳng định thời gian mọc của game gốc.

### AUD-006 — P2 — Thăm farm người khác hiển thị farm của mình

[Chắc chắn, tái hiện local] JOIN đọc userId rồi ghi ID đó lên packet, nhưng đất, vật nuôi và khế đều lấy `session.user`, không load chủ farm được yêu cầu. Probe ID=99999 nhận đúng đất/vật nuôi giả vừa thêm ở tài khoản hiện tại.

Bằng chứng: [src/main/java/avatar/service/FarmService.java:416](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/FarmService.java:416). Không kết luận có ghi vào farm người khác; lỗi đã chứng minh là dữ liệu hiển thị sai. Sửa: lấy snapshot chủ farm được yêu cầu, tách quyền xem và quyền ghi; test hai user có dữ liệu khác nhau.

### AUD-007 — P2 — Xã hội: Hội nhóm/bạn bè chưa hoàn thiện, chat riêng sai tên

[Chắc chắn] User quảng bá Hội nhóm id=0 nhưng HandlerMENU_ROTATE không có case 0; probe chỉ nhận packet REQUEST_YOUR_INFO 2 byte. REQUEST_FRIENDLIST đi vào requestFriendList gửi chat “ok” + “comingsion”, không danh sách. ParkService chỉ gửi lời mời kết bạn; không thấy dispatcher nhận ADD_FRIEND/REMOVE_FRIEND để hoàn tất và lưu quan hệ. Chat riêng tìm cả sender và receiver bằng receiverId.

Bằng chứng: [src/main/java/avatar/model/User.java:1243](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/model/User.java:1243), [src/main/java/avatar/service/AvatarService.java:1023](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/AvatarService.java:1023), [src/main/java/avatar/network/Session.java:1955](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:1955), [src/main/java/avatar/service/ParkService.java:84](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/ParkService.java:84), [src/main/java/avatar/service/AvatarService.java:172](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/AvatarService.java:172). Hội nhóm/bạn bè đã tái hiện local; chat sai tên chứng minh source, chưa thử hai client thật. Sửa theo từng contract create/join/list/accept/remove, rồi test hai người + reconnect; chat lấy sender từ session.

### AUD-008 — P2 — 13 NPC có hình nhưng chưa có chức năng riêng

[Chắc chắn đối với 28 NPC seed] Đối chiếu NPC SQL với case NpcName đang hoạt động: 15 NPC có case riêng, 13 vào fallback báo chưa có chức năng. Danh sách: Tiên cá (50), Giám thị (315), Phi hành gia (335), Phù thủy bóng tối (425), Vé số (465), Chủ hôn (525), Zombie (535), Bậc thầy phù thủy (540), Pretty witch (590), Cô giáo tiếng Anh (645), Cô giáo tiếng Việt (646), Ăn xin (741), Pharaoh (760). Không nhầm NPC 425 với NPC shop 861 có cùng tên hiển thị.

Bằng chứng: [database/avatar_2x.sql:65217](/Volumes/sontc/sources/others/avatar2d/database/avatar_2x.sql:65217), [src/main/java/avatar/handler/NpcHandler.java:235](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/NpcHandler.java:235), [src/main/java/avatar/handler/NpcHandler.java:739](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/NpcHandler.java:739). NPC đấu giá (186) có case nhưng chỉ báo bảo trì tại dòng 505. Không suy ra toàn bộ 13 NPC đều bắt buộc phải có minigame; cần xác định tính năng mong muốn theo NPC trước khi triển khai.

### AUD-009 — P2 — Minigame còn sườn/dữ liệu mẫu, chưa đủ để mở đồng loạt

[Chắc chắn về source] getHandler chỉ nối casino index3, avatar8, park9, farm10, home11; RaceMsgHandler không được nối. Không tìm thấy xử lý JOIN_ROOM_RACE/DAT_CUOC/HISTORY_PET_RACE độc lập. Bầu cua có mở UI và casino có vòng bàn/turn/result; các lớp Phom/TienLen/Diamond rỗng **không được dùng làm bằng chứng duy nhất**: dispatcher thực tế cũng chỉ có một luồng casino chung. gameResult ghi thắng byte999 và FINISH năm số999; không cập nhật tiền thật trong hàm. Oan Tù Xì id4 trả text/số dư mẫu 1/2/3, chưa thấy vòng chơi theo lựa chọn.

Bằng chứng: [src/main/java/avatar/network/Session.java:301](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:301), [src/main/java/avatar/message/CasinoMsgHandler.java:41](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/CasinoMsgHandler.java:41), [src/main/java/avatar/message/CasinoMsgHandler.java:562](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/CasinoMsgHandler.java:562), [src/main/java/avatar/service/AvatarService.java:1036](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/AvatarService.java:1036), [src/main/java/avatar/message/RaceMsgHandler.java:5](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/RaceMsgHandler.java:5). Chưa nghiệm thu multiplayer, chưa khẳng định từng màn client gửi lệnh nào. Sửa: chọn một minigame, hoàn thành validation/turn/tiền/lưu/reconnect, kiểm 2–3 client trước khi quảng bá.

### AUD-010 — P2 — Một đường vào nhà chỉ log, quyền/mật khẩu nhà chưa có handler

[Chắc chắn] JOIN_HOUSE_4 → doJoinHouse4 chỉ readInt + println, probe nhận 0 response. JOIN_HOUSE thông thường lại có truy vấn/response; không kết luận mọi cách vào nhà đều hỏng. Không thấy case cho SET_PASS_MY_HOUSE/KICK_OUT_HOME/INVITE_TO_MY_HOME/PASS_CHEST trong các dispatcher hiện hành; đây là khoảng trống giao thức, cần đối chiếu màn UI có gửi chúng hay không.

Bằng chứng: [src/main/java/avatar/message/MessageHandler.java:29](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/MessageHandler.java:29), [src/main/java/avatar/network/Session.java:1062](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:1062), [src/main/java/avatar/network/Session.java:1965](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:1965), [src/main/java/avatar/message/HomeMsgHandler.java:28](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/HomeMsgHandler.java:28). Sửa JOIN_HOUSE_4 theo contract client, hoặc phản hồi chưa hỗ trợ; chỉ quảng bá mật khẩu/mời/đuổi khi kiểm quyền server đầy đủ.

### AUD-011 — P2 — Hẹn hò/cưới chưa xử lý hoàn chỉnh các tình huống thực tế

[Chắc chắn, source] Mời hẹn hò duyệt user online, không thấy tên thì kết thúc không feedback. Hủy hẹn hò đang comment/nhánh rỗng. WEDDING_BIGIN luôn gửi cặp ID7/1 và cập nhật level của người gửi; không lấy cặp đang cưới để xây packet. Chủ hôn lại thuộc nhóm NPC fallback.

Bằng chứng: [src/main/java/avatar/handler/GlobalHandler.java:160](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/GlobalHandler.java:160), [src/main/java/avatar/handler/GlobalHandler.java:100](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/GlobalHandler.java:100), [src/main/java/avatar/service/AvatarService.java:1030](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/AvatarService.java:1030), [src/main/java/avatar/service/ParkService.java:51](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/ParkService.java:51). Chưa thử kết hôn tài khoản production. Sửa: cặp thực/đồng thuận/trạng thái chờ, phản hồi offline/từ chối/hủy; test hai client và reload.

### AUD-012 — P2 — Menu vẫn quảng bá nhiều đích không thực hiện

[Chắc chắn, source] Mã giới thiệu/Diễn đàn tạo Menu không action; GlobalHandler giờ chỉ phản hồi chưa hỗ trợ (đã sửa sự im lặng ở batch trước). Lái buôn điểm danh chỉ đang xây dựng. Tiên tri dự đoán xổ số/Tài Xỉu chỉ Comingsion. Du lịch quảng bá bốn mục, nhưng GlobalHandler chỉ xử lý select0..2; select3 Biển citylo không có response. requestJoinAny id18 báo Locity đang xây dựng. Đăng ký/Chơi mới/email chỉ thông báo tài khoản đã cấp.

Bằng chứng: [src/main/java/avatar/network/Session.java:1257](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:1257), [src/main/java/avatar/handler/GlobalHandler.java:53](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/GlobalHandler.java:53), [src/main/java/avatar/network/Session.java:1200](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:1200), [src/main/java/avatar/service/AvatarService.java:449](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/service/AvatarService.java:449), [src/main/java/avatar/handler/NpcHandler.java:278](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/NpcHandler.java:278), [src/main/java/avatar/handler/NpcHandler.java:590](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/NpcHandler.java:590), [src/main/java/avatar/message/MessageHandler.java:24](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/message/MessageHandler.java:24). Sửa nhỏ: thống nhất trạng thái trên UI và phản hồi, không để menu im lặng; việc triển khai từng tính năng là phạm vi riêng.

### AUD-013 — P2 — Quay miễn phí thiếu vé gây exception; quay Xu kiểm sai ngưỡng

[Chắc chắn, source] handleDiaLucky lấy itm rồi gọi getQuantity trước khi kiểm null. Người không có vé nhưng rương đủ trống gặp exception thay vì thông báo thiếu vé. Luồng mở menu yêu cầu25.000 Xu; luồng thực thi chỉ chặn dưới15.000 rồi trừ25.000, nên nếu số dư đã thay đổi sau khi chọn hoặc gửi lại yêu cầu, ngưỡng không nhất quán.

Bằng chứng: [src/main/java/avatar/handler/NpcHandler.java:49](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/NpcHandler.java:49), [src/main/java/avatar/network/Session.java:1887](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/network/Session.java:1887). Sửa null guard trước dereference và kiểm cùng mức giá tại điểm trừ tiền. Test không vé, vé0, rương đầy, số dư15k–24.999 và gửi lặp; chưa gọi quay thật.

### AUD-014 — P2 — Gift code sai/hết hạn thường không phản hồi

[Chắc chắn, source] GlobalHandler case20 bỏ boolean trả về từ useGiftCode; mã không tồn tại/hết hạn/hết lượt trả false không gửi dialog. Trong sáu mã seed có năm mã đã hết hạn vào ngày audit, một mã có hạn tới2028; không kết luận tất cả mã đều hết hạn hay giống DB hiện tại. Việc giảm lượt diễn ra trước ghi usage/phát quà, nên lỗi sau đó có thể tiêu lượt mà không nhận quà. DB có UNIQUE(user,giftcode_id), vì vậy không báo nhầm thiếu chống dùng lại hoàn toàn.

Bằng chứng: [src/main/java/avatar/handler/GlobalHandler.java:243](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/handler/GlobalHandler.java:243), [src/main/java/avatar/model/GiftCodeService.java:107](/Volumes/sontc/sources/others/avatar2d/src/main/java/avatar/model/GiftCodeService.java:107), [database/avatar_2x.sql:56322](/Volumes/sontc/sources/others/avatar2d/database/avatar_2x.sql:56322), [database/avatar_2x.sql:65821](/Volumes/sontc/sources/others/avatar2d/database/avatar_2x.sql:65821). Sửa phản hồi từng trạng thái; kiểm kho trước tiêu lượt, gộp thao tác DB phù hợp/khôi phục khi lỗi. Chưa redeem code thật.

## Thứ tự đề xuất

1. Chặn giao dịch sai và mất tiền: AUD-001/002/013; sửa hiển thị farm AUD-006.
2. Hoàn chỉnh một vòng farm: danh mục chuẩn AUD-004 → trồng/chăm/thu/bán AUD-003 → chu kỳ khế AUD-005. Chốt luật kinh tế thay vì tự gán số.
3. Loại phản hồi im lặng, ghi trạng thái menu/NPC đúng: AUD-007/008/010/012/014. Giữ phần chưa làm có thông báo rõ ràng.
4. Mở từng nhóm xã hội/cưới/minigame sau khi có test hợp đồng và kiểm nhiều client. Không gỡ chặn đồng loạt.

## Kiểm chứng và giới hạn

- Java17 `mvn -q test`: exit0, **21 test pass, 1 integration test bỏ qua có chủ đích**, không lỗi/failure. FarmPersistenceTest chỉ chạy nếu bật isolatedFarmDb; lượt audit này không chạy lại DB tích hợp.
- 6 phép thử tạm trong `/tmp`, dùng CaptureSession/User giả và socket loopback, **không start collector/DB/game server**: mở đất sai ID, mua animal sai loại, thăm farm sai dữ liệu, Hội nhóm, friend list, JOIN_HOUSE_4. Assertions xác nhận **hành vi thiếu/sai hiện tại**, không phải test nghiệm thu chức năng. Kết quả kèm `local-probes.log`.
- Quét NPC đã loại comment case VE_SO, đối chiếu const ID với đúng bảng NPC; kết quả13/28 thiếu nhánh. Không đếm class rỗng/enum chưa dùng thành tính năng bắt buộc nếu chưa có caller.
- Source traversal xác nhận farmitem thiếu seed, dispatcher/placeholder/tiền nhà/quay số/gift code. Không gây SQL failure trên production, không tạo vật phẩm/tiền/tài khoản thật.
- Chưa thao tác từng menu trên điện thoại thật; chưa kiểm toàn bộ subtype packet client, nhiều người chơi, timer/restart giữa minigame, mọi shop/event/boss, hạ tầng thanh toán/SMS. Không dùng lượt test pass để tuyên bố game đầy đủ.
- Không cần và không thực hiện tìm web: kết luận dựa source/test/dữ liệu cục bộ, không dựa thông tin phiên bản dịch vụ bên ngoài.
