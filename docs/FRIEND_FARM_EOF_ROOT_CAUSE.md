# Friend farm: EOF decoder và thứ tự khởi tạo

[Chắc chắn] EOF đã tái hiện trên Avatar 2.5.8 tại `gb.d → gb.b → kz.a → ce.a → gv.run`, thao tác `DataInputStream.readByte/readShort`. Đây là EOF trong bộ đệm payload đã giải mã, không phải bằng chứng TCP bị cắt. [Stack thực tế](evidence/milestone-2-4/baseline-client-errors.log), [màn hình chờ](evidence/milestone-2-4/baseline-friend-farm-loading.png).

## Hai nguyên nhân độc lập

1. Session chuyển handler10 nhưng vẫn còn trong danh sách người chơi park. Park broadcast55 (chat) trùng command55 của farm (metadata ảnh). Một payload park thực tế dài29 byte:

   `773594b9001748c3a379207468616d20676961206e676179206ec3a06f`

   Park đọc `int sender=2000000001`, `UTF text` dài23 byte. Farm `gb.d` đọc hai byte đầu thành `short count=30517`, rồi mỗi phần tử gồm hai short và bốn byte. 27 byte còn lại không đủ cho 30517 phần tử; decoder đọc quá cuối buffer. Không thiếu byte trong command61 và không cần nối thêm padding. Command54 park movement cũng trùng farm image. Regression dùng đúng payload này rồi chứng minh context10 loại visitor khỏi park trước ACK; broadcast sau đó không tới visitor.

2. Chỉ rời park chưa đủ: gửi61 ngay sau ACK handler10 chạy trước native bootstrap. Client có thể dựng map24 quá sớm; khi60 đến, nhánh hoàn tất tải không chạy. Client tiếp tục hiện “Đang lấy dữ liệu nông trại…”, kể cả khi xóa cache farm và tải lại54/55/56. Bytecode `kz` xử lý60: sau pending resources bằng0, gửi50/map25 và61/self. `go.a` dùng cờ `bf`: JOIN đầu tiên là preload (`false`), sau đó bật `true`; JOIN tiếp theo mới dựng cảnh farm. Chỉ thay owner ở JOIN preload đưa client ra lobby map25, chưa mở farm bạn.

## Sửa tương thích

- `Session.getHandler`: rời park cho context10 trước khi ACK; reset farm visit state khi đổi context.
- `ParkService.friendListAction`: giữ owner đã chọn trong FarmService của riêng session; đợi native resource/bootstrap, không gửi farm sớm.
- `FarmService.joinFarm`: trả bootstrap JOIN theo request; sau đó trả JOIN owner đã chọn đúng một lần. Hai phản hồi có cùng layout61 đã được client hỗ trợ; phản hồi thứ hai thực hiện visit sau preload. Không sửa MIDlet ngoài endpoint staging.
- Trace opt-in bổ sung51/55/56 để phân biệt resource và park; mặc định tắt, chỉ sau authentication, hex tối đa512 byte.

## Contract và so sánh byte

Native -81 vẫn là `int listId, byte page, short index, byte action`; captured visit `0000000101000002`, delete `0000000101000001`.

61 fixture sáu ô trống dài38 byte, đối chiếu `kz.b(jr)`:

| Offset | Kiểu | Nội dung |
|---|---|---|
| 0 | int | owner71=`00000047`, owner92=`0000005c` |
| 4 | byte | landCount6 |
| 5..10 | byte ×6 | `ff` = ô trống; ô có cây dùng các trường của `writeInfoCell` |
| 11 | byte | animalCount0; con vật dùng `writeInfoAnimal` |
| 12..13 | byte ×2 | kích thước chuồng/hồ10/10 |
| 14..15 | short | cấp cây10 |
| 16..19 | short ×2 | image43/46 |
| 20..21 | short | số khế tính theo thời gian của owner |
| 22..27 | short ×3 | trường trạng thái0 |
| 28..33 | byte ×6 | trạng thái ô đất1 |
| 34..37 | short ×2 | nấu ăn3/5 |

Own bootstrap và friend response dùng cùng counts, field order và ảnh; điểm khác có ý nghĩa là owner. Số khế ở cây71 có thể trở lại180 dù DB lưu quantity0 vì getter tái sinh theo `starfruit_last_harvest_ms`; đây là quy tắc hiện có. Command60 vẫn là inventory/currency của visitor, không lấy inventory của owner.

[Trace ứng viên cuối](evidence/milestone-2-4/final-gameplay-packets.log) ghi length đầy đủ nhưng payload lớn hơn512 byte chỉ là prefix, không được coi là dump ảnh hoàn chỉnh. Trong cùng session farm sau ACK không còn park55; image responses54 có PNG header hợp lệ và client dựng được cây/đất. Online/offline dùng chung serializer; offline gọi `owner.loadFarmData(ownerId)`. Không thay đổi writer persistence, schema, sale63 hoặc kiểm tra revision.

## Regression

`FarmVisitTest.farmContextLeavesParkBeforeOverlappingResourceCommands` tái hiện incompatible decoder rồi kiểm tra rời park. Test RED trước sửa được lưu [tại đây](evidence/milestone-2-4/red-context.log). `FriendshipPersistenceTest` dùng captured -81, yêu cầu không có61 trước resource completion, sau native61/self có đúng hai owner71→92. Forged page không xóa quan hệ. Existing owner/visitor, stale session, rollback, concurrency và revision tests được giữ lại.
