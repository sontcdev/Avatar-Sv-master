# Native command 63 replay analysis — 2026-10-28

Status: **BLOCKED** for live sales. Sales remain OFF; no production price or deployment changed.

## Verified legacy contract

Bundled Avatar258.jar bytecode: `it.b(short)` emits farm command 63, then `kj.p(int)` writes the product short, then flushes. The farm handler requires exactly two payload bytes. Product 215 is `00 d7`. The other command 63 occurrence in `kj.f()` is in a different context and is not an economy request identifier. Framing/XOR key cursors are transport positions, not durable authenticated operation IDs. TCP ordering removes transport duplicate delivery within a connection; it does not establish player intent for a resubmitted application command. No usable operation ID, inventory revision or correlation was found in this request. The XOR transport is not a cryptographic message-authentication mechanism.

## Reproduction using the real isolated database

`FarmSalePersistenceTest.legacyPacketReplayAfterLegitimateHarvestSellsNewStockKnownBlocker` preserves the original `00 d7` payload, owns 20, commits sale at synthetic 7/unit (100→240), invokes the real `User.harvestStarfruit()` to harvest another 20 and persists/reloads that stock, then replays the saved payload. Result: balance 380 and stock 0. The tree fixture sets available 20/clock 0 solely to avoid waiting for growth; inventory replenishment itself uses the production harvest/persistence path. This is an integration characterization of a vulnerability, not real-client sale evidence or a passing replay defense.

`staleFarmSnapshotCanRestoreSoldStockKnownBlocker` loads a second User before sale, sells 20, then saves the old farm snapshot. Persisted stock becomes 20 again while committed money stays 240. Row locks during sale do not protect later unconditional legacy farm writes. No claim of safe multiple sessions/JVMs is supported.

## Transaction fault evidence

The isolated MariaDB fixture additionally exercises concurrent duplicate requests, two stale sale callers, DB constraint failure, integer overflow, malformed inventory, missing ownership/price, and reload. JDBC-boundary fault proxies retain real queries/locks: commit-before-delegate failure leaves 100/20; commit-success/ACK-loss leaves 240/0 while RAM remains 100/20, marks outcome uncertain and blocks farm saves/logout snapshot; rollback failure marks uncertain and blocks farm writes. Pool connection cleanup rolls back the uncommitted transaction in the rollback-failure test. These do not simulate every physical database/network failure or prove a JVM restart through a real client.

## Decision and possible extension

A server-only cooldown, timestamp, payload hash, in-memory flag or server-generated nonce cannot distinguish old `00 d7` from a legitimate later sale of 215. Neither new fields appended to legacy 63 nor a fabricated client nonce is acceptable.

A future client/server extension must negotiate an explicit capability and use a separately verified command/layout. Bind an unpredictable operation ID and expected durable inventory revision to the authenticated player, product, quantity and approved catalog revision. In one DB transaction, enforce a unique player/operation ledger, compare inventory revision, deduct stock, credit currency and persist the result; retries return the same committed result. All other inventory writers, including harvest/logout/reconnect, must use compatible revision checks. Negotiation must preserve legacy decoding, with legacy live sales disabled. Modified-client ownership/licensing and distribution permission have not been established; no modified economy client or unverified layout was shipped.

An explicitly accepted private-server residual-risk policy could relax the requirement, but no such acceptance exists. Pricing approval alone is insufficient. Dependency: authorized compatible client protocol plus durable inventory writer concurrency contract, followed by packet/client/restart verification. Until then sales stay OFF.

## Milestone 2.2 follow-up

M2.2 closes stale snapshot overwrites through revision checks, but keeps replay-after-replenishment characterization and live63 BLOCKED/OFF. No protocol extension or production price approval. See [current report](MILESTONE_2_2_REPORT.md) and [client evidence](MILESTONE_2_2_E2E_EVIDENCE.md). Historical failures remain evidence of the earlier artifact.
