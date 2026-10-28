# Milestone 2.6 — Final report

[Chắc chắn] **PARTIAL — cổng năm native client FAIL tại readiness; không chạy mười. Kết quả10 active CCU: UNVERIFIED.** Tuân thủ điều kiện dừng; không tăng pool/thread/heap/timeout để làm test qua.

## Identity và thay đổi

Baseline clean branch `codex/gameplay-migration`, HEAD `36d80e2f1c5c8105a34e3d9a33be9378884e1e8f`. HEAD chứa source implementation: `f51b2a9ba4e5c1cda272b0f47744768fe9a8daf1`; commit báo cáo tiếp theo chỉ bổ sung tài liệu/evidence. Source Java được chạy native là cùng source với HEAD này. Không push/merge/deploy/cutover.

Final local JAR `target/Avatar2D-1.0-SNAPSHOT.jar` và isolated `m26-final.jar` cùng SHA256:

```text
12c781f70a001d6b487a1614740bfe3e1702e66fbc582c0f4783c3608f4b0331
```

Thêm telemetry handler/disconnect mặc định OFF, khóa opt-in + staging directory, không ghi payload. Packet trace opt-in thêm epoch/session. Giữ logic dispatch, session cleanup, authorization, context isolation và stale-write revision. Driver mới quan sát OCR/server-state và ghi completion thật; timeout dừng. Driver vẫn PARTIAL vì marker park không chịu được camera thay đổi; không tuyên bố đã hoàn thiện farm/chat automation.

## Acceptance và nguyên nhân

Năm client Avatar2.5.8 thật71/92/93/94/95 vào park và cùng xác thực. Plateau auth5 có21 mẫu5 giây/span100.074 giây, không đủ300 giây. Readiness movement:92/93/95 completed;71 gửi54 nhưng tọa độ giữ258;94 tọa độ đổi nhưng camera làm marker thất bại. Dừng ngay cổng; own/friend farm/chat M2.6 chưa đạt bước kiểm chứng. Không đếm thao tác ADB, login hay idle thành active.

M2.5 client93 force-stop là phục hồi automation chủ động; drift liên quan driver mù trạng thái nhưng nguyên nhân từng lần chưa đủ log. Client95 disconnect M2.5 vẫn **UNRESOLVED**, không khẳng định idle timeout hoặc server lỗi. M2.6 không tái hiện disconnect95/ANR/decoder exception; năm EOF sau đó do chủ động đóng client teardown. Phép thử worker riêng tái hiện child không detached bị mất khi tool cha kết thúc; đây là lỗi harness có bằng chứng, chưa chứng minh mọi lần cũ chết cùng cách.

[Diagnosis](NATIVE_CLIENT_DRIVER_DIAGNOSIS.md) phân loại SERVER/CLIENT/EMULATOR/AUTOMATION/NETWORK/UNRESOLVED và giới hạn timestamp; [load evidence](LOAD_TEST_10_CCU_EVIDENCE.md) có serial, session, per-client activity, auth/active mỗi5 giây và teardown. Không chạy protocol-level10, không giả định có máy bổ sung. Nhãn cuối **UNVERIFIED**, không phải NATIVE_CLIENT_10_CCU_VERIFIED hoặc PROTOCOL_LEVEL_10_CCU_VERIFIED.

## Tests và performance

**63 unique PASS =36 unit +27 isolated DB;0fail/0error trong tập executed hợp nhất.** Package mặc định36 PASS/27 opt-in skip; sau đó sales/friendship25 và farm/attendance2 chạy riêng trên disposable catalogs13317 đã backup. Không đếm skip hoặc rerun thành test mới. [Manifest](evidence/milestone-2-6/test-manifest.tsv).

- Corretto17: `mvn -B clean package`.
- `-Davatar.test.isolatedSalesDb=true -Dtest=FarmSalePersistenceTest,FriendshipPersistenceTest test`, private sales DB config.
- `-Davatar.test.isolatedFarmDb=true -Dtest=FarmPersistenceTest,AttendancePersistenceTest test`, private farm DB config; chỉ tái tạo catalog test này sau backup.
- `python3 scripts/test-tools.py` và `python3 scripts/native-client-driver.py --self-check`: PASS. Guard telemetry không bật ở production directory; driver không gửi input thiếu auth hoặc tính submit thành completion.

[Performance report](PERFORMANCE_LATENCY_REPORT.md):103 handler samples phân command; movement54 n11,p50/p95/p99=0.543/0.744/0.744ms. Đây là processing server, **không phải client RTT**. Plateau auth5 CPU5.09% một core, heap20.01MiB/RSS103.40MiB,GC0,poolwait0. Query latency DB chưa có; Performance SchemaOFF. Đây là phép đo readiness ngắn, không phải backend capacity benchmark.

## Integrity, production và teardown

[Integrity](evidence/milestone-2-6/integrity-result.json):20 hàng player/farm của10 accounts giữ nguyên gameplay columns; năm logout tăng revision1, còn lại không đổi. Không claim attendance/harvest/sales. Bốn friendship fixture được gỡ sau JVM writer dừng; friendship total về0. Backup trước chạy, sau dừng writer và disposable test DB được giữ ngoài Git.

Native63 sales **OFF**: không đặt catalog property; không sửa giá/policy/schema. Production Java313665, MariaDB302726, avatar2d active/NRestarts0 được quan sát read-only, giữ nguyên; không kiểm SLA production. [Cleanup](evidence/milestone-2-6/cleanup.txt) xác nhận loopback19129/13317 không còn listen, năm owned emulator và hai owned SSH forward đã dừng, shared ADB không reset. Dừng JVM359161 trước restore fixture, DB sau tests. Clone read-only không save profile.

Complete natural-route attendance tiếp tục **PARTIAL** theo evidence fixture milestone trước; không thử claim mới. Không mở rộng19 farm commands, clan, auction, minigames hay marriage. Milestone chưa đóng acceptance tải; báo cáo giữ đúng điểm dừng thất bại để lần sau sửa driver trước khi tăng tải.
