# Milestone 2.2 — real-client evidence, 2026-10-28

## Scope and outcome

Attendance remains **PARTIAL**. Real Avatar 2.5.8 on J2ME Loader authenticated as player71, rendered staging maps9/23 and reauthenticated after a graceful staging JVM restart. NPC850 was not reached, its attendance menu was not opened and no reward claim was observed. No packet injection was used for this milestone. Private chat and friendship were not attempted: the requested attendance-first gate did not pass. Login/Admin messages are not private-chat evidence.

Evidence lives in [evidence/milestone-2-2](evidence/milestone-2-2/). Intermediate navigation and final-artifact restart captures are deliberately separated. Exact current artifact and test totals are in [the report](MILESTONE_2_2_REPORT.md).

## Captured client actions and packets

| Action | Actual observation | Evidence |
|---|---|---|
| Continue stage71; choose Thành Phố Bảo Bình | Authenticated user71; outgoing -6 messages identify Admin/id1, not a peer conversation | navigation-authenticated-packets.log |
| Select Khu giải trí | Client sends -1 with byte09; server queues map response -1/map9 | navigation-authenticated-packets.log |
| Select shopping area | Server enters map23; character/map rendered | client-map23.png; navigation-authenticated-packets.log |
| Attempt map23 portal/menu | Screen becomes seven-icon global menu, Sự kiện selected; generic interaction -61 targets0 | navigation-blocker.png; navigation-authenticated-packets.log |
| Restart staging; reconnect same MIDlet | Intermediate and final-artifact authentication and rendered city/map UI; unchanged persisted stock/currency, revision increases on graceful shutdown save | reviewed-restart-packets.log; reviewed-restart-client.png; reviewed-final-db.tsv; reviewed-restart-db.tsv |

`GAMEPLAY_PACKET QUEUED` means queued for transport, not confirmed rendered. Screenshots establish rendering only for the shown screens. Packet logs are filtered staging gameplay traces; raw authentication/configuration logs remain private outside Git.

The mis-mapped menu also opened a native Android form labelled “Gửi cho bạn bè / Số điện thoại”. No number was entered and no send was performed. Android Back did not dismiss it; native “ĐÓNG” at bottom right did. This is not a friendship or private-chat flow.

## Exact navigation blocker and input mapping

Existing emulator5554 was preserved. Screenshots are raw1280×2856; a viewer may scale them, so displayed coordinates must not be reused as raw ADB coordinates. J2ME Loader shows game canvas above a virtual keypad. Raw center/F near(640,1880) changes meaning by screen: city selection, map selection, or a global menu; tapping a displayed portal is not proof of a world transition.

Reproducible route: acknowledge disconnect dialog at(640,1610), choose “Chơi tiếp, stage71” near(640,960), select highlighted Bảo Bình with center/F, then select Khu giải trí with center/F. Allow each screen to settle. The shopping marker near(915,720), followed by center/F, produced map23. On map23, the bottom-arrow attempt near(740,1790) opened the global menu rather than reaching NPC850. Blind coordinate retries were stopped. If the native phone form appears, tap “ĐÓNG” near(1180,2700); do not enter a phone number.

The isolated DB places NPC850 (`lai.buon`) on **map25 at world(70,45)**. The authenticated handler requires same zone/map and distance≤55. The captured map23/global-menu screen does not satisfy these conditions. No incoming target2000000850 was captured.

## Manual attendance acceptance procedure

1. Start only the isolated staging MariaDB and final JAR using the existing staging configurations; apply additive revision migration with writers stopped first. Use the staging tunnel/reverse, never the production endpoint. Preserve a private pretest backup and baseline rows for players71/92, farm_data and the local game date (Asia/Ho_Chi_Minh).
2. Open the existing Avatar Stage MIDlet on emulator5554. Continue stage71 and choose Thành Phố Bảo Bình. Use the virtual directional keypad and center/F while observing the canvas; select the shopping area. A human must identify the route/portal into **map25**, then move the character to the visible `lai.buon` NPC near world(70,45). Do not assume the map23 bottom arrow or global Sự kiện menu is the NPC.
3. Capture screenshots/video of the character, NPC name, opened NPC menu and selection. At the same time retain authenticated gameplay traces with user71, incoming -61 and -59. Stop if target/map/distance is wrong; do not inject a packet to substitute for this interaction.
4. Required client request: **-61, int target2000000850**, four bytes `77359752`. Required menu response: **-59, int target, byte menu0, byte count3, three writeUTF labels** (“Điểm Danh”, “Xem hướng dẫn”, “Thoát”). Select Điểm Danh. Required incoming **-59, int target, byte menu0, byte option0**, six bytes `773597520000`. These are source/client contract expectations, **not observed attendance packets in this run**.
5. Capture the reward dialog and chest ticket593. The success/error response uses **-9/writeUTF**. Current packet trace whitelist does not include -9: capture at staging debugger `Service.serverMessage`/`Session.sendMessage` or a protocol-aware transport capture retaining the XOR state. Do not report a decoded response hex from the present logs. Expected handler reward is ticket593 +1, scores+20 and attendance_date=today in one transaction.
6. Read DB with `SELECT user_id,scores,chests,attendance_date,persistence_revision FROM players WHERE user_id=71`. Compare exact deltas to baseline and screenshot the chest. After the handler cooldown, repeat the same real UI selection: capture rejection, unchanged DB reward/date and client dialog.
7. Stop only the owned staging JVM gracefully, record PID/JAR SHA and DB snapshot, restart the same artifact, reconnect same real client and capture ticket/score/date again. A graceful restart is not a power-loss test. Repeat claim must still reject. Keep player92 untouched as a control.
8. Only after all nine requested attendance steps succeed, authenticate a separate player92 client and capture real outgoing private chat (-6), authenticated sender71 identity and recipient92 rendering. Only after chat passes proceed to invitation/accept/delete/list and farm visits. None of these downstream steps has passed in this milestone.

## Persistence observations and limits

Player71 stayed at xu20000, scores0, chests[], attendance_dateNULL. Farm stock215 stayed195, tree quantity0 and harvest anchor1793123779887. Player92 stayed xu20000/scores0/chests[]/NULL; farm stock[], tree180/anchor0. Initial intermediate restart advanced player71/farm revisions0→1; a later pre-review artifact graceful shutdown/restart advanced1→2; final reviewed artifact restart advanced3→4. These are snapshot-save revisions, **not attendance rewards**. See DB TSVs, including final schema defaults. Reconnection is **PARTIAL** as an overall gameplay flow: same-client login/map rendering and existing stock persistence were observed, but required reward/social state acceptance remains missing.

No load test was performed. Periodic staging probe metrics in private logs are not a 1/3/5/10 active-player load test. Production was not migrated, deployed, stopped or restarted.

## Artifact evidence naming

`runtime-identity.txt` and `navigation-*` refer to intermediate SHA7301…; `runtime-identity-final.txt`, `final-authenticated-*`, `final-restart-*`, `final-client-*`, `final-db-*` refer to the pre-review SHAa519… . They are historical navigation/persistence evidence, not the final reviewed binary. **`reviewed-final-*` and `reviewed-restart-*` identify the delivered SHA18df157280dc1ee2915a4a17442a6282c31d5eb3edbbbc385e119016c8d5750a**. Its actual JVMs were332118 then332532. The final reviewed restart preserved stock/currency/chest/date; player/farm revisions3→4. The city map UI rendered after reauthentication; no new NPC interaction was attempted. Runtime metrics are diagnostic only.
