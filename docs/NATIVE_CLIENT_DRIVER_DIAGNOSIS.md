# Native client driver diagnosis — Milestone 2.6

[Chắc chắn] Cổng năm native client **FAIL tại readiness**, không chạy mười. Ba chuyển động được xác nhận; không có cửa sổ năm người hoạt động đủ 300 giây. Không quy lỗi server từ kết quả này.

## Phân loại bằng chứng

|Sự kiện|Phân loại|Căn cứ và giới hạn|
|---|---|---|
|M2.5 user93 lệch sang menu park|AUTOMATION; nguyên nhân từng lần UNRESOLVED|`active.py` dùng chuỗi key/tap với sleep cố định, không xác nhận màn hình; dòng cycle là ý định. Không có logcat/input trace đồng bộ để kết luận client freeze hay server lỗi.|
|M2.5 force-stop93 lúc ramp10|AUTOMATION, chủ động phục hồi input|Đã ghi trong báo cáo và phase abort M2.5; không phải bằng chứng server tự loại phiên. Không khôi phục được thời điểm lệnh chính xác từ trace cũ không timestamp.|
|M2.5 user95 rớt|UNRESOLVED|Auth10 ở epoch1793164445.807, còn8 ở1793164505.828; thiếu socket reason/logcat tương ứng. Timeout120 giây có trong cấu hình nhưng không đủ chứng minh đây là nguyên nhân.|
|M2.5 System UI ANR|EMULATOR, hiện tượng đã quan sát; liên hệ95 UNRESOLVED|Ảnh/lịch sử M2.5 có ANR. M2.6 logcat thu được không có ANR/fatal/decoder EOF; không tái hiện ANR lần này.|
|Worker nền không tách phiên|AUTOMATION, tái hiện độc lập|Worker1369 ghi start1793167008.537 rồi biến mất, không ghi completed sau sleep20. Không chứng minh mọi worker cũ chết cùng cách.|
|M2.6 sai fixture đăng nhập ban đầu|AUTOMATION, sửa trong chuẩn bị|RMS17 byte không phải avlogin63 byte. Cần cả `.rsr` và metadata `.rsh`; chỉ chép record có thể bị loader ghi lại. Sau sửa, native title71 và server xác thực đúng71. Không tính giai đoạn này là tải.|
|M2.6 city selection|AUTOMATION, tái hiện|OCR thấy dòng Bảo Bình đổi y khi danh sách tự cuộn; chọn từ chữ/đợi ổn định thay tọa độ cố định. OCR cũng đọc Bao thành Baa.|
|M2.6 user71 không hoàn thành movement|AUTOMATION completion failure; input semantics UNRESOLVED|ADB22 trả0, server nhận54 với x258,y156 — trùng vị trí cũ. Handler trả bình thường, không đóng session. Không suy ra chuyển động từ keyevent thành công.|
|M2.6 user94 bị từ chối ảnh|AUTOMATION, false negative đã xác minh|Packet54 và state đổi348→258, nhưng camera dịch nền làm bộ marker ảnh từ PARK→UNKNOWN_AUTHENTICATED. Ảnh cuối vẫn là park; không phải decoder hỏng.|
|ADB/SSH mất kết nối|Không tái hiện M2.6; M2.5 UNRESOLVED|436 lệnh trong driver đều return0; năm reverse đúng19128; SSH owned còn trong mẫu host. Không có TCP packet capture, không khẳng định mạng không bao giờ gián đoạn.|
|Server idle/lifecycle|Không tái hiện idle timeout trong M2.6|Năm phiên thật cùng tồn tại tới lúc đóng chủ động; telemetry chỉ ghi EOF sau force-stop teardown. Không tăng120000ms. Unit lifecycle và internal-actor vẫn PASS.|

## Driver được giao và giới hạn

[`scripts/native-client-driver.py`](../scripts/native-client-driver.py) dùng serial/account allowlist, quan sát OCR + server state mới dưới10 giây, ghi ADB returncode/timestamp, và xác nhận movement bằng cùng session + tọa độ thay đổi + UI park. Observe không được tính thành gameplay; timeout ghi failure. Chuyển cảnh chờ tối đa25 giây, movement15 giây; dừng khi không xác minh được, không tự force-stop hay tạo keepalive.

Nhận dạng đã chạy thật: launcher, login, city, world map, park. Heuristic friend list/chat/loading có self-check cơ bản nhưng **chưa xác minh native M2.6**. Farm rendered/owner/bootstrap chưa được tự động xác nhận. Marker park chỉ hiệu chuẩn800×1280 ở một vị trí camera; chính94 cho thấy giới hạn. Vì vậy đây là driver chẩn đoán dừng an toàn, **chưa phải driver đạt acceptance**. Không tiếp tục farm/chat hoặc ramp10 sau cổng thất bại.

`--self-check` kiểm nhận dạng và hai điều kiện quan trọng: không gửi input khi thiếu auth; gửi input nhưng không có completion phải fail. OCR helper dùng Swift/Vision có sẵn, Python dùng Pillow đã cài; không thêm dependency. Helper và collector trong [evidence](evidence/milestone-2-6/) là staging-only, ngoài JAR.

## Đồng hồ và tương quan

Driver/mac dùng epoch của Mac, packet/handler/state dùng epoch server, logcat dùng epoch Android. State lấy mẫu5 giây rồi SSH poll; không phải realtime frame timing. [Clock check](evidence/milestone-2-6/clock-check.json) thực hiện **sau chạy**, round trip khoảng2.24 giây; không đủ căn chỉnh lịch sử tới millisecond. M2.5 packet log không timestamp, nên chỉ dùng mốc metrics và thứ tự dòng. Không có FIN/RST packet capture. Các giới hạn này giữ nguyên nguyên nhân95 là UNRESOLVED.

Bước cần giải quyết trước lần tải tiếp theo: hiệu chuẩn input tạo hai đích khác nhau cho71; nhận dạng park qua thay đổi camera; hoàn thiện xác nhận own/friend farm và chat. Không tăng tài nguyên server để che lỗi driver.
