# Case Migration Guide - Updating Old Cases

## Overview

When you configure workflows and set initial states, **new cases** automatically use the configured initial state. However, **old cases** that were created before workflow configuration need to be updated.

## Automatic vs Manual

### ✅ Automatic (New Cases)
- **New cases** automatically use the initial state (`isInitialState = true`)
- No action needed - system handles it automatically

### ⚠️ Manual (Old Cases)
- **Old cases** remain in their current state
- Need to run migration script/service to update them

---

## Option 1: Using API Endpoints (Recommended)

### Step 1: Initialize Workflow Instances for Old Cases

If old cases don't have workflow instances, create them:

```bash
POST /api/admin/cases/migrate/initialize-workflow-instances
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
  "success": true,
  "message": "Workflow instance initialization completed",
  "data": {
    "processedCount": 150,
    "successCount": 145,
    "skippedCount": 3,
    "errorCount": 2,
    "errors": [
      "Case 5: Workflow not found: OLD_WORKFLOW",
      "Case 12: Initial state not found"
    ]
  }
}
```

### Step 2: Reset Cases to Initial State

If you've changed the initial state and want to reset old cases:

```bash
POST /api/admin/cases/migrate/reset-to-initial-state
Authorization: Bearer <admin_token>
Content-Type: application/json

# Reset all cases
{}

# OR reset specific cases
[1, 2, 3, 5, 10]
```

**Response:**
```json
{
  "success": true,
  "message": "Reset to initial state completed",
  "data": {
    "processedCount": 145,
    "successCount": 140,
    "skippedCount": 3,
    "errorCount": 2,
    "errors": []
  }
}
```

---

## Option 2: Direct SQL Script

### Script 1: Initialize Workflow Instances for Cases Without Instances

```sql
-- Find cases without workflow instances
SELECT c.id, c.case_number, c.status, ct.type_code, ct.workflow_code
FROM case_entity c
LEFT JOIN case_type ct ON c.case_type_id = ct.id
LEFT JOIN case_workflow_instance cwi ON c.id = cwi.case_id
WHERE c.is_active = true
  AND cwi.id IS NULL
  AND ct.workflow_code IS NOT NULL;

-- Initialize workflow instances for cases without them
INSERT INTO case_workflow_instance (
    case_id,
    workflow_id,
    current_state_id,
    assigned_to_unit_id,
    created_at,
    updated_at
)
SELECT 
    c.id as case_id,
    w.id as workflow_id,
    ws.id as current_state_id,
    c.unit_id as assigned_to_unit_id,
    NOW() as created_at,
    NOW() as updated_at
FROM case_entity c
JOIN case_type ct ON c.case_type_id = ct.id
JOIN workflow_definition w ON ct.workflow_code = w.workflow_code
JOIN workflow_state ws ON ws.workflow_id = w.id AND ws.is_initial_state = true
LEFT JOIN case_workflow_instance cwi ON c.id = cwi.case_id
WHERE c.is_active = true
  AND cwi.id IS NULL
  AND ct.workflow_code IS NOT NULL
  AND w.is_active = true;

-- Update case status to initial state
UPDATE case_entity c
SET status = ws.state_code,
    updated_at = NOW()
FROM case_type ct
JOIN workflow_definition w ON ct.workflow_code = w.workflow_code
JOIN workflow_state ws ON ws.workflow_id = w.id AND ws.is_initial_state = true
JOIN case_workflow_instance cwi ON c.id = cwi.case_id
WHERE c.case_type_id = ct.id
  AND cwi.workflow_id = w.id
  AND c.is_active = true
  AND c.status != ws.state_code;
```

### Script 2: Reset Cases to Current Initial State

```sql
-- Reset all cases to their workflow's current initial state
UPDATE case_workflow_instance cwi
SET 
    current_state_id = ws.id,
    updated_at = NOW()
FROM workflow_definition w
JOIN workflow_state ws ON ws.workflow_id = w.id AND ws.is_initial_state = true
WHERE cwi.workflow_id = w.id
  AND cwi.current_state_id != ws.id;

-- Update case status to match
UPDATE case_entity c
SET 
    status = ws.state_code,
    updated_at = NOW()
FROM case_workflow_instance cwi
JOIN workflow_state ws ON ws.id = cwi.current_state_id
WHERE c.id = cwi.case_id
  AND c.is_active = true
  AND c.status != ws.state_code;
```

### Script 3: Reset Specific Cases

```sql
-- Reset specific cases (replace case IDs)
UPDATE case_workflow_instance cwi
SET 
    current_state_id = ws.id,
    updated_at = NOW()
FROM workflow_definition w
JOIN workflow_state ws ON ws.workflow_id = w.id AND ws.is_initial_state = true
WHERE cwi.workflow_id = w.id
  AND cwi.case_id IN (1, 2, 3, 5, 10)  -- Replace with actual case IDs
  AND cwi.current_state_id != ws.id;

-- Update case status
UPDATE case_entity c
SET 
    status = ws.state_code,
    updated_at = NOW()
FROM case_workflow_instance cwi
JOIN workflow_state ws ON ws.id = cwi.current_state_id
WHERE c.id = cwi.case_id
  AND c.id IN (1, 2, 3, 5, 10)  -- Replace with actual case IDs
  AND c.is_active = true;
```

---

## When to Run Migration

### Scenario 1: First Time Setting Up Workflows
**When:** You have old cases without workflow instances
**Action:** Run Script 1 or API endpoint `initialize-workflow-instances`

### Scenario 2: Changed Initial State
**When:** You changed which state is initial (e.g., from `CITIZEN_APPLICATION` to `DA_ENTRY`)
**Action:** Run Script 2 or API endpoint `reset-to-initial-state`

### Scenario 3: Specific Cases Need Reset
**When:** Only certain cases need to be reset
**Action:** Run Script 3 or API endpoint `reset-to-initial-state` with case IDs

---

## Verification

### Check Cases Without Workflow Instances

```sql
SELECT COUNT(*) as cases_without_instances
FROM case_entity c
LEFT JOIN case_workflow_instance cwi ON c.id = cwi.case_id
WHERE c.is_active = true
  AND cwi.id IS NULL;
```

### Check Cases Not in Initial State

```sql
SELECT 
    c.id,
    c.case_number,
    c.status as current_status,
    ws.state_code as initial_state_code,
    w.workflow_code
FROM case_entity c
JOIN case_workflow_instance cwi ON c.id = cwi.case_id
JOIN workflow_definition w ON cwi.workflow_id = w.id
JOIN workflow_state ws ON ws.workflow_id = w.id AND ws.is_initial_state = true
WHERE c.is_active = true
  AND c.status != ws.state_code;
```

### Check Migration Results

```sql
-- Summary of cases by state
SELECT 
    c.status,
    COUNT(*) as case_count
FROM case_entity c
WHERE c.is_active = true
GROUP BY c.status
ORDER BY case_count DESC;
```

---

## Best Practices

1. **Backup First**
   ```sql
   -- Create backup table
   CREATE TABLE case_entity_backup AS SELECT * FROM case_entity;
   CREATE TABLE case_workflow_instance_backup AS SELECT * FROM case_workflow_instance;
   ```

2. **Test on Small Batch First**
   - Run migration on a few test cases first
   - Verify results before running on all cases

3. **Run During Low Traffic**
   - Migration can be resource-intensive
   - Run during maintenance window if possible

4. **Monitor Errors**
   - Check error logs after migration
   - Review cases that failed migration

5. **Verify After Migration**
   - Check that cases are in correct initial state
   - Verify workflow instances are created correctly

---

## Troubleshooting

### Problem: "Workflow not found" errors

**Cause:** Case type has `workflow_code` but workflow doesn't exist

**Solution:**
```sql
-- Find cases with invalid workflow codes
SELECT c.id, c.case_number, ct.workflow_code
FROM case_entity c
JOIN case_type ct ON c.case_type_id = ct.id
LEFT JOIN workflow_definition w ON ct.workflow_code = w.workflow_code
WHERE c.is_active = true
  AND ct.workflow_code IS NOT NULL
  AND w.id IS NULL;

-- Fix: Update case type with correct workflow code or create missing workflow
```

### Problem: "Initial state not found" errors

**Cause:** Workflow exists but no state has `isInitialState = true`

**Solution:**
```sql
-- Find workflows without initial state
SELECT w.id, w.workflow_code, w.workflow_name
FROM workflow_definition w
LEFT JOIN workflow_state ws ON ws.workflow_id = w.id AND ws.is_initial_state = true
WHERE w.is_active = true
  AND ws.id IS NULL;

-- Fix: Set isInitialState = true on one state in each workflow
```

### Problem: Cases stuck in wrong state

**Cause:** Migration didn't update case status

**Solution:**
```sql
-- Sync case status with workflow instance state
UPDATE case_entity c
SET status = ws.state_code
FROM case_workflow_instance cwi
JOIN workflow_state ws ON ws.id = cwi.current_state_id
WHERE c.id = cwi.case_id
  AND c.status != ws.state_code;
```

---

## Summary

- **New cases:** Automatically use initial state ✅
- **Old cases:** Need migration script/service ⚠️
- **Two options:** API endpoints (recommended) or SQL scripts
- **Always verify:** Check results after migration
- **Backup first:** Create backups before running migration

**The system manages new cases automatically, but old cases need one-time migration!**
