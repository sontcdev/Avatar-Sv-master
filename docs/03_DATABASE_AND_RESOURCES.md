# Database and resources

Upstream dump header reports MariaDB10.4.32. It uses InnoDB, utf8/utf8mb4 general/unicode/bin collations, JSON-valid CHECK constraints, MariaDB-compatible text defaults and DATE/current_timestamp syntax. Prefer Ubuntu's MariaDB10.11, maintained through February2028 ([maintenance policy](https://mariadb.org/about/)). MySQL compatibility is not assumed.

The 8.3MiB dump defines32 tables and includes CREATE/ALTER/INSERT, SQL mode/time-zone changes and transaction statements. Static search found no DROP/TRUNCATE/CREATE DATABASE/USE/DEFINER/TRIGGER. DDL is not transactionally rolled back in MariaDB. Provisioning refuses an existing avatar_2x schema or avatar2d account unless its own completed marker exists. An interrupted first import requires manual inspection; it will not overwrite partial data.

Provision only `avatar_2x`; app account `avatar2d@127.0.0.1` has SELECT/INSERT/UPDATE/DELETE on that schema, no DDL/admin access. Password generated remotely using cryptographic randomness, saved root:avatar2d0640 outside source. Database listens on127.0.0.1:3306; existing PostgreSQL clusters are preserved. Default new MariaDB buffer pool is expected128MiB but must be measured. No shared DB tuning is overwritten.

Bundled user accounts are disabled and their password hashes randomized on initial import. Dedicated test account is created by `sudo python3 /opt/avatar2d/current/scripts/create-test-account.py avatartest`, role-1, zero paid balance, normal character creation. It refuses existing accounts. Credentials remain in root-only `/etc/avatar2d/test-account-avatartest.txt`.

`private-compat.sql` adds missing farmitems/marry tables; farmitems has no authentic seed rows. Existing settings table is empty, so config.properties supplies defaults. Maps are constructed in Java (60 maps,10 zones each); NPC/item/image/food data come from SQL. Do not claim complete farm/marriage compatibility from schema creation alone.

Archive `res.rar` lists48,411 entries; all paths checked beneath res with no traversal/absolute paths before extraction using macOS bsdtar. Extracted footprint approximately192MiB. Required layout exists: res/hd, res/medium, res/data, res/map, res/p. No symlinks found in extracted resources. Production preserves the complete tree, with root-owned read-only assets and logs symlinked to /var/log/avatar2d.

Validation still requires real import counts, startup loader logs and client resource requests. See deployment evidence for completed checks; do not equate archive extraction with assets rendered on Android.

Runtime verification:34tables,28NPC,5496items; settings0 (source defaults). Normal active test-account count1, bundled admin/ad active count0. Additive map_item.map_id NULL migration resolves query/schema mismatch while retaining the ambiguous upstream row as unassigned. Verified backup at /var/backups/avatar2d/20261027T095440-306559; scratch restore rehearsal still pending.
