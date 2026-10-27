-- Stop every writer and take a verified backup first. MariaDB additive, repeatable.
-- Requires the new JAR for ALL writers; older binaries do not maintain revisions.
ALTER TABLE players ADD COLUMN IF NOT EXISTS persistence_revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS persistence_revision BIGINT NOT NULL DEFAULT 0;
