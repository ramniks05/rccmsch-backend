# Workflow Initial State Configuration Guide

## Overview

When a case is submitted, it automatically goes to the **initial state** of the workflow. This is configured through the workflow state's `isInitialState` flag.

## How It Works

### Current Flow

1. **Citizen submits case** → Case is created
2. **System finds workflow** → Based on `CaseType.workflowCode`
3. **System finds initial state** → State with `isInitialState = true`
4. **Case is set to initial state** → Case status = initial state code
5. **Workflow instance created** → Case starts in initial state

### Example

For **Mutation Gift/Sale** workflow:
- Initial State: `CITIZEN_APPLICATION` (isInitialState = true)
- When citizen submits → Case goes to `CITIZEN_APPLICATION`
- READER can then accept → Case moves to `DA_ENTRY`

---

## Configuring Initial State

### Method 1: Through Admin API (Recommended)

#### Step 1: Create or Update State with `isInitialState = true`

```bash
POST /api/admin/workflow/{workflowId}/states
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "stateCode": "CITIZEN_APPLICATION",
  "stateName": "Citizen Application",
  "stateOrder": 1,
  "isInitialState": true,  // ← This makes it the initial state
  "isFinalState": false,
  "description": "Landowner applies for mutation"
}
```

**Important:** When you set `isInitialState = true` on a state, the system automatically sets `isInitialState = false` on all other states in the same workflow.

#### Step 2: Verify Initial State

```bash
GET /api/admin/workflow/{workflowId}/states
```

Look for the state with `isInitialState: true`.

---

### Method 2: Direct SQL (If Needed)

```sql
-- Set a state as initial (automatically un-sets others)
UPDATE workflow_state 
SET is_initial_state = false 
WHERE workflow_id = <workflow_id>;

UPDATE workflow_state 
SET is_initial_state = true 
WHERE id = <state_id> AND workflow_id = <workflow_id>;
```

---

## Rules and Validation

### ✅ Rules

1. **Only ONE initial state per workflow**
   - If you set `isInitialState = true` on a state, all other states in the workflow are automatically set to `false`

2. **Initial state must exist**
   - If no state has `isInitialState = true`, case creation will fail with error: "Initial state not found for workflow"

3. **Initial state can be any state**
   - It doesn't have to be the first state in order
   - Common examples: `CITIZEN_APPLICATION`, `REVENUE_APPROVAL`, `GOVERNMENT_APPROVAL`

4. **Initial state cannot be final**
   - A state cannot be both initial and final (`isInitialState = true` AND `isFinalState = true`)

---

## Common Initial States by Workflow Type

### Citizen-Initiated Workflows
- **Initial State:** `CITIZEN_APPLICATION`
- **Example:** Mutation, Partition, etc.
- **Flow:** Citizen submits → Case in `CITIZEN_APPLICATION` → READER accepts → Next state

### Government-Initiated Workflows
- **Initial State:** `GOVERNMENT_APPROVAL` or `REVENUE_APPROVAL`
- **Example:** Allotment, Acquisition
- **Flow:** Government initiates → Case in approval state → Next steps

### Court-Initiated Workflows
- **Initial State:** `COURT_ORDER_RECEIVED`
- **Example:** Higher Court Order implementation
- **Flow:** Court order received → Case starts → Next steps

---

## Code Implementation

### Where Initial State is Used

**File:** `CaseService.java`

```java
// Method: initializeWorkflowInstance()
WorkflowState initialState = workflowStateRepository
    .findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
    .orElseThrow(() -> new RuntimeException("Initial state not found"));

instance.setCurrentState(initialState);
caseEntity.setStatus(initialState.getStateCode());
```

---

## Changing Initial State

### Scenario: Change from `CITIZEN_APPLICATION` to `DA_ENTRY`

**Step 1:** Update the new initial state
```bash
PUT /api/admin/workflow/states/{daEntryStateId}
{
  "isInitialState": true
}
```

**Step 2:** Verify old state is no longer initial
```bash
GET /api/admin/workflow/states/{citizenApplicationStateId}
# Should show: "isInitialState": false
```

**Step 3:** Test case creation
- New cases should start in `DA_ENTRY` state
- Old cases remain in their current states

---

## Troubleshooting

### Problem: "Initial state not found for workflow"

**Cause:** No state has `isInitialState = true`

**Solution:**
1. Check workflow states: `GET /api/admin/workflow/{workflowId}/states`
2. Set one state as initial: `PUT /api/admin/workflow/states/{stateId}` with `isInitialState: true`

### Problem: Multiple initial states

**Cause:** Database inconsistency (shouldn't happen with API)

**Solution:**
```sql
-- Find all initial states for a workflow
SELECT id, state_code, state_name 
FROM workflow_state 
WHERE workflow_id = <workflow_id> 
AND is_initial_state = true;

-- Set only one as initial
UPDATE workflow_state 
SET is_initial_state = false 
WHERE workflow_id = <workflow_id>;

UPDATE workflow_state 
SET is_initial_state = true 
WHERE id = <desired_state_id>;
```

### Problem: Cases not starting in expected state

**Check:**
1. Verify `CaseType.workflowCode` is set correctly
2. Verify workflow has an initial state
3. Check application logs for errors during case creation

---

## Best Practices

1. **Set initial state when creating workflow**
   - Create workflow → Create states → Set one as initial → Create transitions

2. **Use descriptive state codes**
   - `CITIZEN_APPLICATION` (clear)
   - Not `STATE1` (unclear)

3. **Document initial state in workflow description**
   - "Workflow starts when citizen submits application (CITIZEN_APPLICATION state)"

4. **Test after changing initial state**
   - Create a test case
   - Verify it starts in the correct state

---

## Summary

- **Initial state** = State with `isInitialState = true`
- **Configured through:** Admin API or SQL
- **Automatic:** System finds initial state when case is created
- **One per workflow:** Only one state can be initial
- **Flexible:** Can be any state in the workflow

**The initial state determines where cases start after submission!**
