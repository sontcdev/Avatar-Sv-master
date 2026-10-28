# Farm loading — milestone 3.3

[Chắc chắn] M3.2 ghi nhận context 9; đó là park, không phải farm. Ở lần này chọn đúng mục “Nông trại” trên world map, xác nhận biểu tượng đã di chuyển xong ([ảnh](evidence/milestone-3-3/farm-select.png)), client gửi context 10, bootstrap 51/54/60, map 25 (`1900ffffffff`) và JOIN 61 owner 71 (`00000047`). Map 25 và [farm của stage71](evidence/milestone-3-3/farm.png) render, kể cả sau reconnect và restart staging. Đây là cùng đường thành công mô tả ở M2.4/M2.5. Không thêm sleep vào server, padding hay bỏ qua exception.

[Có khả năng cao] Bước chọn ở M3.2 được gửi trước khi animation/selection ổn định: ảnh M3.2 cho thấy biểu tượng đang ở khu mua sắm, còn trace đã chọn context 9. Đây là phân kỳ đầu tiên quan sát được và giải thích vì sao lần đó không đi theo route farm. Không có bằng chứng trực tiếp chứng minh nguyên nhân chính xác màn “Xin chờ” của park hoặc một regression server; không khẳng định lỗi park đã được sửa. Native log lần này không cho thấy EOF khi vào farm; sau khoảng hai phút không thao tác server đóng phiên bằng `SocketTimeoutException`, sau đó reconnect thành công.

[Chắc chắn] Bằng chứng M3.3 là own farm; chưa thực hiện lại friend farm trên artifact này. Test farm visit và park/farm isolation vẫn đạt; native friend farm là bằng chứng lịch sử M2.5, không thay thế acceptance mới.
