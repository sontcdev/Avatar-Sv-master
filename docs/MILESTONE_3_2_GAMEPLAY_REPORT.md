# Milestone 3.2 — gameplay

**PARTIAL.** Chưa có vòng gameplay hoàn chỉnh từ UI native đến kết quả bền vững. Không xem kiểm thử giả lập hoặc DB fixture là acceptance client.

- 64: sửa trồng để kiểm catalog `farmitems`, lưu hạt và ô đất cùng farm revision trong một transaction; rollback RAM nếu ghi lỗi. Test tái hiện lỗi cũ trước sửa. Staging có **0 dòng farmitems**, nên không chứng minh được vòng cây target; không chép catalog donor hay suy đoán quy tắc.
- 66: giữ thu hoạch có CAS hiện hữu. Fixture DB tổng hợp kiểm tra trồng, tải lại, non, stale revision, thu hoạch, tải lại và duplicate; không chứng minh thời gian/sản lượng target. Xem [crop](CROP_GAMEPLAY_IMPLEMENTATION.md).
- 91/92: logic và DB tests tiếp tục đạt, nhưng client chưa gửi được lệnh; không bắt đầu mẻ và không chứng minh countdown/restart/collect. Xem [cooking](COOKING_CLIENT_E2E.md).
- 87: unit hiện hữu kiểm quyền và dialog; chưa chứng minh UI. Server chỉ có sức chứa cục bộ 180, không lưu level cây khế; không thể khẳng định hiển thị level chính xác.

Corretto 17: `clean package` 86 ca khai báo, 53 PASS và 33 DB skip; chạy riêng 31 sales/friendship/cooking và 2 farm/attendance trên DB test cô lập đều PASS, tổng **86 ca khác nhau**. `scripts/test-tools.py` PASS. Farm DB fixture dùng thời gian giả để kiểm logic, không phải đồng hồ acceptance. Final JAR SHA-256 `a763c3b44ca639411849e70f8277ff7f61fc52b57a33223588690eadd6dab255`, khớp bản chạy staging; client 71 xác thực được trên bản này.

DB staging và hai DB test đã được backup trước migration/test (`m32-before.sql`, SHA-256 `6ab004904f1a27a285a2a43496c8dca6e915e539dd69a70619762d3989661eff`, riêng tư trên staging). Chỉ áp dụng lại migration cooking additive vào staging; không áp dụng production. Không bật Native63, quick cook hay cancellation. Không thay đồng hồ hoặc tạo state cooking. Không thực hiện load test mới.

Sau khi ngắt emulator và dừng writer staging, farm 71 vẫn `nongsan=[]`, cooking recipe 0; revision tăng 60 → 61 do logout thông thường. Không có fixture nguyên liệu hay vị trí cần hoàn nguyên; snapshot sau thử được lưu riêng. Đã dừng JVM/DB staging, tunnel và emulator do lần này tạo; production service vẫn PID 313665, NRestarts 0.
