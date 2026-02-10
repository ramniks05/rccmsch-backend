# Workflow System Explanation: Forms, Documents, and Transitions

## Overview

Your workflow system manages case progression through **states** using **transitions**. Officers manage **module forms** and **documents** as prerequisites before executing transitions. The system uses a **checklist** mechanism to ensure all required conditions are met.

---

## Key Components

### 1. **Module Forms** (`CaseModuleFormSubmission`)
- **Purpose**: Officers fill out structured forms (HEARING, NOTICE, ORDERSHEET, JUDGEMENT)
- **Storage**: Form data stored as JSON in `case_module_form_submissions` table
- **When Submitted**: Sets workflow flag `{MODULE_TYPE}_SUBMITTED = true` in `workflow_data`

**Example Flow:**
```
Officer fills HEARING form → submitForm() → 
Sets workflow_data: {"HEARING_SUBMITTED": true}
```

### 2. **Documents** (`CaseDocument`)
- **Purpose**: Generated documents (notices, ordersheets, judgements) from templates
- **Statuses**: DRAFT → FINAL/SIGNED (finalize and sign happen together)
- **When Ready**: Sets `{MODULE}_READY = true` when document is finalized/signed
- **When Signed**: Sets `{MODULE}_SIGNED = true` only when officer finalizes (status becomes SIGNED). Not set for DRAFT — drafting never requires a signature.

**Example Flow:**
```
Officer creates NOTICE (DRAFT) → NOTICE_READY = false, NOTICE_SIGNED = false
Officer finalizes NOTICE (FINAL) → document stored as SIGNED, NOTICE_READY = true, NOTICE_SIGNED = true
```

### 3. **Workflow States & Transitions**
- **State**: Current position in workflow (e.g., "REGISTERED", "NOTICE_GENERATED", "HEARING_SCHEDULED")
- **Transition**: Movement from one state to another (e.g., "SUBMIT_NOTICE", "APPROVE_HEARING")
- **Permission**: Defines which roles can execute transitions and what conditions must be met

### 4. **Workflow Data Flags** (`workflow_data` JSON in `CaseWorkflowInstance`)
- **Purpose**: Tracks completion status of forms and documents
- **Format**: `{"HEARING_SUBMITTED": true, "NOTICE_READY": true, ...}`
- **Updated By**: 
  - Forms → `CaseModuleFormService.updateWorkflowFlag()` sets `{MODULE}_SUBMITTED`
  - Documents → `CaseDocumentService.updateWorkflowFlag()` sets `{MODULE}_READY`

---

## How Officers Manage Forms & Documents with Transitions

### Step-by-Step Flow

#### **Scenario: Officer needs to move case from "REGISTERED" to "NOTICE_GENERATED"**

```
┌─────────────────────────────────────────────────────────────┐
│ STEP 1: Officer views available transitions                │
│ GET /api/cases/{caseId}/transitions                         │
│ → Returns: [{"transitionCode": "GENERATE_NOTICE", ...}]    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 2: Officer checks transition checklist                │
│ GET /api/cases/{caseId}/transitions/GENERATE_NOTICE/        │
│      checklist                                              │
│ → Returns checklist showing required conditions:            │
│   - NOTICE_SUBMITTED: ✓ (form submitted)                    │
│   - NOTICE_READY: ✗ (document not ready)                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 3: Officer fills NOTICE form                          │
│ POST /api/cases/{caseId}/forms/NOTICE                       │
│ Body: {formData: {...}, remarks: "..."}                     │
│ → Updates workflow_data: {"NOTICE_SUBMITTED": true}         │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 4: Officer creates NOTICE document                    │
│ POST /api/cases/{caseId}/documents/NOTICE                    │
│ Body: {contentHtml: "...", status: "FINAL"}                 │
│ → Updates workflow_data: {"NOTICE_READY": true}             │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 5: Officer checks checklist again                     │
│ → All conditions met: ✓                                     │
│ → canExecute: true                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 6: Officer executes transition                        │
│ POST /api/cases/{caseId}/transitions/GENERATE_NOTICE        │
│ Body: {comments: "Notice generated and ready"}              │
│ → Case moves from "REGISTERED" → "NOTICE_GENERATED"         │
│ → Case auto-assigned to next officer (based on state)       │
│ → Workflow history recorded                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## How Conditions Work

### Condition Types in `WorkflowPermission.conditions` (JSON)

#### 1. **Workflow Flags** (`workflowDataFieldsRequired`)
```json
{
  "workflowDataFieldsRequired": ["NOTICE_SUBMITTED", "NOTICE_READY"]
}
```
- Checks if flags exist in `workflow_data` JSON
- Forms set `{MODULE}_SUBMITTED` flags
- Documents set `{MODULE}_READY` and `{MODULE}_SIGNED` flags

**Digital signature flags (`NOTICE_SIGNED`, `ORDERSHEET_SIGNED`, `JUDGEMENT_SIGNED`):**
- Set **only when** an officer **finalizes** a document (finalize = sign in one action). Never set for DRAFT — drafting does not require a signature.
- Use these flags **only on finalize transitions** (e.g. "Send notice", "Finalize ordersheet"). Do **not** add them to draft or submit-draft transitions, so that creating/editing drafts never requires a signature.

#### 2. **Module Form Fields** (`moduleFormFieldsRequired`)
```json
{
  "moduleFormFieldsRequired": [
    {"moduleType": "HEARING", "fieldName": "hearingDate"},
    {"moduleType": "NOTICE", "fieldName": "noticeType"}
  ]
}
```
- Checks if specific fields in form submissions have values
- Validates form data completeness

#### 3. **Case Data Fields** (`caseDataFieldsRequired`)
```json
{
  "caseDataFieldsRequired": ["petitionerName", "respondentName"]
}
```
- Checks if case-level fields are filled

#### 4. **Case Type/Priority Filters**
```json
{
  "caseTypeCodesAllowed": ["NEW_FILE", "APPEAL"],
  "casePriorityIn": ["HIGH", "URGENT"]
}
```

---

## Permission Checking Flow

When an officer tries to execute a transition:

```
1. canPerformTransition() checks:
   ├─ Is transition valid from current state? ✓
   ├─ Is transition active? ✓
   ├─ Does officer's role have permission? ✓
   ├─ Does officer's unit level match? ✓
   ├─ Does hierarchy rule pass? (SAME_UNIT/PARENT_UNIT) ✓
   └─ Do all conditions pass? ✓
      ├─ workflowDataFieldsRequired → Check workflow_data flags
      ├─ moduleFormFieldsRequired → Check form submissions
      ├─ caseDataFieldsRequired → Check case data
      └─ caseTypeCodesAllowed → Check case type

2. If ALL checks pass → Transition allowed
3. If ANY check fails → Transition blocked
```

---

## Auto-Assignment After Transition

After a transition executes:

```
1. Case moves to new state (e.g., "NOTICE_GENERATED")
2. System finds roles that can handle transitions FROM new state
3. System finds officer posted to case's court with that role
4. Case auto-assigned to that officer
5. If no officer found → Case remains unassigned but has expected role
```

**Code Location**: `WorkflowEngineService.assignCaseBasedOnWorkflowState()`

---

## Key Code Locations

### Form Submission
- **Service**: `CaseModuleFormService.submitForm()`
- **Flag Update**: Line 217 - Sets `{MODULE}_SUBMITTED` flag
- **Controller**: `CaseModuleFormController`

### Document Creation
- **Service**: `CaseDocumentService.createOrUpdateDocument()`
- **Flag Update**: Line 92 - Sets `{MODULE}_READY` flag when FINAL/SIGNED
- **Controller**: `CaseDocumentController`

### Transition Execution
- **Service**: `WorkflowEngineService.executeTransition()`
- **Permission Check**: `canPerformTransition()` - Line 52
- **Condition Check**: `checkConditions()` - Line 421
- **Checklist**: `getTransitionChecklist()` - Line 738
- **Controller**: `CaseController` (transitions endpoints)

### Workflow Flags
- **Storage**: `CaseWorkflowInstance.workflowData` (JSON column)
- **Update Methods**: 
  - `CaseModuleFormService.updateWorkflowFlag()` - Line 234
  - `CaseDocumentService.updateWorkflowFlag()` - Line 111

---

## Example: Complete Workflow

### State: "REGISTERED" → Transition: "GENERATE_NOTICE"

**Permission Configuration** (`workflow_permission` table):
```sql
transition_id: <GENERATE_NOTICE transition>
role_code: "CIRCLE_MANDOL"
conditions: {
  "workflowDataFieldsRequired": ["NOTICE_SUBMITTED", "NOTICE_READY"]
}
```

**Officer Actions**:
1. ✅ Fill NOTICE form → `NOTICE_SUBMITTED = true`
2. ✅ Create NOTICE document (status: FINAL) → `NOTICE_READY = true`
3. ✅ Check checklist → All conditions met
4. ✅ Execute transition → Case moves to "NOTICE_GENERATED"
5. ✅ Case auto-assigned to next officer (e.g., CIRCLE_OFFICER)

---

## Summary

**Forms** and **Documents** are prerequisites stored as **workflow flags** in `workflow_data`. When officers execute **transitions**, the system checks these flags via **permission conditions**. The **checklist API** shows officers what's missing. After transition, cases are **auto-assigned** to the next responsible officer based on workflow state and role permissions.

The key insight: **Forms/Documents set flags → Flags checked in conditions → Conditions block/allow transitions → Transitions move cases → Cases auto-assign to officers**

---

## Quick Reference

### API Endpoints

| Action | Endpoint | Purpose |
|--------|----------|---------|
| Get available transitions | `GET /api/cases/{caseId}/transitions` | See what transitions officer can perform |
| Get transition checklist | `GET /api/cases/{caseId}/transitions/{transitionCode}/checklist` | See what conditions are missing |
| Submit form | `POST /api/cases/{caseId}/forms/{moduleType}` | Fill and submit module form |
| Create/update document | `POST /api/cases/{caseId}/documents/{moduleType}` | Create or update document |
| Execute transition | `POST /api/cases/{caseId}/transitions/{transitionCode}` | Move case to next state |

### Workflow Flag Naming Convention

- **Form submitted**: `{MODULE_TYPE}_SUBMITTED` (e.g., `HEARING_SUBMITTED`, `NOTICE_SUBMITTED`)
- **Document ready**: `{MODULE_TYPE}_READY` (e.g., `NOTICE_READY`, `ORDERSHEET_READY`)

### Module Types
- `HEARING` - Hearing scheduling form
- `NOTICE` - Notice generation form/document
- `ORDERSHEET` - Ordersheet form/document
- `JUDGEMENT` - Judgement form/document

### Document Statuses
- `DRAFT` - Document being edited
- `FINAL` - Document finalized (sets `_READY` flag)
- `SIGNED` - Document signed (sets `_READY` flag)

### Condition Types in Permission JSON

```json
{
  "workflowDataFieldsRequired": ["FLAG1", "FLAG2"],  // Check workflow flags
  "moduleFormFieldsRequired": [                        // Check form fields
    {"moduleType": "HEARING", "fieldName": "date"}
  ],
  "caseDataFieldsRequired": ["field1"],               // Check case data
  "caseTypeCodesAllowed": ["NEW_FILE"],              // Filter by case type
  "casePriorityIn": ["HIGH"]                         // Filter by priority
}
```
