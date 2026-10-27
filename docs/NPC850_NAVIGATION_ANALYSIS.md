# NPC850 navigation and attendance — Milestone2.3

## A — natural world route: PARTIAL

`ParkMsgHandler` routes50 to `ServerManager.joinAreaMessage`: client chooses map byte, zone byte, x/y shorts; negative zone auto-selects. `User.move` handles54. `MapManager` creates maps/zones; NPC database rows are loaded by `ServerManager.loadNpcData`. The zone loop increments before `>=3` break, so ordinary NPCs are populated in the first **two** zones, not three. The inspected server join code is not an authoritative portal graph. Client menus/transition assets determine navigation; no verified chain from maps9/23 to25 was established. Prior map23 bottom-arrow action opened the global menu. No further blind coordinate route search is counted as evidence.

The staging DB row is id850,lai.buon,map25,x70,y45 ([snapshot](evidence/milestone-2-3/db-before.tsv)). NPC runtime ID is2000000850 (`77359752`). `NpcHandler` requires matching map/zone and distance<=55. The visible fixture screenshot and -61 request confirm actual spawn/interaction in map25 zone0. This does **not** prove natural navigation.

## B — real client interaction with controlled spawn: WORKING within this scope

Players have no persisted map/zone/x/y fields in the inspected player schema/load/save path. Login park placement follows client50. Consequently no guessed SQL location column was altered. `tools/staging/Npc850Fixture.java` runs outside Maven/JAR and observes only authenticated synthetic71, saves its zone/x/y by live-object identity, checks NPC presence and isolated database, then enters map25zone0 at90,65. Original first placement was map9zone0 x348,y156; reconnect placement recorded9/0 x-1,y-1. Both original live placements were restored before their JVMs stopped. It modifies no farm, currency or unrelated player fields. A marker requests restore and terminates fixture polling; no fixture runs in final normal Avatar JVM.

No -61/menu packet was injected. ADB presses selected the visible lai.buon NPC and actual Điểm Danh menu on Avatar2.5.8. Intermediate JAR SHA `8189d380223b94ac18563954c031e56198f78cf64014654ed15336f9934c86c9`.

Actual authenticated exchange, account71:

```text
RECV50 bytes6 09ffffffffff (original client park entry)
QUEUED50 bytes53 1900ffffffff017735975200086c61692e62756f6e0701110146000000520049012901ba0046002d000065ffff000000000000ffff
RECV-61 bytes4 77359752
QUEUED-59 bytes48 773597520003000cc49069e1bb836d2044616e68001258656d2068c6b0e1bb9b6e672064e1baab6e000654686fc3a174
RECV-59 bytes6 773597520000
QUEUED-9 bytes81 004f42e1baa16e206e68e1baad6e20c491c6b0e1bba36320323020c49169e1bb836d2063687579c3aa6e2063e1baa76e2076c3a020312076c3a920717561792073e1bb91206d69e1bb856e207068c3ad2e
```

-61 is int target; -59 response is target+menu count+UTF labels; action -59 is int target+byte menuID+byte index (zero/zero). -9 is writeUTF dialog, success shows20 attendance points and1 free spin ticket. Actual repeat and post-restart repeat return -9 bytes41:
`002748c3b46d206e61792062e1baa16e20c491c3a320c49169e1bb836d2064616e682072e1bb93692e` (“Hôm nay bạn đã điểm danh rồi.”).

[Menu](evidence/milestone-2-3/attendance-menu.png), [success](evidence/milestone-2-3/attendance-success.png), [repeat](evidence/milestone-2-3/attendance-repeat.png), [restart menu](evidence/milestone-2-3/attendance-restart-menu.png), [restart rejection](evidence/milestone-2-3/attendance-restart-rejected.png).

DB71: xu20000 unchanged; scores0→20; chests[]→one id593 quantity1 expired-1; attendance_date NULL→2026-10-28; revision5→6. Repeated UI claims preserve reward/date/quantity. Graceful JVM restart333504→334391, actual client reconnect and same menu action: reward persisted, repeat rejected, revision7 reflects save. Account92 control remains scores0,chests[],dateNULL. [Before](evidence/milestone-2-3/db-before.tsv), [claim](evidence/milestone-2-3/db-after-claim.tsv), [repeat](evidence/milestone-2-3/db-after-repeat.tsv), [restart](evidence/milestone-2-3/db-after-restart.tsv), [postrestart repeat](evidence/milestone-2-3/db-after-restart-repeat.tsv). Full filtered [first trace](evidence/milestone-2-3/fixture-packets.log) / [restart trace](evidence/milestone-2-3/restart-packets.log) include restore markers.

## C — simulation

Existing attendance integration test and diagnostic test passed, including actual -9 UTF trace assertion. These are regression evidence, separate from B and cannot upgrade A. Attendance overall remains PARTIAL.
