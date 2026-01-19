-- Add version column to form_field_definitions table for optimistic locking
-- This script adds the version column if it doesn't exist

-- For PostgreSQL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'form_field_definitions' 
        AND column_name = 'version'
    ) THEN
        ALTER TABLE form_field_definitions 
        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
        
        -- Update existing records to have version 0
        UPDATE form_field_definitions 
        SET version = 0 
        WHERE version IS NULL;
        
        RAISE NOTICE 'Version column added successfully to form_field_definitions table';
    ELSE
        RAISE NOTICE 'Version column already exists in form_field_definitions table';
    END IF;
END $$;

-- Verify the column was added
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'form_field_definitions' 
AND column_name = 'version';

