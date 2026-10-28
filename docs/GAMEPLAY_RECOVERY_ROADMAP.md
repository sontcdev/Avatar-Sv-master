# Gameplay recovery roadmap — milestone 3.6

**INCOMPLETE.** Milestone 3.6 supersedes the historical details below: BUY_ANIMAL is now BLOCKED with explicit feedback; 89 distinct tests passed on final source across normal, isolated DB and asset invocations. No playable animal/care loop or fresh native acceptance was achieved. See [3.6 report](MILESTONE_3_6_GAMEPLAY_REPORT.md).

Historical milestone 3.5: status here supersedes historical roadmap status, without upgrading earlier evidence to a fresh native verification. The milestone started at `7de246a`; the commit containing this document is the milestone Git SHA (`git log -1 --format=%H -- docs/GAMEPLAY_RECOVERY_ROADMAP.md`). Final locally built JAR SHA-256: `2555bd09294f781f43b7f745d17ff346e995d84e591fd518e5ceaa97d0e0872e`. No staging server or native client ran this artifact.

Baseline Corretto 17 clean package: 87 discovered, 52 passed and 35 skipped. Separately, the 33 opt-in DB tests and the two asset-dependent tests passed: 87 distinct baseline tests passed. After the change clean package discovered 88, passed 53 and skipped 35; the 35 opt-in tests were run on the baseline, not rerun after the one-line request framing change. Protocol tools passed on the baseline. The new regression failed before the fix and passed in the final build. No new native acceptance was performed.

The isolated test databases were backed up before tests (SHA-256 `df949157823ebe97c4a30a1285d6069c098a365179544f321b689e1c5af1c35c`) and restored afterward; the execution-owned isolated database and SSH tunnel were stopped. No production database, production service, deployment, push or merge was touched. Unrelated `logs/null_log.txt` was preserved. Native63 remains default OFF; its replay-after-replenishment problem remains unresolved.

## Current commands

| Command | Status | Evidence and next action |
|---|---|---|
| 64 PLANT_SEED | BLOCKED | Existing transaction/CAS implementation can use a populated legacy `farmitems` table, but target maturity, restrictions and other rules remain unverified. Staging catalog is empty; no rows activated. Obtain authoritative server rules/catalog. |
| 66 TREE_HARVEST | BLOCKED | This milestone rejects packets with trailing data before mutation; the behavior has a regression test. Existing yield and maturity calculations remain unverified; request framing does not make the crop lifecycle safe. Obtain authoritative yield, health, maturity and product mapping, then test real timed native lifecycle and restart. |
| 65 USING_ITEM | BLOCKED | Unity `FarmScr.resetAction` sends item 100 after local watering; `FarmService.doUsingItem` writes int farm, byte index, short item. Incoming 65 reads byte and short and decrements a matching inventory item. Authoritative aridity generation, water consumption, effect, persistence and compatibility with 2.5.8 remain missing. No care loop enabled. |
| 71 BUY_ANIMAL | BLOCKED | Explicit unavailable feedback replaces the free, non-durable legacy purchase. Obtain authoritative rules and transaction design; see [animal lifecycle](ANIMAL_LIFECYCLE_IMPLEMENTATION.md). |
| 72 PRICE_ANIMAL | BLOCKED | Unity incoming decoder reads byte index and UTF text; authoritative pricing and target request behavior remain missing. |
| 73 SELL_ANIMAL | BLOCKED | Price policy and replay-safe atomic sale missing. |
| 74 HARVEST_ANIMAL | BLOCKED | Verified output, maturity, health and persistence rules missing. |
| 78 INFO_CELL | IMPLEMENTED_UNVERIFIED | Previously identified as incoming state update, not an established request. Existing JOIN serialization uses legacy flags whose meaning differs from Unity's names; mapping needs target-specific confirmation and migration strategy. |
| 79 INFO_ANIMAL | BLOCKED | Observed as an incoming update; no request contract established. Animal lifecycle and serialized state semantics missing. |
| 84, 86 starfruit upgrades | BLOCKED | No authoritative levels, transitions or prices; JOIN's hard-coded Lv10 is not evidence of player progression. |
| 85 starfruit harvest | VERIFIED_NATIVE | Historical native UI and persistence evidence from earlier milestone only, not this artifact. |
| 87 starfruit information | VERIFIED_NATIVE | Historical native dialog evidence only, not this artifact. |
| 90 land, 94 storage upgrades | BLOCKED | No authoritative pricing, level/capacity transitions or confirmed 2.5.8 confirmation contract. |
| 91, 92 cooking | VERIFIED_NATIVE | Historical UI/persistence evidence only; 91 replay after replenishment is not solved. No 30-minute rerun this milestone. |
| 93 cooking acceleration; 83 finish starfruit | BLOCKED | Authoritative economic rules missing. |
| 95–98 theft/history | BLOCKED | Authoritative rules, atomic two-farm persistence and replay/history design missing. |
| 60 inventory; social/farm visits | VERIFIED_NATIVE | Historical native evidence only; no claim of fresh acceptance. |
| 63 Native63 sale | BLOCKED | Default OFF; replay after replenishment remains unresolved. |

## Catalog and client evidence

Target `res/data/farm_info.dat` SHA-256 `d52e67ed96ca6161606d7636c9d09e23c69f3f921dafe0c6df257771caf9cbd1`; native 2.5.8 JAR SHA-256 `34bda0739fc7854f1f8b925a18f98dbdfd99424b852a7140c1f2c2042b64314e`. The target-owned Unity decompilation archive `resAndProjectUnityFileGameJar/AvatarDKunitydecode.zip` SHA-256 `30890675f89d2977badae053161a44e3a1a543ba2c9991ad1b376faf23398c07` supplies an additional client version, not authoritative server economy. `FarmData.cs` names fields `harvestTime`, `dieTime`, `priceProduct`, `numProduct`; `FarmScr.cs` displays `numProduct` as yield and uses `harvestTime * 60` for visual stages. Displayed quantity does not establish actual server reward or health effects. After incoming 66 Unity sets harvest percentage to 100 while retaining the crop, whereas existing server clears type. These cross-version observations warrant target 2.5.8 and server-state verification before changing persistence semantics.

Earlier source and SQL inspection is recorded in [crop recovery](CROP_CATALOG_RECOVERY.md) and [next farm feature](NEXT_FARM_FEATURE_IMPLEMENTATION.md). Existing populated catalogs can activate unverified legacy logic; no verified catalog marker exists, so a default-off gate would alter existing deployments and has not been introduced silently. Seek authoritative rules and a backward-compatible migration/activation decision. No synthetic catalog is evidence of target economy.

## NPCs and later systems

The target SQL identities are recorded in [migration matrix](GAMEPLAY_MIGRATION_MATRIX.md). NPCs 50, 315, 335, 425, 465, 525, 535, 540, 590, 645, 646, 741 and 760 are **NOT_IMPLEMENTED** as verified gameplay; donor meanings are hypotheses only. Each still needs target map, native interactions, required data and effects checked before implementation; full marriage for 525 also needs consent/state-machine rules. No NPC was accepted this milestone.

Clan, Phom, Tien Len, Bau Cua, Diamond, housing, marriage, auction and remaining economy are **NOT_IMPLEMENTED** as fully verified workflows. Existing partial code is not a completed lifecycle. Proceed in the requested priority order when target rules, rights, persistence and native multi-client evidence are available.
