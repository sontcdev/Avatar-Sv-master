# Isolated gameplay staging

Không chạy các script deploy production để thử nhánh này. Lần kiểm tra 2026-10-28 dùng thư mục user-owned `/home/sontc/avatar2d-gameplay-stage`, game loopback19129, MariaDB instance riêng loopback13317. Dữ liệu static từ target và tài khoản tổng hợp; không sao chép player production. Resources dùng symlink chỉ đọc tới bộ resources staging hiện có.

Build bằng Java17: `mvn test`, sau đó `mvn -DskipTests package`. Áp dụng compatibility schema và hai migration theo `database/migrations/README.md` trước khi chạy JAR mới. Cấu hình private nằm ngoài Git; pool tối đa4, tối thiểu1, max clients10. Không đưa mật khẩu/SQL tạo account vào evidence.

Probe chỉ dành cho staging, biên dịch bằng cùng Java17 và artifact đã kiểm hash:

```sh
javac -cp server.jar GameplayProbe.java
java -Xms32m -Xmx128m -XX:ActiveProcessorCount=2 -cp server.jar:. GameplayProbe
```

Chạy trong đúng thư mục staging có `server.properties`, `database.properties`, `res` và JAR. Probe dùng JVM management API, in heap/thread/process CPU mỗi10giây. Chỉ so sánh CPU delta giữa hai mẫu cùng PID; chia nanoseconds cho khoảng thời gian thực, rồi nhân100 để ra phần trăm của một core. RSS/OS threads/IO lấy từ `ps` và `/proc/PID/io`; DB lấy `SHOW GLOBAL STATUS` (observer cũng chiếm một connection). Byte IO là cumulative, read_bytes0 có thể do page cache, không có nghĩa không đọc file.

Trước khi stop, kiểm `/proc/PID/cwd` trùng tuyệt đối thư mục staging và command là đúng JVM staging; gửi TERM, chờ PID kết thúc rồi mới khởi động lại. Không dùng `pkill java`, `killall`, systemctl production hoặc restore đè DB hiện hữu. Sau kiểm tra tắt cả JVM staging và MariaDB thử nghiệm, đóng đúng SSH tunnels đã tạo và gỡ adb reverse. Giữ artifact/log/backup private để đối chiếu. Nếu vận hành staging lâu dài, provision DB user/password riêng thay cho bootstrap root local của instance thử nghiệm.

Client: Avatar2.5.8 riêng tên AvatarStage trên J2ME Loader; patch host loopback bằng helper của target, không thay APK production. SSH `-L19128:127.0.0.1:19129 homelab`, `adb reverse tcp:19128 tcp:19128`; chọn Xứ Sở Thần Tiên → Bảo Bình. Các city14444 không đi vào tunnel này. Phải khởi chạy MIDlet mới sau restart nếu reconnect lỗi; không coi screenshot login là gameplay đạt.

DB tests opt-in chỉ chấp nhận catalog `avatar2d_starfruit_test`. Tạo DB rỗng trên instance thử nghiệm; dùng private `-Davatar.database.config=...` cùng `-Davatar.test.isolatedFarmDb=true -Dtest=FarmPersistenceTest,AttendancePersistenceTest`. Test tạo schema tối thiểu, không dùng DB gameplay. Không chạy lại khi fixture chưa được tái tạo.
