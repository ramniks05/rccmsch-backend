# Mutation Gift/Sale Workflow - Quick Reference

## Complete Workflow Structure

### Workflow: MUTATION_GIFT_SALE
**Name:** Mutation (after Gift/Sale Deeds)  
**Description:** Workflow for mutation after gift/sale deeds registration

---

## States (13 Total)

| # | Code | Name | Initial | Final | Description |
|---|------|------|---------|-------|-------------|
| 1 | `CITIZEN_APPLICATION` | Citizen Application | ✅ | ❌ | Landowner applies for mutation |
| 2 | `DA_ENTRY` | DA Entry | ❌ | ❌ | Clerk makes entry in register |
| 3 | `MANDOL_RECEIVED` | Mandol Received | ❌ | ❌ | Application passed to Mandol |
| 4 | `NOTICE_GENERATED` | Notice Generated | ❌ | ❌ | Notice sent to parties |
| 5 | `HEARING_SCHEDULED` | Hearing Scheduled | ❌ | ❌ | Date for hearing fixed |
| 6 | `HEARING_COMPLETED` | Hearing Completed | ❌ | ❌ | SDC hears parties |
| 7 | `DECISION_PENDING` | Decision Pending | ❌ | ❌ | SDC decision pending |
| 8 | `APPROVED` | Approved | ❌ | ❌ | SDC approves |
| 9 | `MANDOL_UPDATE` | Mandol Update | ❌ | ❌ | Passed to Mandol |
| 10 | `LAND_RECORD_UPDATED` | Land Record Updated | ❌ | ❌ | Record updated |
| 11 | `PATTA_PREPARATION` | Patta Preparation | ❌ | ❌ | Patta info sent |
| 12 | `COMPLETED` | Completed | ❌ | ✅ | Case completed |
| 13 | `REJECTED` | Rejected | ❌ | ✅ | Case rejected |

---

## Transitions (12 Total)

| # | Code | Name | From State | To State | Comment Required | Responsible Role |
|---|------|------|------------|----------|------------------|------------------|
| 1 | `SUBMIT_APPLICATION` | Submit Application | CITIZEN_APPLICATION | DA_ENTRY | ❌ | CITIZEN |
| 2 | `ENTER_IN_REGISTER` | Enter in Register | DA_ENTRY | MANDOL_RECEIVED | ❌ | DEALING_ASSISTANT |
| 3 | `RECEIVE_BY_MANDOL` | Receive by Mandol | MANDOL_RECEIVED | NOTICE_GENERATED | ❌ | CIRCLE_MANDOL |
| 4 | `GENERATE_NOTICE` | Generate Notice | NOTICE_GENERATED | HEARING_SCHEDULED | ❌ | CIRCLE_MANDOL |
| 5 | `SCHEDULE_HEARING` | Schedule Hearing | HEARING_SCHEDULED | HEARING_COMPLETED | ❌ | CIRCLE_OFFICER |
| 6 | `COMPLETE_HEARING` | Complete Hearing | HEARING_COMPLETED | DECISION_PENDING | ✅ | CIRCLE_OFFICER |
| 7 | `APPROVE` | Approve | DECISION_PENDING | APPROVED | ✅ | CIRCLE_OFFICER |
| 8 | `REJECT` | Reject | DECISION_PENDING | REJECTED | ✅ | CIRCLE_OFFICER |
| 9 | `PASS_TO_MANDOL` | Pass to Mandol | APPROVED | MANDOL_UPDATE | ❌ | CIRCLE_OFFICER |
| 10 | `UPDATE_LAND_RECORD` | Update Land Record | MANDOL_UPDATE | LAND_RECORD_UPDATED | ❌ | CIRCLE_MANDOL |
| 11 | `PREPARE_PATTA` | Prepare Patta | LAND_RECORD_UPDATED | PATTA_PREPARATION | ❌ | DEALING_ASSISTANT |
| 12 | `COMPLETE` | Complete | PATTA_PREPARATION | COMPLETED | ❌ | DEALING_ASSISTANT |

---

## Permissions (12 Total)

| Transition | Role | Unit Level | Can Initiate | Can Approve | Hierarchy Rule |
|------------|------|------------|--------------|-------------|----------------|
| SUBMIT_APPLICATION | `CITIZEN` | null | ✅ | ❌ | `ANY_UNIT` |
| ENTER_IN_REGISTER | `DEALING_ASSISTANT` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| RECEIVE_BY_MANDOL | `CIRCLE_MANDOL` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| GENERATE_NOTICE | `CIRCLE_MANDOL` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| SCHEDULE_HEARING | `CIRCLE_OFFICER` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| COMPLETE_HEARING | `CIRCLE_OFFICER` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| APPROVE | `CIRCLE_OFFICER` | CIRCLE | ✅ | ✅ | `SAME_UNIT` |
| REJECT | `CIRCLE_OFFICER` | CIRCLE | ✅ | ✅ | `SAME_UNIT` |
| PASS_TO_MANDOL | `CIRCLE_OFFICER` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| UPDATE_LAND_RECORD | `CIRCLE_MANDOL` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| PREPARE_PATTA | `DEALING_ASSISTANT` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |
| COMPLETE | `DEALING_ASSISTANT` | CIRCLE | ✅ | ❌ | `SAME_UNIT` |

---

## Workflow Flow Diagram

```
┌─────────────────────┐
│ CITIZEN_APPLICATION │ ← Initial State
│   (Landowner)       │
└──────────┬──────────┘
           │ SUBMIT_APPLICATION (CITIZEN)
           ▼
┌─────────────────────┐
│     DA_ENTRY        │
│   (Clerk)           │
└──────────┬──────────┘
           │ ENTER_IN_REGISTER (DEALING_ASSISTANT)
           ▼
┌─────────────────────┐
│   MANDOL_RECEIVED   │
│  (Circle Mandol)    │
└──────────┬──────────┘
           │ RECEIVE_BY_MANDOL (CIRCLE_MANDOL)
           ▼
┌─────────────────────┐
│  NOTICE_GENERATED   │
│  (Circle Mandol)    │
└──────────┬──────────┘
           │ GENERATE_NOTICE (CIRCLE_MANDOL)
           ▼
┌─────────────────────┐
│  HEARING_SCHEDULED  │
│  (SDC/Circle        │
│   Officer)          │
└──────────┬──────────┘
           │ SCHEDULE_HEARING (CIRCLE_OFFICER)
           ▼
┌─────────────────────┐
│  HEARING_COMPLETED  │
│  (SDC)              │
└──────────┬──────────┘
           │ COMPLETE_HEARING (CIRCLE_OFFICER) [Comment Required]
           ▼
┌─────────────────────┐
│  DECISION_PENDING   │
│  (SDC)              │
└──────┬───────────┬──┘
       │           │
       │ APPROVE   │ REJECT
       │ (SDC)     │ (SDC)
       │ [Comment] │ [Comment]
       ▼           ▼
┌──────────┐   ┌──────────┐
│ APPROVED │   │ REJECTED │ ← Final State
└────┬─────┘   └──────────┘
     │ PASS_TO_MANDOL (CIRCLE_OFFICER)
     ▼
┌─────────────────────┐
│   MANDOL_UPDATE     │
│  (Circle Mandol)    │
└──────────┬──────────┘
           │ UPDATE_LAND_RECORD (CIRCLE_MANDOL)
           ▼
┌─────────────────────┐
│ LAND_RECORD_UPDATED │
│  (Circle Mandol)    │
└──────────┬──────────┘
           │ PREPARE_PATTA (DEALING_ASSISTANT)
           ▼
┌─────────────────────┐
│ PATTA_PREPARATION   │
│  (Clerk)             │
└──────────┬──────────┘
           │ COMPLETE (DEALING_ASSISTANT)
           ▼
┌─────────────────────┐
│     COMPLETED       │ ← Final State
└─────────────────────┘
```

---

## Role Responsibilities

| Role | Responsibilities | Unit Level |
|------|------------------|------------|
| **CITIZEN** | Submit mutation application | Any |
| **DEALING_ASSISTANT** (Clerk) | Enter in register, Prepare patta, Complete case | CIRCLE |
| **CIRCLE_MANDOL** | Receive application, Generate notice, Update land records | CIRCLE |
| **CIRCLE_OFFICER** (SDC) | Schedule hearing, Complete hearing, Approve/Reject, Pass to Mandol | CIRCLE |

---

## API Endpoints Summary

### Workflow
- `POST /api/admin/workflow/definitions` - Create workflow
- `GET /api/admin/workflow/definitions/MUTATION_GIFT_SALE` - Get workflow

### States
- `POST /api/admin/workflow/{workflowId}/states` - Create state
- `GET /api/admin/workflow/{workflowId}/states` - Get all states

### Transitions
- `POST /api/admin/workflow/{workflowId}/transitions` - Create transition
- `GET /api/admin/workflow/{workflowId}/transitions/all` - Get all transitions

### Permissions
- `POST /api/admin/workflow/transitions/{transitionId}/permissions` - Create permission
- `GET /api/admin/workflow/transitions/{transitionId}/permissions` - Get permissions

---

## Quick Setup Commands

### 1. Login
```bash
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin@123"}'
```

### 2. Create Workflow
```bash
curl -X POST http://localhost:8080/api/admin/workflow/definitions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "workflowCode": "MUTATION_GIFT_SALE",
    "workflowName": "Mutation (after Gift/Sale Deeds)",
    "description": "Workflow for mutation after gift/sale deeds registration",
    "isActive": true
  }'
```

### 3. Use Setup Script
```bash
chmod +x docs/setup-mutation-workflow.sh
./docs/setup-mutation-workflow.sh
```

### 4. Import Postman Collection
Import `docs/MUTATION_GIFT_SALE_POSTMAN_COLLECTION.json` into Postman

---

## Important Notes

1. **State IDs**: Use actual IDs from API responses when creating transitions
2. **Transition IDs**: Use actual IDs from API responses when creating permissions
3. **Order Matters**: Create states in order (stateOrder: 1, 2, 3...)
4. **Initial State**: Only one state can be initial (CITIZEN_APPLICATION)
5. **Final States**: Two final states (COMPLETED, REJECTED)
6. **Comments**: APPROVE, REJECT, and COMPLETE_HEARING require comments
7. **Hierarchy**: All permissions use SAME_UNIT except CITIZEN (ANY_UNIT)

---

## Add-on: Return for Correction (Best Approach)

Use a separate state and DA review step. Citizen does not perform workflow transitions.

### New State
- `RETURNED_FOR_CORRECTION` (stateOrder: 14)

### New Transitions
- `RETURN_FROM_DA` (DA_ENTRY → RETURNED_FOR_CORRECTION) [Comment Required]
- `RETURN_FROM_MANDOL` (MANDOL_RECEIVED → RETURNED_FOR_CORRECTION) [Comment Required]
- `RETURN_FROM_SDC` (DECISION_PENDING → RETURNED_FOR_CORRECTION) [Comment Required]
- `REVIEW_CORRECTION` (RETURNED_FOR_CORRECTION → DA_ENTRY)

### Permissions
- RETURN_FROM_DA → DEALING_ASSISTANT (CIRCLE)
- RETURN_FROM_MANDOL → CIRCLE_MANDOL (CIRCLE)
- RETURN_FROM_SDC → CIRCLE_OFFICER (CIRCLE)
- REVIEW_CORRECTION → DEALING_ASSISTANT (CIRCLE)

### Citizen Resubmission API
```
PUT /api/cases/{caseId}/resubmit
Header: X-User-Id: <citizenId>
```

```json
{
  "caseData": "{ ...corrected JSON... }",
  "remarks": "Updated documents and corrected details"
}
```

---

**This workflow matches the actual business process:**
1. Landowner applies → 2. Clerk enters → 3. Mandol receives → 
4. Notice sent → 5. Hearing scheduled → 6. Hearing completed → 
7. SDC decides → 8. If approved: Mandol updates → Patta prepared → Completed
8. If rejected: Case ends
