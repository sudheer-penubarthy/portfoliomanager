-- V3__add_fund_house_table.sql
-- Add a normalized fund house table and a foreign key from amfi_scheme

CREATE TABLE IF NOT EXISTS amfi_fund_house (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(512) NOT NULL UNIQUE,
  last_nav_date DATE DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- add fund_house_id to amfi_scheme if not present
ALTER TABLE amfi_scheme
  ADD COLUMN IF NOT EXISTS fund_house_id BIGINT NULL;

-- add FK constraint linking to amfi_fund_house
ALTER TABLE amfi_scheme
  ADD CONSTRAINT IF NOT EXISTS fk_scheme_fundhouse
    FOREIGN KEY (fund_house_id)
    REFERENCES amfi_fund_house(id)
    ON UPDATE SET NULL
    ON DELETE SET NULL;

-- make sure scheme_code length matches (already done in V1/V2 if you applied)
ALTER TABLE amfi_scheme MODIFY COLUMN scheme_code VARCHAR(128) NOT NULL;
ALTER TABLE amfi_nav MODIFY COLUMN scheme_code VARCHAR(128) NOT NULL;
