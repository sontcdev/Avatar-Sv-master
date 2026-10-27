# Build and repairs

Use installed Corretto17 explicitly: Maven otherwise selects Homebrew Java25, incompatible with the original Lombok annotation processor.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -B clean package
unzip -p target/Avatar2D-1.0-SNAPSHOT.jar META-INF/MANIFEST.MF
```

Output: `target/Avatar2D-1.0-SNAPSHOT.jar`, shaded runnable JAR, Main-Class `avatar.server.Avatar`, Java17 bytecode. Upstream history retained; no replacement implementation, commit or push.

Verified: upstream baseline package builds on JDK17 but has incorrect main manifest; repaired package builds; two JUnit tests pass (socket EOF cleanup/idempotent disconnect and actual -27 handshake). Missing database configuration exits1 with contextual error. API9 compilation was also tested before the final logging dependency update; Java9 runtime execution was not tested. Deployed runtime requirement is Java17.

See `evidence/build-java17.log`, `evidence/avatar.server.SessionLifecycleTest.txt`, `evidence/compile-api9.log`, `evidence/jar-manifest.txt`, `evidence/startup-missing-config.txt`.

Changes reach POM, DB loader/config, game config/bootstrap, session lifecycle/queue, NPC chat publication, logging, additive schema, operational scripts and tests. Packet command numbers, authentication hashing and character serialization stay compatible. Runtime secret file is `/etc/avatar2d/database.properties` (root:avatar2d0640); repository database.properties is a credential-free development template. Runtime secret path is selected by `-Davatar.database.config`.

Build smoke tests do not establish database-backed startup, login, maps or physical Android compatibility. Deployment/functional reports distinguish those checks.

Runtime deployment follow-up: nullable map_item.map_id additive migration resolves the dump/query mismatch without guessing map placement. Boss.createSession uses the actual server listener rather than localhost:19128. Three socket JUnit tests pass after this repair; repaired JAR SHA256 c9f8f06c1fc8c125bef066bf8c0e9dd0af938dc46ca5934d407f1b5c3f2b68ce.
