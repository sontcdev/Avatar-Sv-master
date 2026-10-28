# Gameplay migration matrix — 2026-10-28

## Milestone 3.2 — trạng thái mới nhất

[Chắc chắn] **PARTIAL**: sửa atomic planting và kiểm DB fixture, nhưng catalog cây target chưa xác minh và staging `farmitems` trống. Cooking 91/92, starfruit 87 chưa đạt native acceptance; client xác thực trên JAR cuối nhưng dừng khi chuyển khu. 86 unique PASS (53 unit + 33 DB), không có gameplay loop native hoàn chỉnh. Native63 OFF; không production/deploy. [Báo cáo](MILESTONE_3_2_GAMEPLAY_REPORT.md), [cooking](COOKING_CLIENT_E2E.md), [crop](CROP_GAMEPLAY_IMPLEMENTATION.md). Các mục milestone cũ bên dưới là lịch sử.

## Milestone 2.6 — lịch sử

[Chắc chắn] **PARTIAL;10 active CCU UNVERIFIED**. Cổng5 native thất bại tại readiness:71 không đổi tọa độ,94 đổi tọa độ nhưng camera làm driver nhận diện sai;92/93/95 có completed movement. Auth5 chỉ có100.074giây/21mẫu, không đủ300giây active; dừng,không chạy10/protocol generator. M2.5 disconnect95 vẫnUNRESOLVED. Final JAR SHA256 `12c781f70a001d6b487a1614740bfe3e1702e66fbc582c0f4783c3608f4b0331`;63uniquePASS(36unit+27DB). Có handler timing theo command,không clientRTT;DBquerytime chưa có. Native63OFF,production giữ nguyên,fixture đã restore sau writerstop. Natural-route attendance vẫnPARTIAL. [Final report](MILESTONE_2_6_FINAL_REPORT.md),[load evidence](LOAD_TEST_10_CCU_EVIDENCE.md),[performance](PERFORMANCE_LATENCY_REPORT.md).

## Milestone 2.5 — lịch sử

[Chắc chắn] **Milestone2.5 tổng thể PARTIAL**: functional acceptance đạt trong phạm vi fixture đã nêu; tải1/3 có cửa sổ native hoạt động trên5phút, mức5 giữ auth322giây nhưng vòng UI chưa đủ tin cậy, mức10 dừng khi auth10→8. **Chưa xác nhận10 active CCU**; p95 chưa đo được. Không dùng số kết nối để thay acceptance hoạt động.

## Milestone 2.5 — functional evidence

[Chắc chắn] Final JAR SHA256 `2c93ede1f6082474b7ab3a419301e9542e5969e745cf961322f71d3e8019f2bf`, **62 unique PASS (35unit+27DB)**. Fresh native UI friend invite/accept/list/restart/delete/restart-empty, chat mới hai chiều sau reconnect, friend farm online/offline/restart/reverse và authenticated server-dispatch unauthorized85 rejection đã được chứng minh cùng artifact. Native UI giấu harvest của owner khi là visitor; không gọi injection là UI85. Fresh93 nhận593×1/+20scores, duplicate UI bị từ chối, reward giữ qua restart; NPC interaction dùng staging spawn fixture, chỉ route map25 là tự nhiên. Complete natural-route claim vẫn chưa chứng minh.

Fix hẹp M2.5 đổi park weather2→-1 theo native decoder, tránh empty-vector timer AIOOBE làm treo điều khiển. Không đổi kiến trúc, writer, schema, social parser hay Native63. Live sales **OFF**, không production cutover. [Final report](MILESTONE_2_5_FINAL_REPORT.md), [client E2E](MILESTONE_2_5_CLIENT_E2E.md), [load report](LOAD_TEST_REPORT.md). Backlog rộng bên dưới vẫn giữ nguyên; M2.5 không đồng nghĩa migration hoàn thành.

## Milestone 2.4 — lịch sử

[Chắc chắn] Final JAR SHA256 `ce491e4c159591c540128f1cea38e9e3b373766fa257949f6265cf5b0159f268`, **61 unique tests PASS (34unit+27DB)**. Sửa park55 lọt vào farm decoder gây EOF và thứ tự native bootstrap/JOIN. Client thật cùng artifact render own71,71→92online/offline,92→71; attendance fixture92 nhận thưởng/repeat/restart rejection; chat hai chiều; delete friends và empty sau restart. Đường world-map “Nông trại” tới map25 được chứng minh bằng packet1900 và ảnh,không phải “Khu ngoại ô” map17.

[Chắc chắn] Toàn milestone **PARTIAL**: visitor unauthorized85 từ UI,friend visit sau restart,chat reconnect repeat vàaccept mới chưa đủ bằng chứng cùng artifact. Không nâng trạng thái bằng unit test hay fixture relation. Native63 live **BLOCKED/OFF**;không load/cutover,production giữ nguyên. [Final report](MILESTONE_2_4_FINAL_REPORT.md), [root cause](FRIEND_FARM_EOF_ROOT_CAUSE.md), [E2E evidence](FRIEND_FARM_E2E_EVIDENCE.md). Các milestone bên dưới là lịch sử.


## Milestone 2.3 — current status supersedes historical results

[Chắc chắn] Baseline `af048f6` clean; **60 unique tests pass (33 unit +27 DB)** on Java17. Real Avatar2.5.8 clients verified fixture-based NPC850 attendance (+20scores,ticket593,date), repeat and restart rejection; bidirectional private chat and reconnect repeat; friend invitation, acceptance, list across restart and deletion. Evidence spans intermediate/final artifacts and is explicitly labeled. Fixed native -81 parser page/action order using captured client bytes and failing-then-passing regression.

[Chắc chắn] Natural navigation to map25 remains **PARTIAL**. Attendance overall **PARTIAL** despite successful fixture interaction. Friendship overall **PARTIAL** (artifact/restart limits); online/offline farm visit **PARTIAL**: owner92 packets received but client farm-loading EOF blocks rendered owner/permission verification. Native63 **PARTIAL, live BLOCKED/OFF**; replay characterization retained. No load test or cutover; production untouched.

Current [M2.3 report](MILESTONE_2_3_REPORT.md), [NPC navigation/fixture](NPC850_NAVIGATION_ANALYSIS.md), [social UI/wire/DB evidence](CLIENT_SOCIAL_E2E_EVIDENCE.md). Final JAR SHA256 `7fbfa1800e38496873f9a719108512a6bfc0dfbb5613f012603c839f5c1a57c7`. Historical sections below do not supersede these results.


## Milestone 2.2 — current status supersedes historical results

[Chắc chắn] Baseline `98d85d7` clean; **60 unique tests pass (33 unit +27 DB)** on Java17. Stale committed inventory/currency overwrite is guarded by persisted player/farm revisions and atomic logout; ambiguous commit/resource-close failures fail closed, rejected snapshots are retained privately. Duplicate login preserves the active owner. Additive schema applied only to isolated staging; mixed old/new writers are unsupported.

[Chắc chắn] Attendance remains **PARTIAL**: real client71 authenticated/rendered maps9/23/reconnected, but NPC850 map25/menu/claim was not reached. No reward/double-claim E2E evidence. Chat/friends/visits remain PARTIAL and downstream gates were not attempted. Native63 remains **PARTIAL, live BLOCKED/defaultOFF**: replay after replenishment is still reproduced. No load test or production cutover.

Current [report](MILESTONE_2_2_REPORT.md), [client evidence/manual procedure](MILESTONE_2_2_E2E_EVIDENCE.md), [stale-session root cause](STALE_SESSION_ROOT_CAUSE.md). Final JAR SHA256 `18df157280dc1ee2915a4a17442a6282c31d5eb3edbbbc385e119016c8d5750a`. Sections below are historical and do not supersede this result.


## Milestone 2.1 — kết quả thay thế trạng thái cũ

[Chắc chắn] Tiếp tục từ `79e939f48403feb054e6ce2ac509bf45d3b04929`, ban đầu worktree clean. Java 17 build đạt; **47 test khác nhau đạt: 32 unit + 15 DB opt-in**, không cộng rerun. JAR SHA256 `9ef00532c343e4dfd44db8aace5fcf67702b70e913a95f39c3150fedb8106008` trùng artifact chạy staging. Đã sửa boss/bot tự chiếm player TCP slots và null-zone khi client gửi context 8. Cùng MIDlet 71 reconnect được sau restart bản cuối, nhưng toàn bộ acceptance E2E vẫn **PARTIAL**.

[Chắc chắn] Test DB tái hiện replay sau harvest mới (100→240→380 xu) và stale snapshot khôi phục kho đã bán. Native 63 chỉ chứa product short, không có operation ID dùng để deduplicate bền vững. **Live sales BLOCKED, mặc định OFF**; không đổi packet layout, không có giá production. Commit ACK loss/precommit/rollback failure đã được fault-inject ở JDBC boundary trên DB cô lập; không phải crash hoặc client E2E test. Backup production **PARTIAL**, load gate chưa đạt, không cutover.

Bằng chứng và giới hạn hiện hành: [MILESTONE_2_1_FINAL_REPORT.md](MILESTONE_2_1_FINAL_REPORT.md), [CLIENT_CONNECTIVITY_DIAGNOSIS.md](CLIENT_CONNECTIVITY_DIAGNOSIS.md), [NATIVE63_REPLAY_ANALYSIS.md](NATIVE63_REPLAY_ANALYSIS.md). Các mục Milestone 1/2 bên dưới là lịch sử, không dùng để nâng trạng thái Milestone 2.1.


[Chắc chắn] Target là working tree local đã bảo toàn trong nhánh `codex/gameplay-migration`, không phải chỉ snapshot upstream. Ma trận mô tả code tại nhánh này. Không có tính năng mới nào được gọi là COMPLETE chỉ dựa trên build/unit test.

## Milestone 2 hiện tại

[Chắc chắn] Baseline `4b81b28` được tiếp tục; tổng41tests khác nhau đạt, trong đó10DB opt-in trên instance cô lập. Bạn bè và native63 có implementation mới nhưng vẫn **PARTIAL** do thiếu real-client/restart evidence; sale còn replay sau replenishment và giá production chưa duyệt, defaultOFF. Hai client login/render cùng khu vui chơi chưa đủ chứng minh social flow. Attendance/visit/reconnect vẫn PARTIAL. Không load test/cutover. [Bằng chứng và phần chưa đạt](MILESTONE_2_EVIDENCE.md), [backup readiness](PRODUCTION_BACKUP_READINESS.md).

## Nguồn và cách đọc bằng chứng

- T: `src/main/java/` và `database/avatar_2x.sql` trong target, cộng các sửa local được giữ nguyên ở commit baseline của nhánh.
- P: [avatar-remake](https://github.com/ductd26100/avatar-remake/tree/970a9f9b30eb655943e455616b44bea3656d6166), SHA `970a9f9b30eb655943e455616b44bea3656d6166`; đường dẫn Java bên dưới tính từ `src/main/java/`.
- S: [new_avatar](https://github.com/thuongazplay/new_avatar/tree/a30f3693756221c688f1350e748d7beb03470e41), SHA `a30f3693756221c688f1350e748d7beb03470e41`.
- C: client Avatar 2.5.8 bundled; `javap` class `ce` và `kz` xác nhận decoder social/farm. Chỉ ghi nhận định dạng, không đưa bản disassembly hay client donor vào commit.
- WORKING: có bằng chứng thực thi trong phạm vi ghi rõ; PARTIAL: có logic nhưng thiếu bước hoặc chứng minh E2E; STUB: phản hồi mẫu/bảo trì; MISSING: không có implementation; UNVERIFIED: chưa đủ bằng chứng. Các trạng thái áp dụng cho target, trừ cột ghi rõ donor.
- Mức tích hợp S/M/L/XL là ước lượng, không phải thời hạn. Mọi hàng P/S đều bị chặn **sao chép code/data**: kiểm tra cây Git tại SHA ghim không tìm thấy LICENSE/COPYING/NOTICE cấp quyền áp dụng. Repository public không được coi là giấy phép. Các sửa trong nhánh là implementation độc lập; chưa kết luận pháp lý về quyền phân phối toàn dự án.

## Các nhóm chức năng

| Nhóm / target hiện tại | Donor đã truy vết, có logic hay stub | Wire / persistence / phụ thuộc | Rủi ro, chi phí và điều kiện còn thiếu |
|---|---|---|---|
| Chat riêng PARTIAL: `message/ParkMsgHandler` → `service/AvatarService.chatToUser/chatTo`; đã sửa ID gửi hằng 1 và receiver offline | P `service/AvatarService.chatToUser/chatTo`: ID xác thực, kiểm tra receiver/privacy; có logic | -6 request `int receiver, UTF content`; response `int sender, UTF name, UTF content`; C `ce` offset968. Chat online không lưu lịch sử | S; packet test đạt, cần 2 client thật; chưa port privacy/offline inbox |
| Bạn bè PARTIAL: `ParkService` có invite/accept/list/delete và canonical InnoDB persistence | P `AvatarService.handleAddFriend`, `ParkService.requestFriendList/handleCustomList`, User pending request; `friends` table + CustomList, popup | -21 request int target; native response `int inviter, UTF name, UTF text` (C `ce` offset822). -19 response `int id, UTF name, boolean, UTF text` (offset869), không được gửi int+boolean như target cũ. -18 list trả native custom-list -81; action1 delete/action2 visit; -20 native remove chưa triển khai | L; pending invitation5phút/max32, chống unsolicited accept; isolated DB invite→accept→reload list→delete đạt. Giới hạn100friends, chưa client E2E/restart; native-20 UNVERIFIED |
| Clan MISSING workflow: target có `clans`, `clan_members`, player.clan_id nhưng chưa có nghiệp vụ đầy đủ | P `AvatarService.createClan`, join/leave/accept/reject/kick; `NpcHandler.createClanMenus`; implementation có gọi procedures | -82 CHANGE_CLAN, -81 CUSTOM_LIST; SQL `sp_CreateClan`, `sp_JoinClan`, `sp_LeaveClan`, `sp_AcceptClanRequest`, `sp_RejectClanRequest`, `sp_KickClanMember`; icons, membership/status/permissions | XL; không tương thích chỉ bằng copy bảng. Phải audit từng procedure/rollback/role/replay + client trước tích hợp |
| Khế PARTIAL: `User.harvestStarfruit/getStarfruitQuantity/saveFarmData/loadFarmData`, `FarmService.harvestStarfruit` | P `User.getStarfruitTime/getStarfruitQuantity`, `FarmService.harvestStarFruit`: có hồi theo phút và kho, XP/quest. S `FarmService.joinFarm` **gán quantity=30 khi now>=plantedTime**; `harvestStarFruil` reset time. Không tái sử dụng reset này | 85 request rỗng; response short215, shortquantity (C `kz` offset2248). Nhánh thêm native85 + mốc epoch ms vào cùng farm_data với kho. Capacity local180, hồi3 quả/phút, tối đa60phút; mốc tương lai cho0. Legacy chưa hái giữ nguyên, đã cạn bắt đầu hồi từ migration | M; unit + isolated DB + client thật hái180/DB persist đạt; khởi chạy mới client sau restart, hồi và hái tiếp15/kho195 đạt; sale/upgrade chưa đủ. P capacity phụ thuộc level, level10=65 khác local180; không port bảng capacity hay upgrade |
| Bán nông sản/khế PARTIAL: `FarmService.sellFarmitm` | P `FarmService.sellProduct` có lookup product, cộng tiền/xóa kho/XP/quest; **không thấy transaction chung kho–tiền trong method** | 63 request short product; response int earned,int balance,short product (C `kz` offset1207). P `FarmManager`, `FarmProduct`, `farm_products`; T chỉ `farmitems` catalog trống, thiếu giá215 | L; transaction FOR UPDATE players→farm_data, kho–xu cùng commit; 7DBtests success/reload/rollback/concurrency/overflow/invalid JSON/normal product đạt. Catalog defaultOFF, chưa giá production/client sale/restart; native63 thiếu nonce nên replay sau harvest mới chưa giải quyết |
| Trồng/thu hoạch PARTIAL: T `FarmService.plandSeed/treeHarvest`, `User.harvestCrop`, `PartManager.loadFarmItemData` | P `FarmService.plantSeed/harvest`, `LandCell`, `FarmManager`, quests có logic | 64 int owner,byte cell,byte seed; 66 int owner,byte cell → byte cell,short yield; 61 farm snapshot,60 inventory. farm_data JSON; T farmitems khác P farm_items/products | L; thu hoạch local có persist+rollback từ baseline; trồng/mua còn lưu theo logout, catalog chưa seed thật. Kiểm tra crop clock/HP/owner/restart toàn chu trình còn thiếu |
| Thăm trại PARTIAL: T `FarmService.joinFarm` đã sửa dùng owner online hoặc snapshot offline, thay vì dữ liệu visitor gắn ID owner | P `FarmService.visitFriendFarm/joinFarm`, friend farm context, offline load có logic | 61 int owner → owner/land/animals/starfruit snapshot; T `User.loadFarmData` nay ghi chính instance, không session.user. Không cho visitor hái khế | M; online wire unit đạt; snapshot DB đạt, nhưng offline visit bằng client, privacy và stealing chưa đủ |
| Vật nuôi PARTIAL: T `Buy_ANIMAL` tạo Animal; không đủ vòng đời | P `buyAnimal/havestAnimal/getPriceAnimal/sellAnimal`; FarmAnimal/Animal aging/progression có logic | 71 byte kind,byte currency;72/73/74 int owner,byte animal; animal JSON, `farm_animals`, `farm_products`, barn/pond | XL; T chấp nhận type chưa kiểm catalog, không có pricing/growth/product chuẩn. Không bật lifecycle khi thiếu catalog + atomic purchase/sale |
| Minigame PARTIAL/STUB: T Casino có board/menu và mẫu kết quả; Phom/TienLen/Diamond classes rỗng; Race handler chưa được Session route | P `ongame/{Board,Room,BoardManager,OnGameService,TienLenService,PhomService,DiamondService,BauCuaService}` có state/rules/result/calculateMoney; không chỉ class rỗng | Session GET_HANDLER context; board cmds6/7/8/9/16/20/21… có nghĩa theo game. P room/board/User models khác T `BoardInfo` và server.BoardManager; rewards qua User tiền | XL; cần đa client finish/drop/rejoin/timeout và ledger trả thưởng. P source có logic không đồng nghĩa rewards persistent/atomic. T Tài Xỉu riêng không chứng minh card games |
| Nhà PARTIAL: T `HomeService`, User.buyHouse; mua đồ đã có guard local, ownership mua nhà còn trừ tiền trước insert | P `HomeService.buyHouse/buyItemHouse/sortItemHouse/deleteItemHouse/updateHouse/transPartChest`, có rental+date_expired; `doJoinHouse4` **vẫn stub** | -65 JOIN_HOUSE, -104 JOIN_HOUSE_4; BUY_ITEM_HOUSE/SORT_ITEM_HOUSE/TRANS_PART_CHEST theo Cmd; house_buy/house_player_item/chests_home khác model ngày thuê P | XL; cần kiểm owner từng mutation, atomic tiền+nhà, hết hạn thuê, restart. Không port joinHouse4 như tính năng hoàn thành |
| Hôn nhân STUB/PARTIAL: T `ParkService.WEDDING_BIGINHanlder` gửi cặp ID7/1; chỉ update level, chưa lễ cưới thật | P `ParkService.handleWeddingBegin/acceptMarry` có logic với đối tượng; User marry/spouse models khác; không chứng minh E2E | WEDDING_BIGIN theo Cmd; marry(idNam,idNu,level,perLevel) T compat; request/accept/đồng thuận/spouse/restart chưa xác minh toàn bộ | XL; tuyệt đối chưa COMPLETE; cần client đôi và kết quả DB cả2 người, không dùng ID cứng |
| Điểm danh PARTIAL mới triển khai độc lập: NpcHandler850 → User.claimDailyAttendance | P `NpcHandler.handleNpc`, case LAI_BUON → “Báo danh hàng ngày”: vé593 +20scores, check localdate; `User.updateRollCall` lưu `players.roll_call` tách reward | -61 int NPC, menu action qua int target,byte menu,byte select; response server message. T migration thêm attendance_date, 1 conditional UPDATE chests/scores/date nguyên tử; ngày Asia/Ho_Chi_Minh | M; kiểm isolated DB gồm replay, concurrency, write failure, ngày sau/tương lai; thiếu real-client click và logout/restart. Không port cách ghi ngày tách reward |
| Đấu giá STUB: T NpcHandler186 dialog bảo trì; không có AuctionManager | P `manager/AuctionManager.start/bid/open/close/checkOpen/loadItems`: thread10s, INTERVAL7, lowest unique bid qua TreeMap, có thưởng online/offline | NPC menu/input, không phải một packet bid cố định tương thích; auction_items + Item/User.updateChests; bid state giữ RAM, settlement chưa có durable idempotent ledger | XL; nhiều bidder/restart giữa charge & award/offline/replay chưa đạt; chưa bật |
| Di chuyển PARTIAL: T GlobalHandler target5/menu0 select0..2 cùng sendCityMap; select3 không có nhánh | P menu/city handling trong NpcHandler/AvatarService, FarmService.handleDichChuyen/visitFriendFarm có logic | menu `int target,byte menu,byte selection`; city/map/context handlers phải kiểm từng lựa chọn C; không suy diễn destination từ nhãn | M; cần map contract + client đi lại từng option; chưa sửa thành destination bịa |

## 19 command farm chưa hỗ trợ

[Chắc chắn] T `message/FarmMsgHandler.onMessage` gom đúng 19 nhãn dưới đây về dialog. Thông báo rõ lỗi **không phải** implementation gameplay. `service/FarmService` của P là nguồn method trừ khi ghi khác. I=int32, B=byte, S=int16, Z=boolean; “chưa chốt” nghĩa chưa có contract response đủ để port.

| Cmd / trạng thái T | P: method và mức triển khai thực tế | Request P / response, phụ thuộc và chặn | Chi phí |
|---|---|---|---|
|91 COOKING STUB|cooking, có recipe/state|S recipe;91 response trạng thái/recipe, cần FarmFood/farm_foods, cooking timer + nguyên liệu; response đầy đủ chưa chốt|L|
|92 HARVEST_COOK STUB|harvestCook, có credit/quest|rỗng;92 + inventory; cần cooking deadline, atomic consumption/reward|L|
|93 NAU_NHANH STUB|quickCook, có xác nhận/trừ tiền|B phase;93 popup/result; giá/timer/tiền chưa port|L|
|65 USING_ITEM STUB|useItem, có branches chăm sóc|I owner,B index,S item;65 action response; farm_support_items, health/water/feed, owner|L|
|72 PRICE_ANIMAL STUB|getPriceAnimal, có giá|I owner,B index;72; cần farm_animals/age/price; response chưa chốt|M|
|73 SELL_ANIMAL STUB|sellAnimal, có xóa/cộng tiền|I owner,B index;73; atomic tiền–animal/replay chưa đạt|L|
|74 HARVEST_ANIMAL STUB|havestAnimal, có product|I owner,B index;74; product/timer/capacity/quest|L|
|78 INFO_CELL STUB|sendCellInfo là **push helper**, không thấy request case78|chưa chốt request;78 response trạng thái LandCell, không copy helper rồi coi request đã hỗ trợ|M|
|79 INFO_ANIMAL STUB|sendAnimalInfo là **push helper**, không thấy request case79|chưa chốt request;79 state Animal khác T|M|
|84 UPDATE_STARFRUIL STUB|updateStarFruit, có level/cost|B phase;84/86 confirmation/result; Constants level capacity, tiền|L|
|86 UPDATE_STARTFRUIT_BYMONEY STUB|quickUpdateStarFruit, có logic|B phase;86/84; cost,time,level; không dùng capacity P để ghi đè local|L|
|87 GET_INFO_STARFRUIT STUB|getInfoStarFruit gửi thông tin, không native87 được xác minh|rỗng; dialog; C2.5.8 farm87 không có nhánh riêng trong switch đã đọc|M|
|83 FINISH_STARFRUIT STUB|constant có, **không case triển khai trong P FarmMsgHandler**|chưa chốt; không coi constant là tính năng|L|
|90 UPDATE_LAND STUB|upgradeLand, có logic|B index,B phase;90 confirmation/result; level,cost,LandCell model|L|
|94 UPDATE_STORE STUB|constant có, **không case trong P FarmMsgHandler**|P User có level_store/capacity_store nhưng chưa tìm đủ workflow94; contract UNVERIFIED|L|
|95 STEAL_INFO STUB|constant có, **không case trong P FarmMsgHandler**|không chứng minh implementation stealing; quota/history/schema/response UNVERIFIED|XL|
|96 STEAL STUB|constant có, **không case trong P FarmMsgHandler**|owner!=self không đủ; quota ngày, ghi nhận nguyên tử2kho còn thiếu|XL|
|97 STEAL_STORE STUB|constant có, **không case trong P FarmMsgHandler**|contract và chống replay UNVERIFIED|XL|
|98 LICH_SU_BI_TROM STUB|không case/method tương ứng đã xác minh|history table + query scope + pagination UNVERIFIED|L|

Ngoài19: 63 sale PARTIAL/defaultOFF, REQUEST_FRIENDLIST(-18) trong context farm là menu trộm STUB; mở rộng chuồng/hồ cũng dialog. Không đưa -18 lên handler chung để vô tình phá context. `FarmManager.load()` của P đọc `farm_items`, `farm_animals`, `farm_products`, `farm_support_items`, `farm_foods`; T `farmitems(id,name,time,quantity,sell)` và S `farmitems(ID,ImageID,Type,Action,Description,PriceXu,PriceLuong,IsItem)` là ba schema khác nhau, kể cả khi tên giống.

## Đối chiếu đủ 28 NPC trong dữ liệu target

[Chắc chắn] Danh sách lấy từ INSERT `npc` trong `database/avatar_2x.sql`, không lấy từ constants hay README. T routes `NpcHandler.handlerCommunicate` → switch `NpcName` và GlobalHandler menu. Mọi NPC dùng request COMMUNICATE(-61) int offsetID; ID wire=`2,000,000,000 + id`. Menu callback request int targetID,byte menuID,byte selection; bảng ghi nhóm hành động, không khẳng định mọi submenu đã chạy bằng client. Các hàng shop cần item catalog/giá và transaction; mọi hàng đều có ràng buộc license P nêu trên.

|ID / DB name|T handler / menu và status|P `NpcHandler.handleNpc` / dependency / khả năng port|
|---|---|---|
|50 tien.ca|MISSING → fallback|TIEN_CA: shop gender189/198 via ItemManager; M|
|95 tho.kim.hoan|PARTIAL: nâng cấp xu/lượng, hướng dẫn|THO_KIM_HOAN nâng cấp; Upgrade/ItemManager/giá; L|
|165 saitama|PARTIAL: mặt/mắt, số ô, xóa trùng|SAITAMA menus; inventory model khác; L|
|185 quay số|PARTIAL: lượng/xu/free, hướng dẫn|QUAY_SO/DialLuckyManager; dial_lucky/catalog/free entitlement; L|
|186 đấu giá|STUB: bảo trì|DAU_GIA/AuctionManager có logic nhưng không durable ledger; XL|
|315 giam.thi|MISSING|GIAM_THI hướng dẫn/trường học, Teacher models; M|
|335 phi.hanh.gia|MISSING|PHI_HANH_GIA shop188 ItemManager; M|
|425 phu.thuy.bong.toi|MISSING|PHU_THUY_BONG_TOI shop195/199 theo gender; M|
|430 tho.cau|PARTIAL: câu cá/shop, hướng dẫn|THO_CAU shop/fishing; FishManager/timers/catalog; L|
|465 ve.so|MISSING: chỉ có code comment|VE_SO có menu; rewards/catalog/lottery chưa E2E; L|
|525 Chủ Hôn|MISSING NPC workflow; wedding service có ID cứng|CHU_HON constant không có case tương ứng đã xác minh; không nhận là donor hoàn thiện; XL|
|535 zombie|MISSING|ZOMBIE có nhánh shop; labyrinth/rules không được chứng minh chỉ từ menu; L|
|540 bac.thay.phu.thuy|MISSING|BAC_THAY_PHU_THUY **dialog đơn thuần**, không có gameplay để port; S/M|
|590 pretty.witch|MISSING|PRETTY_WITCH shop193; ItemManager; M|
|645 co.giao.tieng.anh|MISSING|CO_GIAO_TIENG_ANH hướng dẫn; TeacherEnglish/chat lesson state; L|
|646 co.giao.tieng.viet|MISSING|CO_GIAO_TIENG_VIET hướng dẫn; TeacherVietnamese/chat lesson state; L|
|741 an.xin|MISSING|AN_XIN/popup xử lý quyên góp; amount/score/transaction; M|
|760 pharaoh|MISSING|PHARAOH shop191 + hướng dẫn; maze completion chưa chứng minh; L|
|761 cua.hang|PARTIAL: shop local|**ID collision** P761=MEOW_MEOW, P CUA_HANG=783; không map theo tên enum; L|
|850 lai.buon|PARTIAL: điểm danh mới + hướng dẫn|LAI_BUON dailyquests/báo danh/đổi quà; roll_call,Item593,score,quest models; L|
|857 chu.dau.tu|PARTIAL: mua biệt thự|CHU_DAU_TU mua/thuê/upgrade HomeService,date_expired; L|
|858 Bạch.vô.thường|PARTIAL nhưng sai nghĩa tên: bunma, đổi quà/góp kẹo/top|P BACH_VO_THUONG constant, không case tương ứng xác minh; không port event quà theo sốID; L|
|859 Hắc.vô.thường|PARTIAL nhưng sai nghĩa tên: Vegeta/event|P HAC_VO_THUONG constant, không case tương ứng xác minh; L|
|860 Tien.chi.mu.Lovanga|PARTIAL: dự đoán người yêu;2 prediction còn placeholder|TIEN_CHI_MU_LOVANGA có menu; spouse/lottery rules cần riêng; L|
|861 phu.thuy.bong.toi|PARTIAL nhưng mapping local Shop_Buy_Luong|P không cùng ID shop lượng (P888); M/L|
|862 Pay.To.Win|PARTIAL: shop đổi quà/hướng dẫn|không có cùng case xác minh; custom event economy local; L|
|863 Chay.To.Win|PARTIAL: shop/hướng dẫn|không có cùng case xác minh; custom event economy local; L|
|864 Than.Tai.Xiu|PARTIAL: cược xu/lượng, lịch sử/top/thành tích|TAI_XIU manager/bet/game_rounds; rewards and restart tests còn thiếu; XL|

Có13 ID thiếu handler gameplay trong target (50,315,335,425,465,525,535,540,590,645,646,741,760). Có handler không có nghĩa WORKING; đấu giá là stub, nhiều event có sai nghĩa giữa DB và enum.

## Chốt phạm vi chứng minh

[Chắc chắn] Unit/DB test xác minh contract social/63/85, friend persistence, sale atomic/rollback/concurrency/overflow, owner snapshot online, timer/rollback/concurrency khế, attendance atomic conditional update. Các hàng PARTIAL còn cần client thật và restart; các hàng MISSING/STUB không được tính là di chuyển xong. Source P có ích để định vị nghiệp vụ nhưng chưa đủ điều kiện code reuse hay staged replacement. Xem MIGRATION_REPORT và MIGRATION_ARCHITECTURE_DECISION để biết các gate còn lại.

## Milestone 3.1 update

Cooking 91/92 đã triển khai và VERIFIED_UNIT_DB; 87 đã triển khai với unit test nhưng chưa có native UI acceptance. Không coi milestone hoặc playable loop đã hoàn tất: xem [trạng thái 19 lệnh](FARM_GAMEPLAY_IMPLEMENTATION_STATUS.md), [bằng chứng client](FARM_CLIENT_E2E_EVIDENCE.md) và [báo cáo](MILESTONE_3_1_GAMEPLAY_REPORT.md). M2.7 chứng minh 10 active TCP protocol clients hơn 5 phút; native 5/10 vẫn chưa được xác nhận.
