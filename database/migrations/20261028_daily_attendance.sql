-- MariaDB forward-only; backup and quiesce writers before applying.
-- Never reset the day on restart or repeated migration.
ALTER TABLE players ADD COLUMN IF NOT EXISTS attendance_date DATE NULL DEFAULT NULL;
