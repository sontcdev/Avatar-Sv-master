# Custom Crop v1 implementation status

**BACKEND INCOMPLETE / NATIVE UNVERIFIED.** The custom branch can be exercised by the actual 64/66 handlers with injected test policy and clock, but `avatar.customCropV1=true` deliberately aborts startup: no verified runtime staging identity and item mapping provisioning mechanism has been implemented. Do not enable or deploy it. No commit was made; HEAD remains `4e1e052042deed97bf2b876998ba44bdd20985af`.

Changes: `pom.xml`, `LandItem.java`, `User.java`, `Avatar.java`, `FarmService.java`, `CustomCropPolicy.java`, and the three `CustomCrop*Test.java` classes. No schema migration is required: additive fields in each land JSON object record policy, UUID, epoch planting/deadline and output ID, and existing snapshot writers retain them. Older server versions discard these fields on save; do not roll back a server with active custom crops without first reconciling those records.

The tested custom path subtracts one seed, uses server epoch time and a 14,400,000 ms deadline, and credits two products and clears the land in the existing revision-checked farm snapshot transaction. In-memory changes are restored on a rejected SQL update; ambiguous commits use the existing uncertain state. A stale revision cannot commit over a newer farm snapshot. The 66 path refreshes inventory and farm state after the response. No currency or XP operation is introduced. Legacy behavior remains selected with the flag absent; custom records cannot be harvested through the legacy method.

Target bytecode reads 64 response as int/byte/byte and 66 as byte/short. Its 64 handler uses the response seed ID both to locate inventory and set the displayed crop ID, so synthetic IDs 2/3 do not establish native tomato rendering. Tomato crop ID 0 and four-hour visual duration are documented by the target asset; real seed and product inventory mappings remain UNVERIFIED. Test IDs 2 and 3 are synthetic only. The runtime gate does not yet accept any staging configuration, even when IDs are supplied; a validated deployment identity and native mapping mechanism still needs implementation.

Final-source Corretto 17 build: 100 unique tests passed across the regular build, isolated MariaDB opt-in groups and asset-specific run; reruns are not counted. The custom integration test uses actual handler calls and MariaDB with synthetic mappings and deterministic time; it checks planting, reload, early/exact-deadline harvest, duplicate/concurrent harvest, CAS and SQL rollback. It is not an authenticated network protocol or native UI test. `scripts/test-tools.py` passed its existing framing tests. No authenticated custom 64/66 protocol test or real-time native client test was performed.

Shaded JAR SHA-256: `1185a89da2788bc85c931d3b164fca1ff642c0033974cb75d453e1926d025671`.

A newly initialized isolated MariaDB instance on loopback port 13318 was used with disposable test databases; empty and pre-reset backups and a final backup are retained in `/tmp/avatar-customcrop-test.tomr5vWz` on the test host. No restore was needed; the test instance was shut down and its tunnel closed. Production databases, services and deployment were not touched.

Native packets contain no crop-instance operation ID. A delayed harvest packet after a cell is reused cannot be distinguished from a new request; CAS and a stored UUID do not provide cross-cycle replay protection.
