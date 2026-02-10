# Notice Workflow Guide: DA → Circle Officer → Applicant

This guide explains how to set up the notice workflow where:
1. DA prepares draft notice
2. DA forwards draft notice to Circle Officer
3. Circle Officer can revert to DA or finalize notice
4. When finalized, notice is sent to party
5. Notice appears in applicant login

---

## Workflow States Required

Based on your current workflow, you need to add/modify these states:

### Current State (Where Notice is Prepared)
- **State Code**: `NOTICE_GENERATED` or `DRAFT_NOTICE_PREPARED`
- **State Name**: "Draft Notice Prepared"
- **Description**: "DA has prepared a draft notice"

### New States to Add

1. **DRAFT_NOTICE_WITH_CO**
   - **State Code**: `DRAFT_NOTICE_WITH_CO`
   - **State Name**: "Draft Notice with Circle Officer"
   - **Description**: "Draft notice forwarded to Circle Officer for review"
   - **State Order**: (after NOTICE_GENERATED)

2. **NOTICE_FINALIZED**
   - **State Code**: `NOTICE_FINALIZED`
   - **State Name**: "Notice Finalized"
   - **Description**: "Circle Officer has finalized the notice"
   - **State Order**: (after DRAFT_NOTICE_WITH_CO)

3. **NOTICE_SENT_TO_PARTY**
   - **State Code**: `NOTICE_SENT_TO_PARTY`
   - **State Name**: "Notice Sent to Party"
   - **Description**: "Notice has been sent to the applicant/party"
   - **State Order**: (after NOTICE_FINALIZED)

---

## Workflow Transitions Required

### 1. Forward Draft Notice to Circle Officer
- **Transition Code**: `FORWARD_DRAFT_NOTICE_TO_CO`
- **Transition Name**: "Forward Draft Notice to Circle Officer"
- **From State**: `NOTICE_GENERATED` (or `DRAFT_NOTICE_PREPARED`)
- **To State**: `DRAFT_NOTICE_WITH_CO`
- **Requires Comment**: `true`
- **Description**: "DA forwards draft notice to Circle Officer for review"

**Permission Configuration:**
- **Role**: `DEALING_ASSISTANT`
- **Unit Level**: `CIRCLE`
- **Hierarchy Rule**: `SAME_UNIT`
- **Can Initiate**: `true`
- **Conditions**: 
  ```json
  {
    "workflowDataFieldsRequired": ["NOTICE_SUBMITTED"]
  }
  ```
  (This ensures notice form is submitted before forwarding)

### 2. Revert Notice to DA
- **Transition Code**: `REVERT_NOTICE_TO_DA`
- **Transition Name**: "Revert Notice to DA"
- **From State**: `DRAFT_NOTICE_WITH_CO`
- **To State**: `NOTICE_GENERATED` (or `DRAFT_NOTICE_PREPARED`)
- **Requires Comment**: `true` (important - Circle Officer must provide reason)
- **Description**: "Circle Officer reverts notice back to DA for correction"

**Permission Configuration:**
- **Role**: `CIRCLE_OFFICER`
- **Unit Level**: `CIRCLE`
- **Hierarchy Rule**: `SAME_UNIT`
- **Can Initiate**: `true`
- **Conditions**: None (Circle Officer can always revert)

### 3. Finalize Notice
- **Transition Code**: `FINALIZE_NOTICE`
- **Transition Name**: "Finalize Notice"
- **From State**: `DRAFT_NOTICE_WITH_CO`
- **To State**: `NOTICE_FINALIZED`
- **Requires Comment**: `true`
- **Description**: "Circle Officer finalizes the notice"

**Permission Configuration:**
- **Role**: `CIRCLE_OFFICER`
- **Unit Level**: `CIRCLE`
- **Hierarchy Rule**: `SAME_UNIT`
- **Can Initiate**: `true`
- **Conditions**: 
  ```json
  {
    "workflowDataFieldsRequired": ["NOTICE_READY"]
  }
  ```
  (This ensures notice document status is FINAL or SIGNED)

### 4. Send Notice to Party
- **Transition Code**: `SEND_NOTICE_TO_PARTY`
- **Transition Name**: "Send Notice to Party"
- **From State**: `NOTICE_FINALIZED`
- **To State**: `NOTICE_SENT_TO_PARTY`
- **Requires Comment**: `false`
- **Description**: "Send finalized notice to applicant/party"

**Permission Configuration:**
- **Role**: `CIRCLE_OFFICER` or `DEALING_ASSISTANT`
- **Unit Level**: `CIRCLE`
- **Hierarchy Rule**: `SAME_UNIT`
- **Can Initiate**: `true`
- **Conditions**: None (can send immediately after finalization)

---

## Step-by-Step Implementation

### Step 1: Create/Update Workflow States

Use the Admin API or directly in database:

**API Endpoint**: `POST /api/admin/workflow/states`

**Request Body for each state:**
```json
{
  "workflowId": <your_workflow_id>,
  "stateCode": "DRAFT_NOTICE_WITH_CO",
  "stateName": "Draft Notice with Circle Officer",
  "stateOrder": 5,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Draft notice forwarded to Circle Officer for review"
}
```

Repeat for:
- `NOTICE_FINALIZED`
- `NOTICE_SENT_TO_PARTY`

### Step 2: Create Transitions

**API Endpoint**: `POST /api/admin/workflow/transitions`

**Example for FORWARD_DRAFT_NOTICE_TO_CO:**
```json
{
  "workflowId": <your_workflow_id>,
  "transitionCode": "FORWARD_DRAFT_NOTICE_TO_CO",
  "transitionName": "Forward Draft Notice to Circle Officer",
  "fromStateId": <NOTICE_GENERATED_state_id>,
  "toStateId": <DRAFT_NOTICE_WITH_CO_state_id>,
  "requiresComment": true,
  "description": "DA forwards draft notice to Circle Officer for review",
  "isActive": true
}
```

Repeat for:
- `REVERT_NOTICE_TO_DA`
- `FINALIZE_NOTICE`
- `SEND_NOTICE_TO_PARTY`

### Step 3: Create Permissions

**API Endpoint**: `POST /api/admin/workflow/permissions`

**Example for FORWARD_DRAFT_NOTICE_TO_CO permission:**
```json
{
  "transitionId": <FORWARD_DRAFT_NOTICE_TO_CO_transition_id>,
  "roleCode": "DEALING_ASSISTANT",
  "unitLevel": "CIRCLE",
  "hierarchyRule": "SAME_UNIT",
  "canInitiate": true,
  "isActive": true,
  "conditions": "{\"workflowDataFieldsRequired\": [\"NOTICE_SUBMITTED\"]}"
}
```

**Example for REVERT_NOTICE_TO_DA permission:**
```json
{
  "transitionId": <REVERT_NOTICE_TO_DA_transition_id>,
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "hierarchyRule": "SAME_UNIT",
  "canInitiate": true,
  "isActive": true,
  "conditions": null
}
```

**Example for FINALIZE_NOTICE permission:**
```json
{
  "transitionId": <FINALIZE_NOTICE_transition_id>,
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "hierarchyRule": "SAME_UNIT",
  "canInitiate": true,
  "isActive": true,
  "conditions": "{\"workflowDataFieldsRequired\": [\"NOTICE_READY\"]}"
}
```

**Example for SEND_NOTICE_TO_PARTY permission:**
```json
{
  "transitionId": <SEND_NOTICE_TO_PARTY_transition_id>,
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "hierarchyRule": "SAME_UNIT",
  "canInitiate": true,
  "isActive": true,
  "conditions": null
}
```

---

## Document Status Flow

### Document Status Values
- **DRAFT**: Notice is being prepared/edited
- **FINAL**: Notice is finalized (sets `NOTICE_READY` flag)
- **SIGNED**: Notice is signed (also sets `NOTICE_READY` flag)

### How Document Status Affects Workflow

1. **DA creates notice document** → Status: `DRAFT`
   - No workflow flag set
   - Document not visible to applicant

2. **DA submits notice form** → Sets `NOTICE_SUBMITTED = true`
   - Allows transition: `FORWARD_DRAFT_NOTICE_TO_CO`

3. **Circle Officer updates document status to FINAL** → Sets `NOTICE_READY = true`
   - Allows transition: `FINALIZE_NOTICE`

4. **After SEND_NOTICE_TO_PARTY transition** → Notice becomes visible to applicant

---

## Making Notices Visible to Applicants

### Option 1: Based on Workflow State
Show notice when workflow state is `NOTICE_SENT_TO_PARTY`:

**API Endpoint for Applicants**: `GET /api/citizen/cases/{caseId}/documents`

**Filter Logic:**
```java
// Show documents where:
// 1. moduleType = NOTICE
// 2. case.workflowInstance.currentState.stateCode = "NOTICE_SENT_TO_PARTY"
// 3. status = FINAL or SIGNED
```

### Option 2: Add Visibility Flag to Document
Add a field `visibleToApplicant` to `CaseDocument` entity:

```java
@Column(name = "visible_to_applicant", nullable = false)
private Boolean visibleToApplicant = false;
```

Set to `true` when executing `SEND_NOTICE_TO_PARTY` transition.

### Option 3: Use Document Status + Workflow State
Show notice when:
- Document status is `FINAL` or `SIGNED`
- Workflow state is `NOTICE_SENT_TO_PARTY` or later

---

## Complete Workflow Flow Diagram

```
┌─────────────────────┐
│ NOTICE_GENERATED    │ ← DA prepares draft notice
│ (Draft Prepared)    │   - Creates notice document (DRAFT)
└──────────┬──────────┘   - Submits notice form
           │              - Sets NOTICE_SUBMITTED = true
           │
           │ FORWARD_DRAFT_NOTICE_TO_CO
           │ (DA → Circle Officer)
           ↓
┌─────────────────────┐
│ DRAFT_NOTICE_WITH_CO│ ← Circle Officer reviews
│ (With Circle Officer)│
└──────────┬──────────┘
           │
    ┌──────┴──────┐
    │             │
    │             │
    ↓             ↓
┌──────────┐  ┌──────────────┐
│ REVERT  │  │ FINALIZE     │
│ TO DA   │  │ NOTICE       │
└────┬────┘  └──────┬───────┘
     │              │
     │              │ Sets NOTICE_READY = true
     │              │ (when doc status = FINAL)
     │              │
     │              ↓
     │      ┌──────────────┐
     │      │ NOTICE_      │
     │      │ FINALIZED    │
     │      └──────┬───────┘
     │             │
     │             │ SEND_NOTICE_TO_PARTY
     │             │
     │             ↓
     │      ┌──────────────┐
     │      │ NOTICE_SENT_ │
     │      │ TO_PARTY     │ ← Notice visible to applicant
     │      └──────────────┘
     │
     └──────────────┘
     (Back to NOTICE_GENERATED)
```

---

## API Endpoints for Officers

### 1. View Available Transitions
```
GET /api/cases/{caseId}/transitions
```
Returns transitions based on current state and permissions.

### 2. Check Transition Checklist
```
GET /api/cases/{caseId}/transitions/{transitionCode}/checklist
```
Shows what conditions must be met (e.g., NOTICE_SUBMITTED, NOTICE_READY).

### 3. Execute Transition
```
POST /api/cases/{caseId}/transitions/{transitionCode}
Body: {
  "comments": "Forwarding draft notice for review"
}
```

### 4. Create/Update Notice Document
```
POST /api/cases/{caseId}/documents/NOTICE
Body: {
  "templateId": <template_id>,
  "contentHtml": "...",
  "contentData": "{...}",
  "status": "DRAFT"  // or "FINAL" when ready
}
```

### 5. Update Document Status
```
PUT /api/cases/{caseId}/documents/{documentId}
Body: {
  "status": "FINAL"  // Changes DRAFT → FINAL, sets NOTICE_READY = true
}
```

---

## API Endpoints for Applicants

### 1. View Case Documents
```
GET /api/citizen/cases/{caseId}/documents
```
Should filter to show only documents where:
- `moduleType = NOTICE`
- Workflow state is `NOTICE_SENT_TO_PARTY` or later
- Status is `FINAL` or `SIGNED`

### 2. View Specific Notice
```
GET /api/citizen/cases/{caseId}/documents/NOTICE/latest
```
Returns the latest notice document (if visible).

---

## Testing Checklist

- [ ] State `DRAFT_NOTICE_WITH_CO` created
- [ ] State `NOTICE_FINALIZED` created
- [ ] State `NOTICE_SENT_TO_PARTY` created
- [ ] Transition `FORWARD_DRAFT_NOTICE_TO_CO` created
- [ ] Transition `REVERT_NOTICE_TO_DA` created
- [ ] Transition `FINALIZE_NOTICE` created
- [ ] Transition `SEND_NOTICE_TO_PARTY` created
- [ ] Permissions configured for all transitions
- [ ] DA can forward draft notice (when NOTICE_SUBMITTED = true)
- [ ] Circle Officer can revert notice
- [ ] Circle Officer can finalize notice (when NOTICE_READY = true)
- [ ] Notice becomes visible to applicant after SEND_NOTICE_TO_PARTY
- [ ] Document status updates correctly (DRAFT → FINAL)
- [ ] Workflow flags set correctly (NOTICE_SUBMITTED, NOTICE_READY)

---

## Next Steps

After implementing this workflow:

1. **Test the flow** with sample cases
2. **Verify permissions** are correctly configured
3. **Check applicant visibility** - ensure notices appear in applicant dashboard
4. **Add any additional conditions** if needed (e.g., require specific fields in notice form)
5. **Configure auto-assignment** if cases should auto-assign to Circle Officer after forwarding

---

## Notes

- **Document Status vs Workflow State**: Document status (DRAFT/FINAL/SIGNED) is independent of workflow state. A document can be FINAL while workflow is still in DRAFT_NOTICE_WITH_CO.
- **Workflow Flags**: `NOTICE_SUBMITTED` is set when form is submitted, `NOTICE_READY` is set when document status becomes FINAL or SIGNED.
- **Comments**: Transitions like REVERT and FORWARD should require comments for audit trail.
- **Auto-assignment**: After `FORWARD_DRAFT_NOTICE_TO_CO`, case should auto-assign to Circle Officer at the same unit.
