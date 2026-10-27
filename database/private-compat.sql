-- Additive only; column contracts come from GameData/PartManager and marriage callers.
-- No invented farm economy seed data: farm shop remains unverified/empty.
CREATE TABLE IF NOT EXISTS farmitems (
  id INT NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  time INT NOT NULL,
  quantity INT NOT NULL,
  sell INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS marry (
  idNam INT NOT NULL PRIMARY KEY,
  idNu INT NOT NULL UNIQUE,
  level INT NOT NULL DEFAULT 0,
  perLevel INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Upstream map_item has no map assignment; preserve its row as unassigned.
ALTER TABLE map_item ADD COLUMN IF NOT EXISTS map_id INT NULL;

-- Persist the initial crop previously hardcoded in the farm response.
ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS starfruit_quantity SMALLINT UNSIGNED NOT NULL DEFAULT 180;
