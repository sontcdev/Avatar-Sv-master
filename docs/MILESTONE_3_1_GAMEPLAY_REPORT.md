# Milestone 3.1 — gameplay farm

**PARTIAL:** đã triển khai cooking 91/92 và thông tin khế 87, kiểm thử unit/DB; chưa chứng minh được một gameplay loop hoàn chỉnh trên UI client thật. [Trạng thái 19 lệnh](FARM_GAMEPLAY_IMPLEMENTATION_STATUS.md) và [bằng chứng, giới hạn E2E](FARM_CLIENT_E2E_EVIDENCE.md).

Cooking đọc trực tiếp catalog target `res/data/farm_info.dat` SHA-256 `d52e67ed96ca6161606d7636c9d09e23c69f3f921dafe0c6df257771caf9cbd1`: 30 recipe. Start trừ nguyên liệu và lưu recipe/output/deadline epoch cùng revision farm trong một transaction; collect xác minh owner/deadline/catalog, cộng một sản phẩm và xóa mẻ trong một transaction. Thất bại khôi phục RAM; CAS conflict hoặc commit không rõ kết quả giữ fail-closed theo cơ chế persistence hiện có. JOIN phản ánh trạng thái thực, không còn 3/5 hằng số. 93 và cancel chưa bật do thiếu giá/policy. Thông tin khế 87 chỉ đọc, dùng dialog tương thích và giới hạn owner.

Áp dụng [migration](../database/migrations/20261028_farm_cooking.sql) sau backup trên DB được phép trước khi chạy JAR mới: thêm `cooking_recipe`, `cooking_ready_ms`, `cooking_output_id` mặc định 0; writer logout và farm CAS cùng giữ ba cột. Không áp dụng vào production. Nếu catalog thiếu/hỏng, thao tác nấu bị từ chối, bootstrap farm vẫn giữ đường cũ. Không có dữ liệu donor được chép.

JDK17 `mvn -B clean package` với asset thật: 85 khai báo, 52 PASS và 33 DB opt-in skip; test DB riêng: 31 PASS trên `avatar2d_sales_test`, 2 PASS trên `avatar2d_starfruit_test`, không skip. Tổng 85 unique PASS khi kết hợp các nhóm; Python `scripts/test-tools.py` PASS. Lần đầu test farm fixture gặp bảng cũ, đã backup rồi reset đúng ba bảng database test và chạy lại PASS. Test gồm CAS stale, logout/reload, duplicate collect, lỗi SQL và commit acknowledgement không chắc chắn, phân quyền, serializer, catalog cache. Chưa có acceptance real-client cooking trên artifact cuối. JAR delivery SHA-256 `e5e2434e2380b8de8ef4b651580c14dafb8607a9d113dd3b3cdfd06f78eb8487`.

## Chuẩn bị batch NPC

ID từ `database/avatar_2x.sql` target, không dùng donor enum: 50 tien.ca (map30); 315 giam.thi (18); 335 phi.hanh.gia (14); 425 phu.thuy.bong.toi (56); 465 ve.so (11); 525 Chủ Hôn (10); 535 zombie (55); 540 bac.thay.phu.thuy (58); 590 pretty.witch (57); 645 co.giao.tieng.anh (6); 646 co.giao.tieng.viet (6); 741 an.xin (3); 760 pharaoh (33). Wire NPC là 2000000000 + ID. Cần xác minh menu, điều kiện, catalog và side effects từng NPC trước triển khai; không đánh dấu placeholder hoàn tất. Donor ID 761/858/859/861 có collision với target.

## Infrastructure backlog

- Native client 71 movement: điều khiển/direction chưa đáng tin cậy.
- Historical client 95 disconnect: chưa xác định nguyên nhân.
- Burst-login JDBC pool exhaustion: chưa sửa.
- Native 5/10 active-client validation: chưa đạt; M2.7 chứng minh 10 TCP protocol clients active >5 phút, không phải 10 native.
- Production backup/restore: cần authorization riêng trước thực hiện.
- Native63 replay sau replenish: chưa giải quyết; live sales OFF.

Không push, merge hay deploy. Không sửa production. Backups staging được giữ private; fixture được khôi phục có guard sau khi dừng writer.
