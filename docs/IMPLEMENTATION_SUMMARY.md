# Workflow Condition System - Implementation Summary

## What Has Been Created

### 1. Documentation
- **`docs/WORKFLOW_CONDITION_CONFIGURATION.md`** - Comprehensive frontend documentation for UI implementation

### 2. Backend DTOs
- **`TransitionChecklistDTO.java`** - Response DTO for checklist status
- **`ConditionStatusDTO.java`** - Individual condition status in checklist

### 3. Backend Service Enhancement
- **`WorkflowEngineService.java`** - Added `getTransitionChecklist()` method
  - Evaluates all conditions for a transition
  - Returns detailed status of each condition
  - Shows which conditions are blocking

### 4. Backend Controller
- **`CaseController.java`** - Added endpoint:
  - `GET /api/cases/{caseId}/transitions/{transitionCode}/checklist`

## How It Works

### Form Submission Sets Flags
When forms are submitted, the backend automatically sets workflow flags:
- **Hearing form submitted** → Sets `HEARING_SUBMITTED = true` in workflow data
- **Notice document finalized** → Sets `NOTICE_READY = true` in workflow data
- **Ordersheet finalized** → Sets `ORDERSHEET_READY = true` in workflow data

### Condition Configuration (Admin)
Admins configure conditions in `WorkflowPermission.conditions` JSON field:

```json
{
  "workflowDataFieldsRequired": ["HEARING_SUBMITTED", "NOTICE_READY"],
  "moduleFormFieldsRequired": [
    {
      "moduleType": "HEARING",
      "fieldName": "hearingDate"
    }
  ],
  "caseDataFieldsRequired": ["applicantName"]
}
```

### Condition Checking (Runtime)
When a user tries to execute a transition:
1. System checks all configured conditions
2. Returns checklist showing which are met/blocking
3. Blocks transition if any required condition fails

## API Endpoints

### Get Checklist Status
```
GET /api/cases/{caseId}/transitions/{transitionCode}/checklist
```

**Response:**
```json
{
  "success": true,
  "data": {
    "transitionCode": "SCHEDULE_HEARING",
    "transitionName": "Schedule Hearing",
    "canExecute": false,
    "conditions": [
      {
        "label": "Hearing form submitted",
        "type": "WORKFLOW_FLAG",
        "flagName": "HEARING_SUBMITTED",
        "required": true,
        "passed": false,
        "message": "Hearing form submitted must be completed"
      }
    ],
    "blockingReasons": [
      "Hearing form submitted must be completed"
    ]
  }
}
```

## Next Steps for Frontend Team

1. **Read the Documentation**: See `docs/WORKFLOW_CONDITION_CONFIGURATION.md`

2. **Implement Checklist UI**:
   - Show checklist when user hovers/clicks on transition button
   - Display ✓ for passed conditions, ✗ for failed
   - Show blocking reasons when transition is disabled

3. **Implement Admin Configuration UI**:
   - Create condition editor modal
   - Allow selecting workflow flags, form fields, etc.
   - Save conditions as JSON in permission

4. **Test the API**:
   - Call `/api/cases/{caseId}/transitions/{transitionCode}/checklist`
   - Verify response structure matches documentation

## Example Use Cases

### Example 1: Schedule Hearing Transition
**Requires:**
- Hearing form submitted (`HEARING_SUBMITTED` flag)
- Hearing date field filled (`hearingDate` in hearing form)

**Configuration:**
```json
{
  "workflowDataFieldsRequired": ["HEARING_SUBMITTED"],
  "moduleFormFieldsRequired": [
    {"moduleType": "HEARING", "fieldName": "hearingDate"}
  ]
}
```

### Example 2: Send Notice Transition
**Requires:**
- Notice document finalized (`NOTICE_READY` flag)

**Configuration:**
```json
{
  "workflowDataFieldsRequired": ["NOTICE_READY"]
}
```

## Notes

- All condition checking happens automatically - no code changes needed for new conditions
- Conditions are stored as JSON in `WorkflowPermission.conditions` field
- Frontend just needs to provide UI for configuration and display
- Backend handles all validation and blocking logic

---

**Status**: ✅ Backend implementation complete
**Next**: Frontend UI implementation (see documentation)
