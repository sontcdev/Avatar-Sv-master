# Crop native acceptance — milestone 3.3

**BLOCKED / PARTIAL.** [Chắc chắn] Không có catalog `farmitems` target được xác thực ở staging, nên chưa thử trồng hay thu hoạch native và chưa có deadline/sản lượng thực. Không dùng ID 6 từ recipe để đoán các trường crop. [Provenance](CROP_CATALOG_PROVENANCE.md).

[Chắc chắn] Test DB cô lập dùng row tổng hợp ID 5 kiểm atomic seed+land, reload, non, stale revision, harvest, reload và duplicate; thời gian được thay trong fixture để kiểm logic, không phải thời gian thực. Các test ownership/visitor trong suite không thay thế UI crop. Logic yield `quantity * health / 100` là legacy chưa được phê duyệt cho target. Cần xác minh catalog, tương tác 64/66 thực, countdown, persistence qua restart và mutation của visitor trước khi gọi WORKING.
