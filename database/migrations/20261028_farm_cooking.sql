-- Apply to an authorized database after backup. No inventory or currency rewrite.
ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS cooking_recipe INT NOT NULL DEFAULT 0;
ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS cooking_ready_ms BIGINT NOT NULL DEFAULT 0;
ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS cooking_output_id INT NOT NULL DEFAULT 0;
