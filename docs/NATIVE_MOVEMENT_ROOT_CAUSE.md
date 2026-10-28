# Native movement diagnosis (M2.7)

[Chắc chắn] M2.6 contains two different outcomes, not one proven collision defect. Account94 changed world coordinates348→258 while the original driver rejected the moved screenshot. Account71 sent command54 after key22 but remained at258; an earlier key21 had changed348→258. See `MILESTONE_2_6_FINAL_REPORT.md` and private `/tmp/avatar2d-m26/driver-5556.jsonl`, `driver-5562.jsonl` (do not publish raw account logs).

## Confirmed driver cause

The original park recognizer required three exact colors at three fixed screen positions. On the cached800×1280 moved image for94, those positions no longer matched. The three palette colors still have126833,365,66237 pixels respectively, versus131866,283,77928 for71's cached screenshot. Camera scrolling invalidates screen-position assertions without invalidating the scene.

The driver now corroborates map9 with these three colors across the screenshot, requiring substantial sky/path coverage and at least100 dark park pixels. This is a calibrated scene heuristic, not a general image classifier; modal OCR takes precedence. Synthetic translated images and both cached M2.6 screenshots pass; a sky-only image fails. Farm rendering is never inferred from the server context. `FARM_RENDERED` remains unverified until independent farm UI calibration exists.

## Client and server evidence

Offline inspection: `javap -classpath resAndProjectUnityFileGameJar/Avatar258.jar -c -p main.a`.

- `main.a.keyPressed`: `Canvas.getGameAction`2 sets directional flag4 (left),5 sets flag6 (right). Its fallback MIDP keycodes-3/-4 also set4/6. Android ADB key21/22 are outside this jar's API boundary: the loader must translate them; jar inspection alone does not prove how the installed loader translated each event.
- `main.a.pointerPressed` and `pointerReleased` store screen coordinates in `h/i`.
- `go` touch handlers add `do.a().a/b` camera offsets to these screen coordinates before tile lookup. This proves at least this client scene treats screen and map coordinates separately; it does not establish an untested park tap target as traversable.
- `avatar.model.User.move` reads x/y/direct, directly assigns x/y/direct and broadcasts through the map service. It performs no collision check. Receiving command54 at the same x does not prove the server blocked movement, and direction-only changes are not completed movement.

[Chắc chắn] The archived evidence does not identify why71 remained unchanged after22: the archived evidence does not isolate loader translation, client selection/path state, obstacle handling or key duration. No server collision fix is justified by this evidence.

## Driver contract and next native run

`ui` preserves the existing login helper's `LOGIN`; `state` exposes `AUTHENTICATING`. Other observed states include CITY, PARK, FARM_BOOTSTRAP, FRIEND_LIST, CHAT, DISCONNECTED and ERROR. A farm context with unknown UI reports only FARM_BOOTSTRAP. FARM_RENDERED is deliberately not claimed. Server fields and UI corroboration are logged separately.

The cache accepts legacy six columns and at least nine columns: epochMillis,user,session,map,x,y,zone,context,direct; a tenth farmOwner column may be appended. Only samples aged0..10000ms authenticate. Completion needs a newer sample, identical session/map/zone/context and changed world x/y. Missing authentication, UI drift, disconnect or error stops the action. There is no retry input after failure.

The only historically proven input on the shared348→258 corridor is ADB key21. Key22 has succeeded for other accounts but is not established as a reliable reverse for71. A deterministic300-second five-client movement schedule therefore requires one observed native round trip per client before the plateau: key21, verify changed world coordinates, then key22, verify return/change. Stop at the first failed leg; do not count submitted ADB inputs, blocked coordinates, direction changes or camera changes as active movement. No guaranteed new traversable input can honestly be derived from the archived evidence alone.

Verification: `python3 scripts/native-client-driver.py --self-check`; `python3 scripts/test-native-client-driver.py`. These automated checks validate the driver contract; live M2.7 evidence is recorded below.

## M2.7 reproduction and deeper offline trace

[Chắc chắn] The parent runtime reproduced71's failure with the updated scene recognition. At epoch1793170102589 key21 produced command54 payload `0102009c020000`, changing server coordinates348→258. At epoch1793170106446 key22 produced the same `0102009c020000`; the server broadcast also contained account71 and the same coordinates. The gate stopped and native clients were shut down. Therefore the palette correction fixes94's documented recognition defect but does not fix71's unchanged movement.

Decode: x=`0x0102`=258, y=`0x009c`=156, direction=`0x02`=2, final short=0. The second input did not request a new world position. This is direct evidence against a server collision rejection as the cause of this particular packet.

Offline trace of the actual park input path in Avatar258:

1. `ae.l()` invokes `main.a.x` (`ir`) update, then `main.GameMidlet.i` (`hn`).`e()`.
2. `hn.e()` reads `main.a.d[4]` for left (bytecode offset306), sets facing `gx.K=gx.J=2` (offset348), then tests movement/collision. Its `else if d[6]` right branch (offset450) sets `gx.K=0` at offset490 **before** right collision checks at offsets521–551. If both flags are true, left wins.
3. `hn.h(dx,dy)` delegates to `ir.a(worldX+dx,worldY+dy)`; `hn.i(dx,dy)` delegates to `gx.b` and then `gx.c`. `gx.b` checks `ir.b`, which consults tile flags. Normal passable flag values include80 and51; special movement stateQ=-5 permits additional flags79/81/92/67. `ir.c` does world-coordinate bounds checks and indexes `ir.g` using tile size`ir.i` and width`ir.e`.
4. `ae.a(x,y,facing,extra)` updates local targetC/D and delegates to `bf.a(IIII)`, which emits54 and writes shortX, shortY, byteFacing, shortExtra. `ae.n()` supplies the current local player `js.aw/ax`, `gx.K`, `gx.N`. The self-broadcast handler `ae.a(int,int,int,int,short)` ignores the local player's own id, so its normal branch does not overwrite local direction from that echo.

A right branch blocked solely by collision would already have selected facing0; it does not explain a second unchanged packet still containing facing2. The available packet proves serialization of the old coordinates/facing, but does not prove which Android event or client frame produced it. A simultaneous stale left flag, an unprocessed short right press, or another caller that sends current state must still be isolated.

### Timing hypothesis and the next discriminating native check

[Có khả năng cao] A short DOWN/UP pair can be lost as a movement interval. `main.a.run()` targets one update each50ms. `keyPressed` sets held flag`d` plus press flag`b`; `keyReleased` clears held flag`d` and sets release flag`c` in the normal fallback mapping. `hn.e()` moves and changes facing from held`d`, while its release handling (offsets747–843) can send the current coordinates/facing after observing`c`. Thus DOWN and UP delivered between two player updates can produce a movement packet with unchanged coordinates and old facing. Device flags`ah/ak/E` affect release handling; their live values are unknown. This is a code-supported mechanism, **not a proven root cause for71**.

A controlled next check is one held right input (`adb … shell input keyevent --longpress 22`, with22 a separate shell argument), followed by observation of fresh world coordinates and facing0, then one held left input and observation of the reverse. It differs from the failed instantaneous keyevent only in duration. Stop on unchanged coordinates or drift; do not issue repeated inputs until one eventually works. The subsequent preparation reached park for all five accounts, but authentication expired before the controlled held-key check. The driver observed DISCONNECTED and rejected the action before submitting input. The held-key hypothesis remains UNVERIFIED; no300-second active plateau is claimed.

The inspected local resource tree contains map27 data but no map9 tile-data file, and map9's runtime`ir.g` array was not captured. The traversability of the full258↔348 corridor therefore remains unproven; a past left traversal does not establish a successful native reverse. No client/server collision logic was changed.

## Final native outcome

[Chắc chắn] NATIVE_5_ACTIVE=FAIL. First attempt stopped on71’s unchanged movement; the later held-key preparation lost authentication before calibration. No blind recovery or ten-native run followed. The second preparation is not a capacity measurement. The historical95 disconnect remains unresolved; these observations do not establish its cause. Evidence: `evidence/milestone-2-7/native-result.json`, original driver logs, and `held-native/result.json`.
