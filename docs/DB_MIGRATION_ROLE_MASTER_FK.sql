-- =============================================================================
-- Database migration: Role Master FK and citizen_type removal
-- Database: PostgreSQL (rccms_chandigarh)
-- Run this if you have existing data and need to align schema with the new
-- entities (Citizen/Lawyer/OfficerDaHistory use role_id FK; citizen_type removed).
-- If you use JPA ddl-auto: update, Hibernate may add new columns; use this script
-- to backfill existing rows and drop obsolete columns.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. role_master: drop unit_level if it exists (was removed from entity)
-- -----------------------------------------------------------------------------
ALTER TABLE role_master DROP COLUMN IF EXISTS unit_level;

-- -----------------------------------------------------------------------------
-- 2. Ensure required roles exist (if not already seeded by RoleDataInitializer)
-- -----------------------------------------------------------------------------
INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'SUPER_ADMIN', 'Super Administrator', 'System administrator with full access', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'SUPER_ADMIN');

INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'CITIZEN', 'Citizen', 'Citizen/applicant in the system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'CITIZEN');

INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'RESPONDENT', 'Respondent', 'Respondent in cases', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'RESPONDENT');

INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'OPERATOR', 'Operator', 'Operator role', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'OPERATOR');

INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'LAWYER', 'Lawyer', 'Advocate/lawyer', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'LAWYER');

INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'PRESIDING_OFFICER', 'Presiding Officer', 'Presiding officer (e.g. Tehsildar)', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'PRESIDING_OFFICER');

INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'DEALING_ASSISTANT', 'Dealing Assistant', 'Dealing assistant/reader', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'DEALING_ASSISTANT');

INSERT INTO role_master (role_code, role_name, description, created_at)
SELECT 'FIELD_OFFICER', 'Field Officer', 'Field officer', NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = 'FIELD_OFFICER');

-- -----------------------------------------------------------------------------
-- 3. citizens: add role_id, backfill, drop citizen_type
-- -----------------------------------------------------------------------------
-- Add column if not present (JPA may have already added it)
ALTER TABLE citizens ADD COLUMN IF NOT EXISTS role_id BIGINT;

-- Backfill role_id from existing citizen_type (if column exists)
UPDATE citizens c
SET role_id = r.id
FROM role_master r
WHERE c.role_id IS NULL
  AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'citizens' AND column_name = 'citizen_type')
  AND UPPER(TRIM(c.citizen_type::TEXT)) = r.role_code;

-- If no citizen_type column or some rows still null, set to CITIZEN role
UPDATE citizens c
SET role_id = (SELECT id FROM role_master WHERE role_code = 'CITIZEN' LIMIT 1)
WHERE c.role_id IS NULL;

-- Enforce NOT NULL
ALTER TABLE citizens ALTER COLUMN role_id SET NOT NULL;

-- Add FK and index if not exist
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_citizen_role') THEN
    ALTER TABLE citizens ADD CONSTRAINT fk_citizen_role FOREIGN KEY (role_id) REFERENCES role_master(id);
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_citizen_role ON citizens(role_id);

-- Drop old column
ALTER TABLE citizens DROP COLUMN IF EXISTS citizen_type;

-- -----------------------------------------------------------------------------
-- 4. lawyers: add role_id, backfill, add FK
-- -----------------------------------------------------------------------------
ALTER TABLE lawyers ADD COLUMN IF NOT EXISTS role_id BIGINT;

UPDATE lawyers l
SET role_id = (SELECT id FROM role_master WHERE role_code = 'LAWYER' LIMIT 1)
WHERE l.role_id IS NULL;

ALTER TABLE lawyers ALTER COLUMN role_id SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_lawyer_role') THEN
    ALTER TABLE lawyers ADD CONSTRAINT fk_lawyer_role FOREIGN KEY (role_id) REFERENCES role_master(id);
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_lawyer_role ON lawyers(role_id);

-- -----------------------------------------------------------------------------
-- 5. officer_da_history: add role_id, backfill from role_code, add FK
-- -----------------------------------------------------------------------------
ALTER TABLE officer_da_history ADD COLUMN IF NOT EXISTS role_id BIGINT;

UPDATE officer_da_history p
SET role_id = r.id
FROM role_master r
WHERE p.role_code = r.role_code
  AND p.role_id IS NULL;

-- Set NOT NULL only when all rows have role_id (all role_codes exist in role_master)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM officer_da_history WHERE role_id IS NULL) THEN
    ALTER TABLE officer_da_history ALTER COLUMN role_id SET NOT NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_posting_role') THEN
    ALTER TABLE officer_da_history ADD CONSTRAINT fk_posting_role FOREIGN KEY (role_id) REFERENCES role_master(id);
  END IF;
END $$;

COMMIT;

-- =============================================================================
-- Summary of changes
-- =============================================================================
-- role_master:     unit_level column dropped (if existed).
-- citizens:        role_id added (FK to role_master), citizen_type dropped.
-- lawyers:         role_id added (FK to role_master).
-- officer_da_history: role_id added (FK to role_master).
-- =============================================================================
