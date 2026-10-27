-- MariaDB forward-only; stop staging/game writers and take a verified backup first.
-- Additive, repeatable. Existing unharvested quantities are preserved.
ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS starfruit_last_harvest_ms BIGINT NOT NULL DEFAULT 0;
-- A depleted legacy tree begins regrowth at migration, never receives an immediate refill.
UPDATE farm_data SET starfruit_last_harvest_ms = CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED)
WHERE starfruit_quantity = 0 AND starfruit_last_harvest_ms = 0;
