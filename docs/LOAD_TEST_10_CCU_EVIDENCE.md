# Five-client gate and ten-client evidence — Milestone 2.6

[Chắc chắn] **UNVERIFIED**: năm client không qua readiness; mười native **NOT ATTEMPTED — GATE FAILED**. Không có protocol-level load generator hay cửa sổ active300 giây. Không dùng auth-only để tuyên bố10 CCU.

Source HEAD `f51b2a9ba4e5c1cda272b0f47744768fe9a8daf1`; baseline sạch `36d80e2f1c5c8105a34e3d9a33be9378884e1e8f`. JAR local/remote `12c781f70a001d6b487a1614740bfe3e1702e66fbc582c0f4783c3608f4b0331`.

Năm Avatar2.5.8 thật qua J2ME Loader, read-only clones Pixel_10_Pro, khởi động lần lượt5556/5558/5560/5562/5564. Không sửa profile gốc hoặc reset ADB. ADB reverse19128→owned SSH→remote loopback19129. Server Xmx128MiB/ActiveProcessorCount2; MariaDB buffer32MiB/max12; Hikari max4/min1; max clients10; socket timeout120 giây. Native63 OFF.

## Timeline 2026-10-28 UTC

|Mốc|Sự kiện|
|---|---|
|05:53:19.255 Mac|Khởi động emulator5556, PID99723; tiếp theo5558 PID764,5560 PID1192,5562 PID1377,5564 PID2203.|
|05:57–05:59|Sửa RMS trong chuẩn bị; chọn city từ OCR. Force-stop chuẩn bị71 xảy ra trước phiên xác thực thành công; không gọi đây là disconnect tải.|
|06:00:26.792 server|Mẫu đầu auth5:71/92/93/94/95. Đây là readiness, chưa phải window active.|
|06:01:34–35 server|Nhận54 từ cả năm;92/93/94/95 đổi348→258;71 vẫn258.|
|06:01:40.628–42.821 Mac|Driver xác nhận95/93/92 lần lượt;94 không qua marker ảnh.|
|06:01:50.437 Mac|71 timeout movement.94 timeout06:01:51.500. Dừng cổng; không ramp10.|
|06:02:06.866 server|Mẫu auth5 cuối trước thu dọn.21 mẫu5 giây, span100.074 giây, **không phải300 giây active**.|
|06:02:11.167–12.484 server|Năm EOF khi chủ động force-stop client teardown; không có disconnect ngoài dự kiến trước đó trong telemetry.|
|06:02:13 remote|Gửi TERM JVM359161; dừng writer trước backup/restore bốn friendship fixture.|
|06:07:54 remote|JVM/DB staging và owned emulators/tunnels đã dừng; production PID/service giữ nguyên.|

Mốc khác host không được coi đồng bộ millisecond; xem [clock limits](NATIVE_CLIENT_DRIVER_DIAGNOSIS.md).

## Per-client

|Serial|User/session|Movement verified|Readiness movement failure|Lệnh ADB driver lỗi|Kết luận|
|---|---|---:|---:|---:|---|
|5556|71/2|0|1|0/212|Packet54 trùng tọa độ trước; hai transition timeout khác thuộc chuẩn bị.|
|5558|92/3|1|0|0/56|UI park + tọa độ đổi.|
|5560|93/4|1|0|0/56|UI park + tọa độ đổi; không force-stop phục hồi trong gate.|
|5562|94/5|0|1|0/60|Server đổi tọa độ; camera làm marker ảnh thất bại; không tính completed.|
|5564|95/6|1|0|0/52|Không tái hiện disconnect95 của M2.5.|

[Auth và active mỗi5 giây](evidence/milestone-2-6/authenticated-active-counts.csv) tính active từ **completed movement trong30 giây gần nhất**, tối đa3; không đếm observe/transition login, intent hay authenticated idle. Cả năm vào park; own-farm/friend-farm/chat chưa được xác minh M2.6 do dừng sớm. Không có claim active cho96–100, không sử dụng NPC/internal actors làm người chơi.

## Integrity và giới hạn

[20 hàng trước/sau](evidence/milestone-2-6/integrity-result.json):10 players +10 farms; mọi cột gameplay đồng nhất. Năm phiên logout tăng player/farm revision đúng1;96–100 không đổi. Không claim thưởng/harvest/sales trong tải. Friendships từ0→4 fixture `(71,92..95)`→0 sau writer dừng; không gọi fixture là UI invitation. Backup trước và sau stop giữ riêng trên staging.

[Telemetry](evidence/milestone-2-6/session-telemetry.txt), [Android events](evidence/milestone-2-6/android-events.txt), năm ảnh `*-gate-failed.png`, log từng driver, JVM/host/state raw và [cleanup](evidence/milestone-2-6/cleanup.txt) cung cấp đối chiếu. Không packet capture TCP, không đồng hồ frame, không logcat lịch sử tương ứng M2.5; không chứng minh backend10 hay production readiness. Chưa phân phối emulator sang máy khác hoặc chạy protocol generator: lỗi gate hiện đã xác định ở automation, chưa chứng minh giới hạn native-host10.
