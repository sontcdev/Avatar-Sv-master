# Evidence index — Milestone 2.2

- `test-results.json`: 60 distinct methods, failures/errors/skips0 after default package + opt-in DB profiles. 33unit +27DB; reruns not added.
- `final-package.log`, `final-sales.log`, `final-farm.log`: final reviewed source. All three commands exited0. `-q` suppresses success summaries; method counts are retained in the JSON extracted from Surefire XML. Sale test24, friendship1, farm1, attendance1.
- `red-*.log`: expected pre-fix regression failures. `red-resource-close.log` contains two failures before the final review fix; these are not final suite failures.
- `reviewed-final-runtime.txt`, `reviewed-restart-runtime.txt`: delivered SHA18df… JVM identities332118→332532, unchanged production identity at checks.
- `reviewed-final-db.tsv`, `reviewed-restart-db.tsv`: final-artifact graceful restart preserves economy/attendance state, revisions3→4.
- `reviewed-final-packets.log`, `reviewed-restart-packets.log`, corresponding client PNGs: authenticated real MIDlet and map selection/rendering. No NPC850 reward claim.
- `navigation-*`, `client-map23.png`, `runtime-identity.txt`: earlier intermediate navigation and blocker.
- `final-*`, `runtime-identity-final.txt`: pre-review SHAa519… history; these names predate the close-failure review fix. They do not identify the delivered binary.
- `final-schema-and-db.tsv`: additive revision columns, no production DDL.
- `teardown-*`: only owned staging processes/tunnel/reverse stopped, production MainPID313665/NRestarts0, emulator5554 and shared ADB68690 retained.

Raw authentication logs, private DB configs, backup dumps, rejected snapshot journals and client binaries are excluded. Read the linked reports for static wire expectations versus actual observed packets and the human attendance acceptance procedure.

Commands (JAVA_HOME set to Corretto17.0.18; private config paths are placeholders, not included):

```sh
mvn -q package
mvn -q -Dtest=FarmSalePersistenceTest,FriendshipPersistenceTest -Davatar.test.isolatedSalesDb=true -Davatar.database.config=PRIVATE_SALES_CONFIG test
# Clear only farm fixture tables in avatar2d_starfruit_test before this profile; never gameplay/production tables.
mvn -q -Dtest=FarmPersistenceTest,AttendancePersistenceTest -Davatar.test.isolatedFarmDb=true -Davatar.database.config=PRIVATE_FARM_CONFIG test
```

Profiles assert exact disposable catalog names before destructive fixture setup. Staging gameplay catalog is avatar2d_gameplay_stage; no test profile points at it.

Text captures have trailing whitespace normalized; raw private originals remain outside Git.
