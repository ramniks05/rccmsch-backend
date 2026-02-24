# Debug: READER Actions Not Showing

## Problem
READER logs in but no actions are showing for a case in "CASE_INITIATION" state, even though permissions are created.

## Case Information
- **State**: `CASE_INITIATION`
- **Workflow**: `PARTITION_CASE`
- **Assigned To Unit**: `2` (Chandigarh)
- **Assigned To Role**: `CITIZEN`

---

## Step 1: Check What Transition Exists from CASE_INITIATION

Run this SQL to see what transitions exist from the current state:

```sql
-- Find transitions from CASE_INITIATION state
SELECT 
    t.id as transition_id,
    t.transition_code,
    t.transition_name,
    t.is_active,
    fs.state_code as from_state,
    ts.state_code as to_state,
    w.workflow_code
FROM workflow_transition t
JOIN workflow_state fs ON t.from_state_id = fs.id
JOIN workflow_state ts ON t.to_state_id = ts.id
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE fs.state_code = 'CASE_INITIATION'
  AND w.workflow_code = 'PARTITION_CASE'
  AND t.is_active = true;
```

**Expected:** Should show transition(s) from `CASE_INITIATION` state.

---

## Step 2: Check READER Permissions for That Transition

Run this SQL to check if READER has permission:

```sql
-- Check READER permissions for transitions from CASE_INITIATION
SELECT 
    wp.id,
    wp.role_code,
    wp.unit_level,
    wp.can_initiate,
    wp.can_approve,
    wp.hierarchy_rule,
    wp.is_active,
    t.transition_code,
    t.transition_name
FROM workflow_permission wp
JOIN workflow_transition t ON wp.transition_id = t.id
JOIN workflow_state fs ON t.from_state_id = fs.id
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE fs.state_code = 'CASE_INITIATION'
  AND w.workflow_code = 'PARTITION_CASE'
  AND wp.role_code = 'READER'
  AND wp.is_active = true;
```

**Expected:** Should show READER permission with:
- `can_initiate: true`
- `unit_level: DISTRICT` (or NULL)
- `hierarchy_rule: SAME_UNIT` (or appropriate rule)

---

## Step 3: Check READER's Unit ID

Check what unitId the READER is logged in with:

```sql
-- Find READER's posting and unit
SELECT 
    p.id as posting_id,
    p.posting_userid,
    p.role_code,
    p.court_id,
    p.unit_id,
    p.is_current,
    c.court_code,
    c.court_name,
    u.unit_id as unit_unit_id,
    u.unit_name,
    u.unit_level
FROM officer_da_history p
LEFT JOIN court c ON p.court_id = c.id
LEFT JOIN admin_unit u ON (p.unit_id = u.unit_id OR c.unit_id = u.unit_id)
WHERE p.role_code = 'READER'
  AND p.is_current = true;
```

**Check:** READER's `unit_id` should match case's `assignedToUnitId` (2) for SAME_UNIT rule to pass.

---

## Step 4: Check Case Workflow Instance

```sql
-- Check case workflow instance details
SELECT 
    cwi.id,
    cwi.case_id,
    cwi.workflow_id,
    cwi.current_state_id,
    cwi.assigned_to_unit_id,
    cwi.assigned_to_officer_id,
    cwi.assigned_to_role,
    ws.state_code,
    ws.state_name,
    w.workflow_code
FROM case_workflow_instance cwi
JOIN workflow_state ws ON cwi.current_state_id = ws.id
JOIN workflow_definition w ON cwi.workflow_id = w.id
WHERE cwi.case_id = 3;  -- Replace with your case ID
```

**Check:** 
- `assigned_to_unit_id` should be `2` (Chandigarh)
- `current_state_id` should match CASE_INITIATION state

---

## Common Issues and Fixes

### Issue 1: No Transition from CASE_INITIATION

**Problem:** No transition exists from `CASE_INITIATION` state

**Fix:** Create transition via Admin API:
```bash
POST /api/admin/workflow/{workflowId}/transitions
{
  "transitionCode": "REGISTER_CASE",
  "transitionName": "Register Case",
  "fromStateId": <CASE_INITIATION_STATE_ID>,
  "toStateId": <NEXT_STATE_ID>,
  "isActive": true
}
```

### Issue 2: READER Permission Missing

**Problem:** Transition exists but READER doesn't have permission

**Fix:** Add READER permission via Admin API:
```bash
POST /api/admin/workflow/transitions/{transitionId}/permissions
{
  "roleCode": "READER",
  "unitLevel": "DISTRICT",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "isActive": true
}
```

### Issue 3: Hierarchy Rule Failing

**Problem:** READER's unitId doesn't match case's assignedToUnitId

**Check:**
- READER's posting unitId: `SELECT unit_id FROM officer_da_history WHERE role_code = 'READER' AND is_current = true`
- Case's assignedToUnitId: `SELECT assigned_to_unit_id FROM case_workflow_instance WHERE case_id = 3`

**Fix Options:**
1. **Change hierarchy rule to ANY_UNIT** (if READER should see all cases):
   ```sql
   UPDATE workflow_permission 
   SET hierarchy_rule = 'ANY_UNIT'
   WHERE role_code = 'READER' 
     AND transition_id IN (
       SELECT id FROM workflow_transition 
       WHERE from_state_id = (SELECT id FROM workflow_state WHERE state_code = 'CASE_INITIATION')
     );
   ```

2. **Or ensure READER is posted to same unit as case** (unitId = 2)

### Issue 4: Unit Level Mismatch

**Problem:** Permission has `unitLevel: DISTRICT` but READER's unit is different level

**Check:**
```sql
-- Check READER's unit level
SELECT u.unit_level, u.unit_name
FROM officer_da_history p
JOIN admin_unit u ON p.unit_id = u.unit_id
WHERE p.role_code = 'READER' AND p.is_current = true;
```

**Fix:** Update permission to match or set `unitLevel: NULL`:
```sql
UPDATE workflow_permission 
SET unit_level = NULL  -- NULL means all levels
WHERE role_code = 'READER' 
  AND transition_id IN (
    SELECT id FROM workflow_transition 
    WHERE from_state_id = (SELECT id FROM workflow_state WHERE state_code = 'CASE_INITIATION')
  );
```

---

## Quick Diagnostic Query

Run this to see everything at once:

```sql
-- Complete diagnostic query
SELECT 
    'Transition' as type,
    t.transition_code,
    t.transition_name,
    t.is_active as transition_active,
    NULL as permission_info,
    NULL as hierarchy_info
FROM workflow_transition t
JOIN workflow_state fs ON t.from_state_id = fs.id
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE fs.state_code = 'CASE_INITIATION'
  AND w.workflow_code = 'PARTITION_CASE'

UNION ALL

SELECT 
    'Permission' as type,
    t.transition_code,
    NULL,
    NULL,
    CONCAT('Role: ', wp.role_code, ', UnitLevel: ', COALESCE(wp.unit_level::text, 'NULL'), ', CanInitiate: ', wp.can_initiate) as permission_info,
    CONCAT('Hierarchy: ', COALESCE(wp.hierarchy_rule, 'NULL')) as hierarchy_info
FROM workflow_permission wp
JOIN workflow_transition t ON wp.transition_id = t.id
JOIN workflow_state fs ON t.from_state_id = fs.id
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE fs.state_code = 'CASE_INITIATION'
  AND w.workflow_code = 'PARTITION_CASE'
  AND wp.role_code = 'READER'
ORDER BY type, transition_code;
```

---

## Enable Debug Logging

Check application logs with DEBUG level enabled. You should see:
```
DEBUG: Checking transition: REGISTER_CASE for role: READER, unitLevel: DISTRICT
DEBUG: Found 1 permission(s) for transition: REGISTER_CASE, role: READER
DEBUG: Checking permission - canInitiate: true, hierarchyRule: SAME_UNIT, unitLevel: DISTRICT
DEBUG: SAME_UNIT check: officer unitId=2, case assignedToUnitId=2, result=true
```

---

## Summary Checklist

- [ ] Transition exists from `CASE_INITIATION` state
- [ ] Transition is active (`is_active = true`)
- [ ] READER has permission for that transition
- [ ] Permission has `can_initiate = true`
- [ ] Permission is active (`is_active = true`)
- [ ] READER's unitId matches case's `assignedToUnitId` (for SAME_UNIT rule)
- [ ] Unit level matches (or permission has `unit_level = NULL`)
