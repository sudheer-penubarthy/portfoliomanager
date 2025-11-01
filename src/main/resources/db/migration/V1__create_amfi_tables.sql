-- V1__create_amfi_tables.sql
-- Creates AMFI scheme master, NAV history and import audit tables.

-- Master table for AMFI schemes / funds
CREATE TABLE IF NOT EXISTS amfi_scheme (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  scheme_code VARCHAR(32) NOT NULL,     -- scheme code as published by AMFI
  isin_dividend VARCHAR(64),            -- ISIN Div Payout (nullable)
  isin_growth VARCHAR(64),              -- ISIN Growth (nullable)
  scheme_name VARCHAR(1024) NOT NULL,
  fund_house VARCHAR(255),
  instrument_type VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  replaced_by_scheme_code VARCHAR(32),
  metadata JSON DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ux_amfi_scheme_code UNIQUE (scheme_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- NAV history table (one row per scheme_code + nav_date)
CREATE TABLE IF NOT EXISTS amfi_nav (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  scheme_code VARCHAR(32) NOT NULL,
  nav_date DATE NOT NULL,
  nav_value DECIMAL(28,8) NOT NULL,
  source VARCHAR(255) DEFAULT 'AMFI',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY ux_scheme_date (scheme_code, nav_date),
  CONSTRAINT fk_nav_scheme FOREIGN KEY (scheme_code) REFERENCES amfi_scheme(scheme_code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Imports / audit table for each AMFI file ingest
CREATE TABLE IF NOT EXISTS amfi_import (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(512),
  source_url VARCHAR(1024),
  file_date DATE,                        -- date as reported inside file if present
  rows_processed INT DEFAULT 0,
  rows_inserted INT DEFAULT 0,
  rows_skipped INT DEFAULT 0,
  status ENUM('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
  error_message TEXT,
  raw_content_location VARCHAR(1024),    -- optional pointer to raw file on disk / object store
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Helpful indexes
CREATE INDEX idx_amfi_scheme_fundhouse ON amfi_scheme(fund_house(100));
CREATE INDEX idx_amfi_scheme_name ON amfi_scheme(scheme_name(255));
CREATE INDEX idx_amfi_nav_date ON amfi_nav(nav_date);

