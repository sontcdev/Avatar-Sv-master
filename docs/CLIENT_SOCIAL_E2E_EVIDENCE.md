# Real-client social E2E — Milestone2.3

Two Android-hosted real Avatar2.5.8 MIDlets, authenticated synthetic stage71/id71 and stage92/id92, shared park map9zone0. No gameplay packet injection. Raw authentication logs remain private. Committed traces whitelist authenticated71/92 gameplay only. `QUEUED` is server enqueue evidence; screenshots separately establish UI rendering.

## Private chat: WORKING on intermediate artifact

Intermediate artifact SHA256 `8189d380223b94ac18563954c031e56198f78cf64014654ed15336f9934c86c9`. JVM334391 exchanged A messages; after graceful restart JVM335356 both reauthenticated and exchanged B messages. Sender identity is authenticated session, not a supplied sender field or Admin/id1.

```text
RECV user92 -6 bytes20 00000047000e4d32335f39325f544f5f37315f41
QUEUED user71 -6 bytes29 0000005c000773746167653932000e4d32335f39325f544f5f37315f41
RECV user71 -6 bytes20 0000005c000e4d32335f37315f544f5f39325f41
QUEUED user92 -6 bytes29 00000047000773746167653731000e4d32335f37315f544f5f39325f41
```

Request: int recipient+UTF message. Response: int actual sender+UTF actual username+UTF message. A messages are `M23_92_TO_71_A` and `M23_71_TO_92_A`; B repeats replace final41 with42. Client font renders underscores as spaces; captured bytes retain underscores. Screens: [92→71](evidence/milestone-2-3/chat-92-to-71.png), [71→92](evidence/milestone-2-3/chat-71-to-92.png), [restart92→71](evidence/milestone-2-3/chat-restart-92-to-71.png), [restart71→92](evidence/milestone-2-3/chat-restart-71-to-92.png). [A trace](evidence/milestone-2-3/restart-packets.log), [B trace](evidence/milestone-2-3/social-restart-packets.log). No persisted chat-history claim; messages are online delivery.

## Friend flows: subflows observed, overall PARTIAL

An initial stale invitation expired during idle automation and was not counted. Both clients reauthenticated;92 sent fresh invitation and71 accepted using UI:

```text
RECV user92 -21 bytes4 00000047
QUEUED user71 -21 bytes54 0000005c000773746167653932002773746167653932206d75e1bb916e206be1babf742062e1baa16e2076e1bb9b692062e1baa16e2e
RECV user71 -19 bytes5 0000005c01
QUEUED user92 -19 bytes46 0000004700077374616765373101001ec490c3a3206be1babf742062e1baa16e2076e1bb9b692073746167653731
QUEUED user71 -19 bytes46 0000005c00077374616765393201001ec490c3a3206be1babf742062e1baa16e2076e1bb9b692073746167653932
RECV user71 -18 bytes0
```

-21 request is int target; notification is int inviter+UTF username+UTF prompt. -19 acceptance is int inviter+boolean; acknowledgement int other+UTF name+boolean+UTF text. [Invitation](evidence/milestone-2-3/friend-invitation.png), [accepted dialog](evidence/milestone-2-3/friend-accepted.png), [list](evidence/milestone-2-3/friend-list-online.png). [DB canonical71/92](evidence/milestone-2-3/db-friend-accepted.tsv), created_at2026-10-27 21:41:12 in DB UTC (local2026-10-28).

Restart335356→336538 changed to final SHA256 `7fbfa1800e38496873f9a719108512a6bfc0dfbb5613f012603c839f5c1a57c7`; both real clients reauthenticated, [list after restart](evidence/milestone-2-3/friend-list-restart.png) and [DB](evidence/milestone-2-3/db-social-restart.tsv) retain relation. -18 returns custom list -81:

```text
000a667269656e646c697374000942e1baa16e2062c3a80000000101010001000000005cffffffffffff00077374616765393200064f6e6c696e650201000a58c3b3612062e1baa16e0200125468c4836d206ec3b46e67207472e1baa169
```

List wire: UTF friendlist, UTF title, int listID, page/config bytes, short row count; row type, int userID, three appearance shorts, UTF name/status, action count and byte/UTF action pairs. Offline response changes status to `00074f66666c696e65`; [real Offline list](evidence/milestone-2-3/friend-list-offline.png).

After farm attempt blocked UI, graceful final-artifact restart336538→337656 restored park UI; account71 reauthenticated, opened friend list and selected Xóa bạn. Actual -81 payload `0000000101000001`; response -81 is empty list; [empty UI](evidence/milestone-2-3/friend-deleted.png), [DB rows0](evidence/milestone-2-3/db-after-delete.tsv), [trace](evidence/milestone-2-3/delete-packets.log). This is actual client deletion; deletion was not itself repeated after another restart. Invite/accept, list/restart and delete are individually observed; whole friendship remains PARTIAL because acceptance spans artifacts and deletion restart persistence is not rechecked. Native separate -20 behavior is not verified here.

## Farm visits: PARTIAL; permission UI UNVERIFIED

The first real Thăm nông trại action generated -81 `0000000101000002` but was rejected. [Before-fix UI](evidence/milestone-2-3/friend-visit-rejected-before-fix.png). Client bytecode `cx.a(int,byte,int,byte)` writes int,byte,short,byte; actual request establishes those bytes as listID1,page1,index0,action2. Server reversed page/action. Existing deletion test concealed this because both were1. Literal captured request regression failed before fix and passed after; forged page2 cannot delete relation. [RED](evidence/milestone-2-3/red-friend-wire.txt), [GREEN run excerpt](evidence/milestone-2-3/green-friend-wire.txt), [final60 manifest](evidence/milestone-2-3/test-manifest.tsv). No client wire rewrite.

Final artifact actual online and offline requests both reached:

```text
RECV user71 ParkMsgHandler -81 bytes8 0000000101000002
QUEUED user71 ParkMsgHandler -1 bytes1 0a
QUEUED user71 FarmMsgHandler 61 bytes38 0000005c06ffffffffffff000a0a000a002b002e00b400000000000001010101010100030005
RECV user71 FarmMsgHandler 54 bytes2 0000
RECV user71 FarmMsgHandler 54 bytes2 0001
RECV user71 FarmMsgHandler 60 bytes0
QUEUED user71 FarmMsgHandler 60 bytes31 000000004e200000000100d700c3010000fa0001000000000100d7000000c3
```

61 begins int92, six land cells, animals and tree180. 60 is visitor71's own inventory/currency (20000xu and item215 quantity195), not owner92 inventory. Full [trace](evidence/milestone-2-3/social-final-packets.log) includes online and Offline-list attempts. However client remains at [farm loading](evidence/milestone-2-3/farm-offline-loading.png). [Client errors](evidence/milestone-2-3/farm-client-errors.log) include EOFException in `gb.d` readShort, through `kz.a/ce.a`, plus Vector removal exceptions. These errors are an observed blocker, not a proven root cause or proof which field is missing. Do not change payload from a guessed layout. No rendered owner label or real forbidden harvest action was verified. DB inventory unchanged across attempts: [before](evidence/milestone-2-3/db-farm-before-social-restart.tsv), [after](evidence/milestone-2-3/db-after-visits.tsv). Existing permission integration tests are C-level evidence only.

## Repeatable UI procedure

Use server group Xứ Sở Thần Tiên / Thành Phố Bảo Bình through local19128. After server restart dismiss disconnect, Chơi tiếp stageXX, select Bảo Bình, select Khu giải trí. Wait for actual park render before continuing; stale client canvas is not authentication evidence.

Physical virtual gamepad F selects, L opens action menu, R closes; directions navigate. F on park opens seven-icon menu. Blue card bottom-left is Bạn bè: F opens list (-18). L in list gives Cập nhật / Xóa bạn / Thăm nông trại; one down chooses delete, two down chooses visit. Center F on a list row opens private chat, not visit.

For player-to-player actions approach/select the other avatar, F→Giao dịch (handshake). Speech bubble Nhắn tin riêng opens chat, green pair Kết bạn sends invitation. Incoming notification: park global menu envelope Sự kiện→Kết bạn→F accepts. Ensure displayed menu title before F: selection is retained across sessions. Close private chat with L→Đóng, then close Tin đến if present. Input uses actual client editor with adb text, F sends; no synthetic -6 packets injected. Synthetic messages only; raw auth traces excluded from Git.
