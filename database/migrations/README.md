# Gameplay additive migrations

Các script dành cho MariaDB (ADD COLUMN IF NOT EXISTS). Không áp dụng tự động khi server boot. Nếu dùng MySQL khác phiên bản, xác minh cú pháp trước. Bản target này yêu cầu các column starfruit, attendance, persistence_revision và bảng friendship; không chạy JAR mới trước schema.

1. Xác nhận đúng host/port/database và dừng **đúng writer staging**, hoặc maintenance production đã được cho phép. Không dùng tài khoản/root/socket production cho staging.
2. Lưu `mariadb-dump --single-transaction --routines --triggers DATABASE` vào file riêng mode600; kiểm exit code, kích thước/hash và thử restore vào DB cách ly. Backup DB hiện hữu phải có trước DDL.
3. Nếu baseline chưa có compatibility tables/column, áp dụng `database/private-compat.sql` của target local trước. Script này không phải seed catalog gameplay.
4. Áp dụng `20261028_starfruit_regrowth.sql`, rồi `20261028_daily_attendance.sql`, sau đó `20261028_friendships.sql` và `20261028_persistence_revisions.sql`. Chạy lại không reset harvest clock/day. Kiểm column, row count và farm JSON trước/sau.
5. Deploy JAR đã kiểm tra, xác minh login, khế, attendance, logout/restart và DB state; chỉ mở traffic sau kiểm chứng.

## Rollback

Stop đúng server mới và giữ log/DB snapshot sau migration trước mọi recovery. Có thể chạy JAR baseline với2column additive còn nguyên (không DROP column); tuy nhiên baseline không biết timer nên không phục hồi gameplay regrowth, attendance baseline là stub. **Không chạy bản cũ rồi nâng lại mà không reconcile clock**: writer cũ có thể đổi quantity nhưng để lại anchor cũ. Nếu cần rollback lâu dài, giữ writer dừng, restore backup vào DB tên khác, xác minh inventory/currency/daily claims phát sinh rồi chuyển cấu hình có kiểm soát. Không restore đè DB đang nhận traffic hoặc làm mất reward đã phát mà không đối soát.

Bốn migration không thay catalog, không cấp currency, không thay level cây. Legacy quantity180 được giữ; legacy quantity0+clock0 bắt đầu60phút hồi từ lúc migration. Date mới NULL chỉ cho phép lần nhận đầu sau khi bật tính năng (target baseline không có lịch sử điểm danh thật). Nếu môi trường đã có lịch sử thưởng từ nhánh khác, phải backfill attendance_date trước khi mở menu để tránh nhận thêm cùng ngày.

Production hiện chưa được backup/migrate trong công việc này. Xem docs/MIGRATION_REPORT.md.

Migration friendship tạo `gameplay_friendships(user_low,user_high)` InnoDB với khóa chính cặp canonical low<high và index high. Không backfill hay xóa bảng legacy. Rollback code giữ bảng này để không mất quan hệ đã tạo; không DROP khi vẫn có writer. Nhánh chưa được áp dụng lên production.

## Milestone 2.2 — revision rollout và đối soát

`players.persistence_revision` và `farm_data.persistence_revision` là BIGINT default0; migration không sửa JSON, tiền, ngày nhận thưởng hay quantity. Dừng **mọi writer của cùng database** trước DDL; không chạy lẫn JAR cũ/mới. Writer SQL ngoài server cũng phải conditional-update và tăng revision tương ứng, hoặc giữ dừng. Khóa Java không thay thế điều kiện revision trong DB. Thay đổi này chỉ được áp dụng lên staging cách ly.

Rollback về JAR cũ làm mất bảo vệ stale writer: giữ traffic và writers dừng, lưu snapshot sau sự cố rồi đối soát trước khi chọn artifact phục hồi. Không DROP revision, không reset về0, không dùng backup trước migration để đè reward/tiền đã commit. File `logs/persistence-conflicts/*.json` quyền0600 là proposal bị từ chối, **không phải bản để import tự động**. So sánh với DB mới nhất và log giao dịch; chỉ áp dụng delta hợp lệ trong transaction conditional-revision khi mọi writer đã dừng. Giữ bản gốc và biên bản quyết định; không chọn full snapshot làm nguồn sự thật. Chi tiết: `docs/STALE_SESSION_ROOT_CAUSE.md`.
