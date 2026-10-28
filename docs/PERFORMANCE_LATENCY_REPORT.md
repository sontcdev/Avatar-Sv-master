# Performance and handler latency — Milestone 2.6

[Chắc chắn] Chỉ có phép đo **readiness chẩn đoán**, không phải benchmark năm/mười active CCU. Cổng fail; không có cửa sổ đủ300 giây. Artifact SHA256 `12c781f70a001d6b487a1614740bfe3e1702e66fbc582c0f4783c3608f4b0331`.

## Handler processing

`Session.MessageCollector` bắt đầu `nanoTime` ngay trước `onMessage`, ghi trong finally sau khi handler trả/throw. Chỉ bật với `avatar.staging.telemetry=true` và user.dir chính xác `/home/sontc/avatar2d-gameplay-stage`; mặc định OFF. Đầu ra chỉ timestamp/session/user/command/duration/completion, không payload hay credentials. Packet trace cũ vẫn opt-in riêng, thêm epoch/session để đối chiếu. Không đổi packet wire, cleanup, dispatch, pool hay timeout.

Khoảng đo bao gồm công việc đồng bộ trong handler và chi phí tạo tiền tố telemetry nhỏ; không gồm socket read, outbound queue/send, client render hoặc round trip. Trace đồng bộ cũng ảnh hưởng runtime, chưa đo overhead riêng. `completed=true` chỉ nghĩa handler trả bình thường: handler có thể tự bắt lỗi/reject nghiệp vụ; **không phải completion gameplay**. Nhãn authenticated được lấy sau handler; command-2 chính là login tạo user.

103 dispatch tổng,66 có user sau handler,37 pre-auth;0 exception thoát handler. Tổng hợp nearest-rank trên toàn preparation/readiness, **không suy ra capacity từ số mẫu nhỏ**:

|Command authenticated|n|p50 ms|p95 ms|p99 ms|
|---|---:|---:|---:|---:|
|-97|5|0.463190|0.560759|0.560759|
|-80|20|0.408016|1.579331|1.614810|
|-70|5|0.059435|0.103505|0.103505|
|-11|5|0.870716|4.092515|4.092515|
|-2 login|5|151.430135|453.545164|453.545164|
|-1 context|5|0.287328|0.403755|0.403755|
|50 join|5|0.870577|1.009422|1.009422|
|54 movement|11|0.542670|0.744233|0.744233|
|57|5|0.149043|0.980648|0.980648|

[CSV đầy đủ](evidence/milestone-2-6/handler-latency.csv) có pre-auth và max. Không đo client RTT: chưa có request/response và frame correlation đáng tin cậy. Không sử dụng sleep hoặc thời gian ADB làm gameplay latency.

## JVM/DB tại plateau auth5

21 mẫu5 giây, epoch1793167226792–1793167326866, span100.074 giây; nhiều thời gian là idle/readiness.34 dispatch trong khoảng mẫu,0.33975 request/s; đây là workload đã phát sinh, không phải mức tải tối đa.

|Đo|Kết quả|
|---|---|
|JVM CPU delta/wall,100%=1 core|5.09%|
|Heap/RSS sampled max|20.01/103.40 MiB|
|GC count/time delta|0/0ms|
|Java/OS threads max|75/89|
|Hikari active/idle/wait sampled max|0/2/0|
|JVM disk read/write delta|0/135168 bytes|
|DB lock waits/time/current waits|0/0ms/0 trong observer|
|Slow_queries|0; không thay cho query latency|

Heap128MiB, processors2, DB buffer32MiB/max12, pool4/min1, socket120 giây giữ nguyên. Pool active0 chỉ là giá trị tại thời điểm lấy mẫu, không nghĩa không có SQL. Performance Schema staging=OFF; **thời gian gameplay DB query chưa đo được**. Không bật full SQL logging hay thay cấu hình DB để tạo số đẹp. Host diskstats/production stats nằm trong raw observer, có cả workload khác; không quy toàn bộ I/O host cho game. Production process I/O permission denied như M2.5.

## Client host và giới hạn quy kết

Mac10 logical CPU/32GiB;83 mẫu có năm emulator. Tổng `%CPU` của emulator theo `ps` sampled max983.9% (không phải đo delta CPU toàn máy), tổng RSS max12096.875MiB (có thể đếm trùng trang shared). Swap dùng sẵn5011.88MiB đầu quan sát, max cùng mức, cuối4907.88MiB: không được quy toàn bộ swap cho lần chạy này. Khởi động emulator tốn CPU nhưng chưa chứng minh đây là nguyên nhân cổng fail hoặc giới hạn10 client. Lần này không có System UI ANR trong logcat thu được. Không đổi RAM/core/FPS của clone để che lỗi.

Server-host observer giữ production Java313665/MariaDB302726 và service active/NRestarts0. Không có kiểm thử SLA ứng dụng production. [Raw JVM](evidence/milestone-2-6/jvm-metrics.csv), [server host](evidence/milestone-2-6/server-host-metrics.jsonl), [client host summary](evidence/milestone-2-6/client-host-summary.json), [summary](evidence/milestone-2-6/performance-summary.json). Các số này không chứng minh production readiness hay khả năng chịu10 người chơi.
