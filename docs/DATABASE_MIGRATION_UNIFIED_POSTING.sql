-- Database Migration Script for Unified Posting System
-- Adds support for unit-based postings (field officers)
-- Run this script on your PostgreSQL database

-- ============================================
-- STEP 1: Make court_id nullable
-- ============================================
ALTER TABLE officer_da_history 
  ALTER COLUMN court_id DROP NOT NULL;

-- ============================================
-- STEP 2: Add unit_id column
-- ============================================
ALTER TABLE officer_da_history 
  ADD COLUMN IF NOT EXISTS unit_id BIGINT;

-- Add foreign key constraint
ALTER TABLE officer_da_history 
  ADD CONSTRAINT fk_posting_unit 
  FOREIGN KEY (unit_id) 
  REFERENCES admin_unit(unit_id);

-- ============================================
-- STEP 3: Add unique constraint for unit-based postings
-- ============================================
-- Note: PostgreSQL doesn't support partial unique constraints directly
-- We'll use a unique index instead

CREATE UNIQUE INDEX IF NOT EXISTS uk_posting_unit_role_current 
ON officer_da_history(unit_id, role_code) 
WHERE unit_id IS NOT NULL AND is_current = true;

-- ============================================
-- STEP 4: Add indexes for performance
-- ============================================
CREATE INDEX IF NOT EXISTS idx_unit_role 
ON officer_da_history(unit_id, role_code) 
WHERE unit_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_posting_type 
ON officer_da_history(court_id, unit_id);

-- ============================================
-- STEP 5: Add check constraint (optional but recommended)
-- ============================================
-- Ensure either court_id OR unit_id is provided (not both, not neither)
ALTER TABLE officer_da_history 
  ADD CONSTRAINT chk_posting_type 
  CHECK (
    (court_id IS NOT NULL AND unit_id IS NULL) OR 
    (court_id IS NULL AND unit_id IS NOT NULL)
  );

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check table structure
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'officer_da_history'
ORDER BY ordinal_position;

-- Check constraints
SELECT 
    constraint_name, 
    constraint_type
FROM information_schema.table_constraints 
WHERE table_name = 'officer_da_history';

-- Check indexes
SELECT 
    indexname, 
    indexdef
FROM pg_indexes 
WHERE tablename = 'officer_da_history';

-- ============================================
-- ROLLBACK SCRIPT (if needed)
-- ============================================
-- WARNING: Only run if you need to rollback
-- This will fail if there are unit-based postings

-- DROP INDEX IF EXISTS uk_posting_unit_role_current;
-- DROP INDEX IF EXISTS idx_unit_role;
-- DROP INDEX IF EXISTS idx_posting_type;
-- ALTER TABLE officer_da_history DROP CONSTRAINT IF EXISTS chk_posting_type;
-- ALTER TABLE officer_da_history DROP CONSTRAINT IF EXISTS fk_posting_unit;
-- ALTER TABLE officer_da_history DROP COLUMN IF EXISTS unit_id;
-- ALTER TABLE officer_da_history ALTER COLUMN court_id SET NOT NULL;
