# Milestone2.5 evidence provenance

`final/` dùng SHA2562c93ede1f6082474b7ab3a419301e9542e5969e745cf961322f71d3e8019f2bf. Các ảnh ở thư mục cha và diagnostic-old-artifact-db.tsv thuộc vòng chẩn đoán JAR cũ; không dùng làm final acceptance.

`final/friend-new-invite.png` và `friend-incoming.png` thuộc lời mời đã bị bấm reject; lời mời gửi lại được accept qua key66, ảnh friend-accepted.png và wire-19 ending01 mới là bằng chứng acceptance. Packet export giữ cả hai lần để kiểm tra được.

DB/private raw server logs/backups/config và credential RMS không commit. Export chỉ whitelist authenticated gameplay. `functional-observations.txt` ghi SQL observations trong phiên kiểm tra, không phải fresh SQL query sau khi tạo load fixtures. Farm before.tsv bắt đầu sau online case; db-invariants.txt dùng private backup trước E2E để bao phủ cả lifecycle.

`load/` giữ raw metrics toàn phiên (bao gồm preparation/ramp/invalid windows), phase-events và summary chỉ rõ các cửa sổ được đo. Level1/3 có xác minh hoạt động trên5phút; level5 chỉ xác nhận plateau auth5 với hạn chế UI93; level10 không qualified. `active-*.jsonl` ghi **ý định thao tác**, không tự chứng minh render hay thành công. Screenshot5-93-farm71 là lần visit thủ công quan sát được; không thay ảnh park-menu lỗi bằng nhãn PASS. Không xuất ảnh park-menu lỗi dưới tên farm.

M25Metrics.java/observe.py/active.py là instrumentation/driver ngoài artifact, không được package vào server. active.py lưu phiên bản cuối (pause2giây, optional skip first chat); vòng đầu dùng pause1giây. Logs start/cycle và phase timestamps mới là bằng chứng thời gian thực tế. Không suy diễn latency từ pause. Full JVM/host metrics được giữ để kiểm lại cả thời gian bị loại. Native63/property và DB credentials không có trong export.
