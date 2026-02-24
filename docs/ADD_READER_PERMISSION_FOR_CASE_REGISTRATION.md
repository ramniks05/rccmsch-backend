# Add READER Permission for Case Registration

## Problem
READER role cannot see the "CASE REGISTRATION" action because they don't have permission for the `SUBMIT_APPLICATION` transition from `CITIZEN_APPLICATION` state to `DA_ENTRY` state.

## Solution
Add READER permission for the `SUBMIT_APPLICATION` transition.

---

## Option 1: Direct SQL (Quick Fix)

Run this SQL query to add READER permission:

```sql
-- First, find the transition ID for SUBMIT_APPLICATION
-- Replace <WORKFLOW_ID> with your actual workflow ID
SELECT t.id, t.transition_code, t.transition_name, w.workflow_code
FROM workflow_transition t
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE t.transition_code = 'SUBMIT_APPLICATION';

-- Then add permission for READER
-- Replace <TRANSITION_ID> with the ID from above query
INSERT INTO workflow_permission (
    transition_id,
    role_code,
    unit_level,
    can_initiate,
    can_approve,
    hierarchy_rule,
    is_active,
    created_at
)
SELECT 
    t.id as transition_id,
    'READER' as role_code,
    NULL as unit_level,  -- NULL means all unit levels
    true as can_initiate,
    false as can_approve,
    'SAME_UNIT' as hierarchy_rule,  -- READER can only register cases in their own unit/court
    true as is_active,
    NOW() as created_at
FROM workflow_transition t
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE t.transition_code = 'SUBMIT_APPLICATION'
  AND NOT EXISTS (
      SELECT 1 FROM workflow_permission wp 
      WHERE wp.transition_id = t.id 
      AND wp.role_code = 'READER'
  );
```

---

## Option 2: Using Admin API

### Step 1: Get Transition ID

```bash
GET /api/admin/workflow/{workflowId}/transitions
Authorization: Bearer <admin_token>
```

Find the transition with `transitionCode: "SUBMIT_APPLICATION"` and note its `id`.

### Step 2: Add Permission

```bash
POST /api/admin/workflow/transitions/{transitionId}/permissions
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "roleCode": "READER",
  "unitLevel": null,
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "isActive": true
}
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/admin/workflow/transitions/1/permissions" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roleCode": "READER",
    "unitLevel": null,
    "canInitiate": true,
    "canApprove": false,
    "hierarchyRule": "SAME_UNIT",
    "isActive": true
  }'
```

---

## Option 3: Update WorkflowDataInitializer (If Re-enabled)

If you re-enable `WorkflowDataInitializer`, update the permission creation logic:

```java
// In createPermissionsForTransition method
if (transitionCode.contains("SUBMIT") || transitionCode.contains("APPLICATION")) {
    // Citizen can submit applications
    createPermissionIfNotExists(transitionId, "CITIZEN", null, true, false, "ANY_UNIT");
    // READER can also register/accept cases
    createPermissionIfNotExists(transitionId, "READER", null, true, false, "SAME_UNIT");
}
```

---

## Verify Permission Added

After adding the permission, verify it exists:

```sql
SELECT 
    wp.id,
    wp.role_code,
    wp.unit_level,
    wp.can_initiate,
    wp.hierarchy_rule,
    t.transition_code,
    t.transition_name
FROM workflow_permission wp
JOIN workflow_transition t ON wp.transition_id = t.id
WHERE t.transition_code = 'SUBMIT_APPLICATION'
  AND wp.role_code = 'READER';
```

---

## Expected Result

After adding this permission:
1. READER will see the "SUBMIT_APPLICATION" transition (which can be labeled as "Case Registration" in the frontend)
2. READER can execute this transition to move cases from `CITIZEN_APPLICATION` to `DA_ENTRY`
3. The case will be registered and move to the next state

---

## Notes

- **Unit Level**: Set to `NULL` so READER can register cases at any unit level (DISTRICT, CIRCLE, etc.)
- **Hierarchy Rule**: Set to `SAME_UNIT` so READER can only register cases in their own court/unit
- **Can Initiate**: `true` - READER can start this transition
- **Can Approve**: `false` - This is not an approval step

---

## Frontend Label

In your frontend, you can map `SUBMIT_APPLICATION` transition to display as "Case Registration" or "Register Case" for READER role:

```typescript
const transitionLabels = {
  'SUBMIT_APPLICATION': {
    'READER': 'Case Registration',
    'CITIZEN': 'Submit Application'
  }
};
```
