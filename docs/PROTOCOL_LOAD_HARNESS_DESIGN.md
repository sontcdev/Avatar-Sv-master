# Protocol load harness — M2.7

The independent backend workload is `scripts/protocol-load.py`. It connects to the real isolated staging TCP socket through the owned localhost tunnel. It never inserts a User/Session, directly changes gameplay results, or treats internal NPC actors as players. Native UI acceptance is a separate gate.

## Wire contract

The client sends the unencrypted -27/zero-length frame, decodes the server's delta-encoded XOR key, and maintains independent cyclic read/write key cursors. Normal frames encrypt command, unsigned-short length and payload. Server command90 encrypts only its command; its four-byte length and payload remain raw, matching `Session.doSendMessage`. Java modified UTF-8 is implemented explicitly, including NUL/surrogate encoding. Login -2 sends username/password/version; -17 client-info follows `Session.clientInfo`. The harness version is2.5.0, not a claim to render Avatar2.5.8 assets.

Accounts come from a chmod600 private JSON file. Only distinct synthetic IDs71,92–100 are allowed, at most10. Host is restricted to127.0.0.1 and the two staging/tunnel ports19128/19129. This is an operator scope guard, not remote endpoint attestation; runtime/cwd/artifact identity must also be checked before running. Passwords and authentication payloads are never logged.

## Deterministic cycle

1. Authenticate against -4 carrying the expected player ID. Observe a fresh server session, then another observation at least5seconds after login. A barrier aligns all participating clients.
2. Context -1/9, join50 with map9/zone0/348,156. Verify authenticated park context from the external collector.
3. Move54 to alternating270/280,156. Match the returned54 broadcast byte-for-byte on sender ID, coordinates and direction, and corroborate server coordinates. These points lie within the previously observed348→258 horizontal corridor; the backend itself does not enforce collision geometry.
4. Send park chat55 with a unique user/sequence marker; match the exact sender ID and text in the broadcast.
5. When another client participates, send private chat -6 to another live test client. Its receiver thread must observe exact sender ID, username and unique text. Delivery timing is logged separately from sender RTT. One-player load excludes private chat.
6. Context -1/10, open own farm61 then friend farm61. Match response owner ID and corroborate FarmMsgHandler/viewed owner. The explicitly declared star fixture is71↔92–100; no invitation-acceptance claim is made.
7. Repeat within the30second action deadline. Close the actual TCP socket after an end barrier so peers do not lose a recipient during their final cycle. Repeated functional/load runs establish logout/reconnect through new real session generations.

The external CSV schema is epoch,user,session,map,x,y,zone,context,direction,farmOwner. It samples registered, logged-in users with real connected sockets every5seconds. An owned persistent SSH tail stream atomically refreshes this cache; it cannot itself generate gameplay. Every state lookup requires freshness under10seconds and the current session. A cached corroborating state may precede a request; exact wire responses establish command completion, so cache polling time is never presented as request latency.

## Completion, failure and metrics

Successful operations count only exact matched movement/chat/farm responses and verified private-chat delivery. Context acknowledgements and login do not count as active gameplay. Every5seconds the sampler reports fresh authenticated sessions and players with a completed gameplay operation in the previous30seconds. Submitted writes, idle sockets and background actors count zero. Failed requests, decoder/EOF errors or deadline expiry set a shared stop signal; all workers close their sockets. Socket writes are bounded8seconds; response/state waits are bounded8seconds and cycles30seconds. The read thread blocks on the socket until shutdown, with no unbounded main-thread wait.

The detached supervisor owns each workload process and a bounded430second level deadline. It checks read-only production identity/state and fresh host observations throughout, stopping on stale monitoring, available memory below512MiB or load1≥4. It compares20 synthetic player/farm rows between levels, excluding only revision columns. The supervisor lifetime is independent of the launching tool; owned PID records and explicit stop markers support teardown.

Server handler timings come from existing staging-only SessionTelemetry and exclude network/queue/render time. Client RTT uses only an exact post-send matched response; -6 peer delivery is a separate metric. Sample counts accompany percentiles, and sparse tails are descriptive only. The workload is deliberately low-rate and does not measure saturation or the production gameplay mix.

## Exclusions and checks

No harvest, sales, attendance reset/claim, purchases or minigame commands are generated. Native63 sales remain OFF. Rendering, camera correctness and native input reliability are not tested by TCP. No invented resource fields are sent. Farm rendering/resource downloads and fresh UI invitation acceptance remain outside this harness.

`python3 scripts/test-protocol-load.py` covers modified UTF-8, independent XOR cursors including raw command90, EOF, stale/wrong-account state, stale response rejection, shared cancellation, private-chat payload identity and scope guards. Run the native-driver checks separately. Functional1/3 passed before the long ramp. The first three-client load then exposed a backend park-join race; after its regression and fix, the complete1/3/5/10 ramp was restarted on the new artifact. Retain every failed/aborted attempt separately.

## Backend race discovered by real concurrency

The initial three-player run exposed ConcurrentModificationException in AvatarService.enter while serializing live zone membership. Zone.enter assigned the zone before serialization and added the user afterward; the exception left a player with a zone reference but outside its broadcast membership. The minimal fix snapshots the synchronized player list once and uses it for count and serialization. A deterministic regression mutates membership during getUsername and verifies a consistent packet plus successful join membership. Existing handler telemetry may still report completed=true when an inner layer catches an exception; exact response matching remains required.

The first long one-client attempt also exposed stale local state transport: sequential host/ADB/SSH polling could delay a valid five-second sample beyond the response deadline. A persistent SSH tail now separates state transport from resource collection. The failed attempt remains evidence and is not included in the final passing window.

## Login ramp versus burst failure

The first five-client simultaneous-login attempt exhausted the four-connection pool: loadData holds a connection while loadFarmData acquires another; handlers stalled approximately30seconds, beyond the eight-second response bound. That failed log is retained separately. The capacity scenario now serializes only the login request/response using LOGIN_LOCK, then keeps the same authentication barrier and simultaneous gameplay workers. All levels are rerun with this final harness. This does not fix or validate burst login, and the nested-connection backend limit remains open. Login queues are bounded by the level supervisor; they do not count as authenticated or active.
