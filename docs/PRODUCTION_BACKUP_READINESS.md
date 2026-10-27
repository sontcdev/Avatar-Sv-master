# Production backup readiness — Milestone 2, 2026-10-28

[Chắc chắn] **PARTIAL; production chưa sẵn sàng cutover.** Chỉ audit quyền đọc và trạng thái service; không dump, sửa DB, migrate, deploy, restart hoặc đổi quyền production.

## Bằng chứng thực tế

SSH user `sontc` không đọc được `/opt/avatar2d/config.properties`; `sudo -n true` exit1. Không vượt quyền, không lấy credential qua process/env. Service `avatar2d` MainPID313665/NRestarts0 trước và sau các lần thử. Các script deploy cũ có bước dừng production không được dùng.

Instance MariaDB10.11 riêng do user sở hữu: loopback13317, datadir `/home/sontc/avatar2d-gameplay-stage/data` (kiểm đường dẫn thực tế trước thao tác), game loopback19129. Dữ liệu static target + tài khoản tổng hợp71/92; không copy người chơi production. Root bootstrap không mật khẩu chỉ thuộc instance thử nghiệm, không phải quyền root production.

Đã dừng đúng JVM staging322394 sau xác minh `/proc/PID/cwd`, dump bằng `--single-transaction --routines --triggers --events --hex-blob`, restore vào DB riêng `avatar2d_m2_restore_check`. File private `m2-quiesced.sql`, SHA256 `52b920137beba3bc8464f8c3f95c5b886644c6707a94af8daadd61b8b1f4b1c7`, mode600. Source/restore đều35bảng. CHECKSUM TABLE: players=1715480917 ở cả2DB; farm_data=3907078398 ở cả2DB. Dump ban đầu khi writer đang hoạt động không dùng để so checksum với DB tiếp tục thay đổi; đã lặp lại khi writer dừng để tránh kết luận sai.

Khôi phục này chứng minh schema/data staging đọc được, không chứng minh tính đầy đủ, quyền hoặc RPO/RTO của backup production. Chưa đo thời gian restore, chưa thử startup app trên DB restore, chưa so checksum mọi bảng.

## Quy trình production chờ quản trị viên duyệt và thực hiện

Các lệnh bên dưới là **runbook chưa thực thi**. Quản trị viên cần phê duyệt DB đích, khung giờ, storage, quyền backup và người chịu trách nhiệm trước khi chạy. Không gửi credential vào chat/Git/CLI arguments.

1. Xác minh service, endpoint DB và database từ cấu hình mà quản trị viên được phép đọc; ghi phiên bản MariaDB/MySQL, JAR SHA, schema version và thời điểm. Audit engine từng bảng; `--single-transaction` chỉ nhất quán cho transactional tables. Nếu có MyISAM hoặc writer/DDL khác, quản trị viên phải duyệt maintenance/locking riêng; không coi dump live mặc nhiên nhất quán.
2. Quản trị viên cấp account backup tối thiểu đúng phiên bản (SELECT, SHOW VIEW, TRIGGER, EVENT và quyền routine tương ứng; tránh GRANT ALL), lưu option file mode600, giữ ngoài repo. Ghi nơi lưu/retention/encryption và người có quyền restore. Không chỉnh quyền file production cho SSH user để lách bước này.
3. Trên máy được duyệt, thay placeholders bằng giá trị đã kiểm tra, chạy với shell hỗ trợ pipefail. Dùng `--defaults-extra-file` ở vị trí đầu và không dùng `--databases` để restore đổi tên được:

```bash
set -euo pipefail
backup_dir=/APPROVED/PRIVATE/BACKUP_DIRECTORY
backup_options=/APPROVED/PRIVATE/backup.cnf
production_db=APPROVED_DATABASE_NAME
umask 077
stamp=$(date -u +%Y%m%dT%H%M%SZ)
dump_path="$backup_dir/avatar2d-$stamp.sql"
mariadb-dump --defaults-extra-file="$backup_options" \
  --single-transaction --routines --triggers --events --hex-blob \
  "$production_db" > "$dump_path"
test -s "$dump_path"
sha256sum "$dump_path" > "$dump_path.sha256"
sha256sum -c "$dump_path.sha256"
```

4. Quản trị viên archive artifact/config/resources và manifest SHA cùng bản backup; config chứa secret phải được mã hóa/giới hạn truy cập, không đưa vào evidence công khai. Dump DB không thay backup artifact/config. Xác minh procedure/event DEFINER không trỏ sang quyền production trong môi trường restore.
5. Provision **instance DB khác**, account khác, loopback hoặc mạng cô lập; không dùng production socket. Tạo DB mới, không DROP DB hiện hữu. Trong instance restore, giữ event scheduler OFF, chặn kết nối tới production, không khởi chạy job thanh toán/email. Ví dụ sau chỉ dành cho instance restore đã được xác minh:

```bash
restore_options=/APPROVED/PRIVATE/restore-isolated.cnf
restore_db=avatar2d_restore_verification_YYYYMMDD
mariadb --defaults-extra-file="$restore_options" \
  -e "CREATE DATABASE $restore_db CHARACTER SET utf8mb4"
mariadb --defaults-extra-file="$restore_options" "$restore_db" < "$dump_path"
```

6. So table count, engines, row counts, schema, routines/triggers/events, các invariant currency/kho/attendance/friendships. Với writer source còn chạy, so theo snapshot/manifest đã ghi, không so checksum với source đang biến đổi rồi kết luận restore sai. Dùng app đọc DB restore với port riêng, không cho kết nối/ghi production; chạy login/restart và các gameplay cần bảo toàn. Ghi thời lượng để xác lập RTO, thời điểm snapshot để xác lập RPO. Chỉ công bố backup production WORKING khi bước này có bằng chứng.

## Rollback

Nhánh Milestone2 chưa cutover. Rollback staging: dừng đúng PID có cwd staging, giữ log và snapshot hậu kiểm, quay JAR trước đó; giữ hai column starfruit/attendance và bảng gameplay_friendships additive. Không DROP bảng/column để rollback code, không chạy hai writer cùng DB. Giá bán để OFF bằng cách bỏ system property; đây không hoàn tác giao dịch đã commit.

Nếu sau này cần rollback production, phải có phê duyệt riêng: maintenance dừng writer, snapshot hậu lỗi, xác định JAR/config trước đó và ảnh hưởng writer legacy lên clock/inventory. Ưu tiên rollback artifact tương thích schema. Restore DB chỉ vào DB mới và reconcile mọi giao dịch sau snapshot (currency, inventory, attendance, friendships) trước khi đổi cấu hình; không restore đè DB đang chạy hoặc âm thầm mất dữ liệu mới. Chưa có bằng chứng cho phép thực hiện cutover/rollback production tự động.
