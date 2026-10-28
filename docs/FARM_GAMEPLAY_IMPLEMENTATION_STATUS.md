# Farm gameplay implementation status — milestone 3.6

Milestone 3.7: **PARTIAL/BLOCKED**; 84/86/90/94 remain unavailable pending authoritative progression and costs. The global BUY_ANIMAL 71 guard remains in place; real-client compatibility has not been verified. See [report](MILESTONE_3_7_GAMEPLAY_REPORT.md) and [upgrade evidence](FARM_UPGRADE_BLOCKERS.md).


Milestone 3.6 is **INCOMPLETE**. Command 71 is BLOCKED with explicit feedback; no playable animal/care loop is claimed. See [3.6 report](MILESTONE_3_6_GAMEPLAY_REPORT.md) and [current roadmap](GAMEPLAY_RECOVERY_ROADMAP.md), which supersede historical status labels below.

Milestone 3.4 remains **INCOMPLETE**: no new playable feature or native crop acceptance. Client visual duration and incoming 78/79 decoding were clarified; see [report](MILESTONE_3_4_GAMEPLAY_REPORT.md). The command statuses below remain unchanged.

Trạng thái M3.3 thay thế mọi trạng thái lịch sử bên dưới trong matrix; xem [báo cáo](MILESTONE_3_3_GAMEPLAY_REPORT.md). Trạng thái phản ánh bằng chứng hiện có, không xem unit test là native acceptance. Lệnh chưa được bật vẫn trả dialog unsupported; Native63 live sales tiếp tục OFF.

| Command | Status | Căn cứ / việc còn lại |
|---|---|---|
| 64 PLANT_SEED | BLOCKED | Seed/land CAS cùng transaction; catalog staging trống, chưa xác minh duration/yield target hay UI. |
| 66 TREE_HARVEST | BLOCKED | CAS/duplicate/reload có DB fixture tổng hợp; semantics target và UI chưa xác minh. |
| 91 COOKING | VERIFIED_NATIVE | UI thật chọn Cơm và dispatch 91, DB trừ nguyên liệu/lưu deadline và UI phục hồi sau restart; request payload chưa trace, replay không nonce chưa giải quyết. |
| 92 HARVEST_COOK | VERIFIED_NATIVE | UI thực nhận 1 Cơm sau 30 phút, DB clear và kho tồn tại qua restart; early/duplicate đã kiểm unit/DB nhưng không gửi qua native UI. |
| 93 NAU_NHANH | BLOCKED | Wire xác minh, giá tăng tốc chưa xác minh. |
| 65 USING_ITEM | BLOCKED | Có catalog item nhưng thiếu semantics tác động và giới hạn. |
| 71 BUY_ANIMAL | BLOCKED | Từ chối rõ ràng; không tạo vật nuôi miễn phí khi chưa có chính sách giá được xác minh. |
| 72 PRICE_ANIMAL | BLOCKED | Cần xác minh giá bán và request/response thực. |
| 73 SELL_ANIMAL | BLOCKED | Thiếu policy giá, vòng đời animal và atomic economy. |
| 74 HARVEST_ANIMAL | BLOCKED | Thiếu state/timer/product contract hoàn chỉnh. |
| 78 INFO_CELL | BLOCKED | Chưa xác minh đầy đủ request/response native. |
| 79 INFO_ANIMAL | BLOCKED | Chưa xác minh đầy đủ request/response native. |
| 84 UPDATE_STARFRUIL | BLOCKED | Thiếu điều kiện và giá upgrade được xác minh. |
| 86 UPDATE_STARTFRUIT_BYMONEY | BLOCKED | Thiếu giá và contract economy. |
| 87 GET_INFO_STARFRUIT | VERIFIED_NATIVE | Client thực gọi 87; dialog hiển thị lượng, capacity và hồi phục, không chứa level. JOIN legacy vẫn gửi level 10 cố định và client vẽ Lv10; chưa có level authoritative. |
| 90 UPDATE_LAND | BLOCKED | Thiếu quy tắc cấp đất và giá. |
| 94 UPDATE_STORE | BLOCKED | Thiếu giá và quy tắc sức chứa. |
| 83 FINISH_STARFRUIT | BLOCKED | Thiếu contract và policy. |
| 95 STEAL_INFO | BLOCKED | Chưa xác minh contract/quota. |
| 96 STEAL | BLOCKED | Thiếu atomic hai farm, quota, anti-replay và history. |
| 97 STEAL_STORE | BLOCKED | Thiếu contract và atomic hai farm. |
| 98 LICH_SU_BI_TROM | BLOCKED | Thiếu schema và contract lịch sử được xác minh. |

Catalog thật `res/data/farm_info.dat` có 30 công thức; parser đọc toàn bộ cấu trúc và tham chiếu đầu vào/đầu ra, fail closed nếu không hợp lệ. Asset không được đưa vào Git. Cooking dùng ảnh chụp output trong DB để không cấp sản phẩm khác nếu catalog thay đổi; khi thu hoạch kiểm tra lại recipe/output hiện hành. Protocol không có nonce: gói start cũ phát lại sau khi thu hoạch có thể bắt đầu một mẻ mới và trả đủ nguyên liệu; không tuyên bố chống replay hoàn hảo.

[Chắc chắn] Native 85: đã thu hoạch 180 khế qua UI, stock 195→375 và vẫn hiển thị sau restart; kiểm lặp UI riêng chưa thực hiện. Không suy ra level cây từ nhãn Lv10 hard-coded.
