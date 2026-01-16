# Database Migration - Add Version Column

## Issue

The `version` column is missing from the `form_field_definitions` table, causing a 500 error when querying form schemas.

**Error:**
```
ERROR: column ffd1_0.version does not exist
```

## Solution

### Option 1: Automatic (Recommended)

If you have `ddl-auto: update` enabled (which you do), simply **restart the Spring Boot application**. Hibernate will automatically add the missing column.

**Steps:**
1. Stop the application
2. Start the application
3. Hibernate will detect the `@Version` annotation and add the column automatically

---

### Option 2: Manual SQL Script

If automatic update doesn't work, run the SQL script manually:

**File:** `src/main/resources/db/migration/add_version_column_to_form_field_definitions.sql`

**Run this in your PostgreSQL database:**

```sql
-- Add version column if it doesn't exist
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
        
        -- Set version to 0 for existing records
        UPDATE form_field_definitions 
        SET version = 0 
        WHERE version IS NULL;
        
        RAISE NOTICE 'Version column added successfully';
    ELSE
        RAISE NOTICE 'Version column already exists';
    END IF;
END $$;
```

**Or simple version (if column definitely doesn't exist):**

```sql
ALTER TABLE form_field_definitions 
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Update existing records
UPDATE form_field_definitions 
SET version = 0;
```

---

### Option 3: Using psql Command Line

```bash
psql -U postgres -d rccms_manipur -f src/main/resources/db/migration/add_version_column_to_form_field_definitions.sql
```

---

## Verify

After running the migration, verify the column exists:

```sql
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'form_field_definitions' 
AND column_name = 'version';
```

**Expected Result:**
```
column_name | data_type | column_default | is_nullable
------------+-----------+----------------+-------------
version     | bigint    | 0              | NO
```

---

## What This Column Does

The `version` column is used for **optimistic locking** to prevent concurrent modification conflicts:

1. Each field has a version number (starts at 0)
2. Version increments automatically on each update
3. When updating, include `expectedVersion` in the request
4. Backend checks: if current version ≠ expected version → 409 Conflict
5. Prevents lost updates when multiple users edit the same field

---

## After Migration

Once the column is added:
- ✅ All existing fields will have `version = 0`
- ✅ New fields will start with `version = 0`
- ✅ Updates will automatically increment the version
- ✅ Frontend can use version for conflict detection

---

## Quick Fix Summary

**Easiest:** Restart the Spring Boot application (Hibernate will add the column automatically)

**If that doesn't work:** Run the SQL script manually

**Verify:** Check that the column exists in the database

