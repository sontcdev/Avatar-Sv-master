# Isolated gameplay staging

Không chạy các script deploy production để thử nhánh này. Lần kiểm tra 2026-10-28 dùng thư mục user-owned `/home/sontc/avatar2d-gameplay-stage`, game loopback19129, MariaDB instance riêng loopback13317. Dữ liệu static từ target và tài khoản tổng hợp; không sao chép player production. Resources dùng symlink chỉ đọc tới bộ resources staging hiện có.

Build bằng Java17: `mvn test`, sau đó `mvn -DskipTests package`. Áp dụng compatibility schema và ba migration theo `database/migrations/README.md` trước khi chạy JAR mới. Cấu hình private nằm ngoài Git; pool tối đa4, tối thiểu1, max clients10. Không đưa mật khẩu/SQL tạo account vào evidence.

Probe chỉ dành cho staging, biên dịch bằng cùng Java17 và artifact đã kiểm hash:

```sh
javac -cp server.jar GameplayProbe.java
java -Xms32m -Xmx128m -XX:ActiveProcessorCount=2 -cp server.jar:. GameplayProbe
```

Chạy trong đúng thư mục staging có `config.properties`, `database.properties`, `res` và JAR. Probe dùng JVM management API, in heap/thread/process CPU mỗi10giây. Chỉ so sánh CPU delta giữa hai mẫu cùng PID; chia nanoseconds cho khoảng thời gian thực, rồi nhân100 để ra phần trăm của một core. RSS/OS threads/IO lấy từ `ps` và `/proc/PID/io`; DB lấy `SHOW GLOBAL STATUS` (observer cũng chiếm một connection). Byte IO là cumulative, read_bytes0 có thể do page cache, không có nghĩa không đọc file.

Trước khi stop, kiểm `/proc/PID/cwd` trùng tuyệt đối thư mục staging và command là đúng JVM staging; gửi TERM, chờ PID kết thúc rồi mới khởi động lại. Không dùng `pkill java`, `killall`, systemctl production hoặc restore đè DB hiện hữu. Sau kiểm tra tắt cả JVM staging và MariaDB thử nghiệm, đóng đúng SSH tunnels đã tạo và gỡ adb reverse. Giữ artifact/log/backup private để đối chiếu. Nếu vận hành staging lâu dài, provision DB user/password riêng thay cho bootstrap root local của instance thử nghiệm.

Client: Avatar2.5.8 riêng tên AvatarStage trên J2ME Loader; patch host loopback bằng helper của target, không thay APK production. SSH `-L19128:127.0.0.1:19129 homelab`, `adb reverse tcp:19128 tcp:19128`; chọn Xứ Sở Thần Tiên → Bảo Bình. Các city14444 không đi vào tunnel này. Phải khởi chạy MIDlet mới sau restart nếu reconnect lỗi; không coi screenshot login là gameplay đạt.

DB tests opt-in chỉ chấp nhận catalog `avatar2d_starfruit_test`. Tạo DB rỗng trên instance thử nghiệm; dùng private `-Davatar.database.config=...` cùng `-Davatar.test.isolatedFarmDb=true -Dtest=FarmPersistenceTest,AttendancePersistenceTest`. Test tạo schema tối thiểu, không dùng DB gameplay. Không chạy lại khi fixture chưa được tái tạo.

## Milestone 2: sales và social

Áp dụng thêm `database/migrations/20261028_friendships.sql` sau backup. Bảng này additive; không thay bảng friends legacy hay dữ liệu production. Friend list hiện tối đa100 người, chưa có pagination.

Bán native63 mặc định **OFF** khi không có system property, file không bật, approval trống hoặc item không có giá. Copy `farm-sales.properties.example` ra đường dẫn private, giữ mode600. Chỉ sau khi người chịu trách nhiệm economy duyệt bằng văn bản từng `price.ID` mới điền mã phê duyệt vào `approval`, đặt `enabled=true` và truyền `-Davatar.farm.sales.catalog=/absolute/private/path`. Không có giá production nào được cung cấp trong nhánh. Không lấy giá7 từ fixture làm giá thật. Synthetic tests dùng approval `SYNTHETIC_TEST_ONLY`, item215=7 và item7=2; chỉ trong DB test.

Request63 chỉ chứa short item, bán toàn bộ số lượng item đang sở hữu; không có nonce. Duplicate cùng kho bị chặn, nhưng packet cũ đến sau một lần thu hoạch mới không thể phân biệt với yêu cầu bán mới. Vì hạn chế này cùng thiếu client E2E, chưa bật live sales. Transaction khóa players rồi farm_data và ghi kho/tiền cùng commit; yêu cầu RAM khớp DB. Nếu mất xác nhận commit/rollback, đóng session và chặn snapshot logout cũ. Các writer legacy khác vẫn cần single active session/server; chưa chứng minh an toàn nhiều JVM.

Opt-in sales/friends tests chỉ cho DB **avatar2d_sales_test** trên instance cô lập:

```sh
mvn -Davatar.test.isolatedSalesDb=true -Davatar.database.config=/private/sales.properties -Dtest=FarmSalePersistenceTest,FriendshipPersistenceTest test
```

Fixture DROP/recreate các bảng test sau khi kiểm tra tên DB; không trỏ cấu hình vào DB có dữ liệu cần giữ. Chạy nhóm starfruit/attendance riêng với DB `avatar2d_starfruit_test`; hai nhóm không dùng chung cấu hình trong một JVM.

Trace chỉ staging: `-Davatar.protocol.trace=true`, mặc định false. Log các command được chọn gồm nội dung chat đã giải mã; giữ log private, dùng tài khoản tổng hợp, không bật trên production. `QUEUED` chỉ chứng minh server xếp gói gửi, không chứng minh client nhận/render. Bản trace thử đầu dùng getData trên incoming Message nên bytes=0 không có giá trị chứng minh payload; bản cuối lấy payload trực tiếp sau giải mã.
