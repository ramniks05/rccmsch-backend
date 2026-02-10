# Complete Workflow Configuration Example: Mutation (after Gift/Sale Deeds)

## Overview

This document provides a complete step-by-step guide to configure the **Mutation (after Gift/Sale Deeds)** workflow using the admin APIs. This includes all states, transitions, and permissions.

## Workflow Flow

```
1. CITIZEN_APPLICATION (Initial)
   ↓ SUBMIT_APPLICATION
2. DA_ENTRY
   ↓ ENTER_IN_REGISTER
3. MANDOL_RECEIVED
   ↓ RECEIVE_BY_MANDOL
4. NOTICE_GENERATED
   ↓ GENERATE_NOTICE
5. HEARING_SCHEDULED
   ↓ SCHEDULE_HEARING
6. HEARING_COMPLETED
   ↓ COMPLETE_HEARING
7. DECISION_PENDING
   ↓ APPROVE or REJECT
8. APPROVED (if approved)
   ↓ PASS_TO_MANDOL
9. MANDOL_UPDATE
   ↓ UPDATE_LAND_RECORD
10. LAND_RECORD_UPDATED
   ↓ PREPARE_PATTA
11. PATTA_PREPARATION
   ↓ COMPLETE
12. COMPLETED (Final)

OR

8. REJECTED (if rejected) - Final State
```

---

## Step-by-Step Configuration

### Prerequisites

1. **Login as Admin** and get token:
```bash
POST http://localhost:8080/api/admin/auth/login
{
  "username": "admin",
  "password": "admin@123"
}
```

2. **Save the token** for all subsequent requests:
```
Authorization: Bearer <your_admin_token>
```

---

## Step 1: Create Workflow Definition

**Request:**
```bash
POST http://localhost:8080/api/admin/workflow/definitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "workflowCode": "MUTATION_GIFT_SALE",
  "workflowName": "Mutation (after Gift/Sale Deeds)",
  "description": "Workflow for mutation after gift/sale deeds registration. Landowner applies → Clerk enters → Mandol processes → SDC hears and decides → Mandol updates records → Patta prepared",
  "isActive": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Workflow created successfully",
  "data": {
    "id": 1,
    "workflowCode": "MUTATION_GIFT_SALE",
    "workflowName": "Mutation (after Gift/Sale Deeds)",
    "description": "Workflow for mutation after gift/sale deeds registration...",
    "isActive": true,
    "version": 1
  }
}
```

**Save `workflowId` = 1** (or the ID from response)

---

## Step 2: Create All States

**Workflow ID:** `1` (replace with your actual workflow ID)

### State 1: CITIZEN_APPLICATION (Initial State)
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "CITIZEN_APPLICATION",
  "stateName": "Citizen Application",
  "stateOrder": 1,
  "isInitialState": true,
  "isFinalState": false,
  "description": "Landowner applies for mutation in Form no. 16 with a copy of the Registered Deed"
}
```

**Response:** Save `stateId` = 1

### State 2: DA_ENTRY
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "DA_ENTRY",
  "stateName": "DA Entry",
  "stateOrder": 2,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Clerk makes an entry in the Mutation Register"
}
```

**Response:** Save `stateId` = 2

### State 3: MANDOL_RECEIVED
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "MANDOL_RECEIVED",
  "stateName": "Mandol Received",
  "stateOrder": 3,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Application passed to Circle Mandol"
}
```

**Response:** Save `stateId` = 3

### State 4: NOTICE_GENERATED
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "NOTICE_GENERATED",
  "stateName": "Notice Generated",
  "stateOrder": 4,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Notice is sent to the parties concerned"
}
```

**Response:** Save `stateId` = 4

### State 5: HEARING_SCHEDULED
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "HEARING_SCHEDULED",
  "stateName": "Hearing Scheduled",
  "stateOrder": 5,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Date for hearing is fixed"
}
```

**Response:** Save `stateId` = 5

### State 6: HEARING_COMPLETED
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "HEARING_COMPLETED",
  "stateName": "Hearing Completed",
  "stateOrder": 6,
  "isInitialState": false,
  "isFinalState": false,
  "description": "SDC hears the parties and studies the documents"
}
```

**Response:** Save `stateId` = 6

### State 7: DECISION_PENDING
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "DECISION_PENDING",
  "stateName": "Decision Pending",
  "stateOrder": 7,
  "isInitialState": false,
  "isFinalState": false,
  "description": "SDC decision pending - may approve or reject"
}
```

**Response:** Save `stateId` = 7

### State 8: APPROVED
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "APPROVED",
  "stateName": "Approved",
  "stateOrder": 8,
  "isInitialState": false,
  "isFinalState": false,
  "description": "SDC approves and passes a Mutation Order"
}
```

**Response:** Save `stateId` = 8

### State 9: MANDOL_UPDATE
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "MANDOL_UPDATE",
  "stateName": "Mandol Update",
  "stateOrder": 9,
  "isInitialState": false,
  "isFinalState": false,
  "description": "SDC hands over the order to the Circle Mandol to update the land record"
}
```

**Response:** Save `stateId` = 9

### State 10: LAND_RECORD_UPDATED
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "LAND_RECORD_UPDATED",
  "stateName": "Land Record Updated",
  "stateOrder": 10,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Circle Mandol updates the land record by specifying the new owner(s)"
}
```

**Response:** Save `stateId` = 10

### State 11: PATTA_PREPARATION
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "PATTA_PREPARATION",
  "stateName": "Patta Preparation",
  "stateOrder": 11,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Landowner gets informed regarding preparation of new patta"
}
```

**Response:** Save `stateId` = 11

### State 12: COMPLETED (Final State)
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "COMPLETED",
  "stateName": "Completed",
  "stateOrder": 12,
  "isInitialState": false,
  "isFinalState": true,
  "description": "Case completed successfully"
}
```

**Response:** Save `stateId` = 12

### State 13: REJECTED (Final State)
```bash
POST http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
Content-Type: application/json

{
  "stateCode": "REJECTED",
  "stateName": "Rejected",
  "stateOrder": 13,
  "isInitialState": false,
  "isFinalState": true,
  "description": "SDC rejects the application"
}
```

**Response:** Save `stateId` = 13

---

## Step 3: Create All Transitions

**Note:** Replace state IDs with actual IDs from Step 2 responses.

### Transition 1: SUBMIT_APPLICATION
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "SUBMIT_APPLICATION",
  "transitionName": "Submit Application",
  "fromStateId": 1,
  "toStateId": 2,
  "requiresComment": false,
  "isActive": true,
  "description": "Landowner submits mutation application with Form no. 16 and Registered Deed"
}
```

**Response:** Save `transitionId` = 1

### Transition 2: ENTER_IN_REGISTER
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "ENTER_IN_REGISTER",
  "transitionName": "Enter in Register",
  "fromStateId": 2,
  "toStateId": 3,
  "requiresComment": false,
  "isActive": true,
  "description": "Clerk makes an entry in the Mutation Register and passes to Circle Mandol"
}
```

**Response:** Save `transitionId` = 2

### Transition 3: RECEIVE_BY_MANDOL
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "RECEIVE_BY_MANDOL",
  "transitionName": "Receive by Mandol",
  "fromStateId": 3,
  "toStateId": 4,
  "requiresComment": false,
  "isActive": true,
  "description": "Circle Mandol receives the application"
}
```

**Response:** Save `transitionId` = 3

### Transition 4: GENERATE_NOTICE
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "GENERATE_NOTICE",
  "transitionName": "Generate Notice",
  "fromStateId": 4,
  "toStateId": 5,
  "requiresComment": false,
  "isActive": true,
  "description": "Notice is sent to the parties concerned"
}
```

**Response:** Save `transitionId` = 4

### Transition 5: SCHEDULE_HEARING
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "SCHEDULE_HEARING",
  "transitionName": "Schedule Hearing",
  "fromStateId": 5,
  "toStateId": 6,
  "requiresComment": false,
  "isActive": true,
  "description": "Date for hearing is fixed"
}
```

**Response:** Save `transitionId` = 5

### Transition 6: COMPLETE_HEARING
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "COMPLETE_HEARING",
  "transitionName": "Complete Hearing",
  "fromStateId": 6,
  "toStateId": 7,
  "requiresComment": true,
  "isActive": true,
  "description": "SDC hears the parties and studies the documents"
}
```

**Response:** Save `transitionId` = 6

### Transition 7: APPROVE
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "APPROVE",
  "transitionName": "Approve",
  "fromStateId": 7,
  "toStateId": 8,
  "requiresComment": true,
  "isActive": true,
  "description": "SDC approves the application and passes a Mutation Order"
}
```

**Response:** Save `transitionId` = 7

### Transition 8: REJECT
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "REJECT",
  "transitionName": "Reject",
  "fromStateId": 7,
  "toStateId": 13,
  "requiresComment": true,
  "isActive": true,
  "description": "SDC rejects the application"
}
```

**Response:** Save `transitionId` = 8

### Transition 9: PASS_TO_MANDOL
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "PASS_TO_MANDOL",
  "transitionName": "Pass to Mandol",
  "fromStateId": 8,
  "toStateId": 9,
  "requiresComment": false,
  "isActive": true,
  "description": "SDC hands over the order to the Circle Mandol"
}
```

**Response:** Save `transitionId` = 9

### Transition 10: UPDATE_LAND_RECORD
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "UPDATE_LAND_RECORD",
  "transitionName": "Update Land Record",
  "fromStateId": 9,
  "toStateId": 10,
  "requiresComment": false,
  "isActive": true,
  "description": "Circle Mandol updates the land record by specifying the new owner(s)"
}
```

**Response:** Save `transitionId` = 10

### Transition 11: PREPARE_PATTA
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "PREPARE_PATTA",
  "transitionName": "Prepare Patta",
  "fromStateId": 10,
  "toStateId": 11,
  "requiresComment": false,
  "isActive": true,
  "description": "Inform landowner regarding preparation of new patta"
}
```

**Response:** Save `transitionId` = 11

### Transition 12: COMPLETE
```bash
POST http://localhost:8080/api/admin/workflow/1/transitions
Authorization: Bearer <token>
Content-Type: application/json

{
  "transitionCode": "COMPLETE",
  "transitionName": "Complete",
  "fromStateId": 11,
  "toStateId": 12,
  "requiresComment": false,
  "isActive": true,
  "description": "Case completed successfully"
}
```

**Response:** Save `transitionId` = 12

---

## Step 4: Create Permissions for Each Transition

**Note:** Replace transition IDs with actual IDs from Step 3 responses.

### Permission 1: SUBMIT_APPLICATION - CITIZEN
```bash
POST http://localhost:8080/api/admin/workflow/transitions/1/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CITIZEN",
  "unitLevel": null,
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "ANY_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 2: ENTER_IN_REGISTER - DEALING_ASSISTANT
```bash
POST http://localhost:8080/api/admin/workflow/transitions/2/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "DEALING_ASSISTANT",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 3: RECEIVE_BY_MANDOL - CIRCLE_MANDOL
```bash
POST http://localhost:8080/api/admin/workflow/transitions/3/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_MANDOL",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 4: GENERATE_NOTICE - CIRCLE_MANDOL
```bash
POST http://localhost:8080/api/admin/workflow/transitions/4/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_MANDOL",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 5: SCHEDULE_HEARING - CIRCLE_OFFICER (SDC)
```bash
POST http://localhost:8080/api/admin/workflow/transitions/5/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 6: COMPLETE_HEARING - CIRCLE_OFFICER (SDC)
```bash
POST http://localhost:8080/api/admin/workflow/transitions/6/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 7: APPROVE - CIRCLE_OFFICER (SDC)
```bash
POST http://localhost:8080/api/admin/workflow/transitions/7/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": true,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 8: REJECT - CIRCLE_OFFICER (SDC)
```bash
POST http://localhost:8080/api/admin/workflow/transitions/8/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": true,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 9: PASS_TO_MANDOL - CIRCLE_OFFICER (SDC)
```bash
POST http://localhost:8080/api/admin/workflow/transitions/9/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 10: UPDATE_LAND_RECORD - CIRCLE_MANDOL
```bash
POST http://localhost:8080/api/admin/workflow/transitions/10/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "CIRCLE_MANDOL",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 11: PREPARE_PATTA - DEALING_ASSISTANT
```bash
POST http://localhost:8080/api/admin/workflow/transitions/11/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "DEALING_ASSISTANT",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

### Permission 12: COMPLETE - DEALING_ASSISTANT
```bash
POST http://localhost:8080/api/admin/workflow/transitions/12/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "roleCode": "DEALING_ASSISTANT",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": null,
  "isActive": true
}
```

---

## Complete Summary Table

### States Summary

| Order | State Code | State Name | Initial | Final | Description |
|-------|------------|------------|---------|-------|-------------|
| 1 | CITIZEN_APPLICATION | Citizen Application | ✅ | ❌ | Landowner applies |
| 2 | DA_ENTRY | DA Entry | ❌ | ❌ | Clerk makes entry |
| 3 | MANDOL_RECEIVED | Mandol Received | ❌ | ❌ | Passed to Mandol |
| 4 | NOTICE_GENERATED | Notice Generated | ❌ | ❌ | Notice sent |
| 5 | HEARING_SCHEDULED | Hearing Scheduled | ❌ | ❌ | Date fixed |
| 6 | HEARING_COMPLETED | Hearing Completed | ❌ | ❌ | SDC hears |
| 7 | DECISION_PENDING | Decision Pending | ❌ | ❌ | Decision pending |
| 8 | APPROVED | Approved | ❌ | ❌ | SDC approves |
| 9 | MANDOL_UPDATE | Mandol Update | ❌ | ❌ | Passed to Mandol |
| 10 | LAND_RECORD_UPDATED | Land Record Updated | ❌ | ❌ | Record updated |
| 11 | PATTA_PREPARATION | Patta Preparation | ❌ | ❌ | Patta info |
| 12 | COMPLETED | Completed | ❌ | ✅ | Case completed |
| 13 | REJECTED | Rejected | ❌ | ✅ | Case rejected |

### Transitions Summary

| Transition Code | Transition Name | From State | To State | Requires Comment | Responsible Role |
|----------------|-----------------|------------|----------|------------------|------------------|
| SUBMIT_APPLICATION | Submit Application | CITIZEN_APPLICATION | DA_ENTRY | ❌ | CITIZEN |
| ENTER_IN_REGISTER | Enter in Register | DA_ENTRY | MANDOL_RECEIVED | ❌ | DEALING_ASSISTANT |
| RECEIVE_BY_MANDOL | Receive by Mandol | MANDOL_RECEIVED | NOTICE_GENERATED | ❌ | CIRCLE_MANDOL |
| GENERATE_NOTICE | Generate Notice | NOTICE_GENERATED | HEARING_SCHEDULED | ❌ | CIRCLE_MANDOL |
| SCHEDULE_HEARING | Schedule Hearing | HEARING_SCHEDULED | HEARING_COMPLETED | ❌ | CIRCLE_OFFICER |
| COMPLETE_HEARING | Complete Hearing | HEARING_COMPLETED | DECISION_PENDING | ✅ | CIRCLE_OFFICER |
| APPROVE | Approve | DECISION_PENDING | APPROVED | ✅ | CIRCLE_OFFICER |
| REJECT | Reject | DECISION_PENDING | REJECTED | ✅ | CIRCLE_OFFICER |
| PASS_TO_MANDOL | Pass to Mandol | APPROVED | MANDOL_UPDATE | ❌ | CIRCLE_OFFICER |
| UPDATE_LAND_RECORD | Update Land Record | MANDOL_UPDATE | LAND_RECORD_UPDATED | ❌ | CIRCLE_MANDOL |
| PREPARE_PATTA | Prepare Patta | LAND_RECORD_UPDATED | PATTA_PREPARATION | ❌ | DEALING_ASSISTANT |
| COMPLETE | Complete | PATTA_PREPARATION | COMPLETED | ❌ | DEALING_ASSISTANT |

### Permissions Summary

| Transition | Role | Unit Level | Can Initiate | Can Approve | Hierarchy Rule |
|------------|------|------------|--------------|-------------|----------------|
| SUBMIT_APPLICATION | CITIZEN | null | ✅ | ❌ | ANY_UNIT |
| ENTER_IN_REGISTER | DEALING_ASSISTANT | CIRCLE | ✅ | ❌ | SAME_UNIT |
| RECEIVE_BY_MANDOL | CIRCLE_MANDOL | CIRCLE | ✅ | ❌ | SAME_UNIT |
| GENERATE_NOTICE | CIRCLE_MANDOL | CIRCLE | ✅ | ❌ | SAME_UNIT |
| SCHEDULE_HEARING | CIRCLE_OFFICER | CIRCLE | ✅ | ❌ | SAME_UNIT |
| COMPLETE_HEARING | CIRCLE_OFFICER | CIRCLE | ✅ | ❌ | SAME_UNIT |
| APPROVE | CIRCLE_OFFICER | CIRCLE | ✅ | ✅ | SAME_UNIT |
| REJECT | CIRCLE_OFFICER | CIRCLE | ✅ | ✅ | SAME_UNIT |
| PASS_TO_MANDOL | CIRCLE_OFFICER | CIRCLE | ✅ | ❌ | SAME_UNIT |
| UPDATE_LAND_RECORD | CIRCLE_MANDOL | CIRCLE | ✅ | ❌ | SAME_UNIT |
| PREPARE_PATTA | DEALING_ASSISTANT | CIRCLE | ✅ | ❌ | SAME_UNIT |
| COMPLETE | DEALING_ASSISTANT | CIRCLE | ✅ | ❌ | SAME_UNIT |

---

## Quick Setup Script (cURL)

Save this as `setup-mutation-workflow.sh`:

```bash
#!/bin/bash

# Configuration
API_URL="http://localhost:8080"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin@123"

# Step 1: Login and get token
echo "Step 1: Logging in as admin..."
TOKEN=$(curl -s -X POST "$API_URL/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}" \
  | jq -r '.data.token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "Error: Failed to login. Please check credentials."
  exit 1
fi

echo "Token obtained: ${TOKEN:0:20}..."

# Step 2: Create Workflow
echo "Step 2: Creating workflow..."
WORKFLOW_RESPONSE=$(curl -s -X POST "$API_URL/api/admin/workflow/definitions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "workflowCode": "MUTATION_GIFT_SALE",
    "workflowName": "Mutation (after Gift/Sale Deeds)",
    "description": "Workflow for mutation after gift/sale deeds registration",
    "isActive": true
  }')

WORKFLOW_ID=$(echo $WORKFLOW_RESPONSE | jq -r '.data.id')
echo "Workflow created with ID: $WORKFLOW_ID"

# Step 3: Create States (simplified - you'll need to create all 13 states)
echo "Step 3: Creating states..."
# Add state creation commands here...

echo "Workflow setup completed!"
```

---

## Verification Steps

After creating all states, transitions, and permissions:

### 1. Verify Workflow
```bash
GET http://localhost:8080/api/admin/workflow/definitions/MUTATION_GIFT_SALE
Authorization: Bearer <token>
```

### 2. Verify All States
```bash
GET http://localhost:8080/api/admin/workflow/1/states
Authorization: Bearer <token>
```

Should return 13 states ordered by stateOrder.

### 3. Verify All Transitions
```bash
GET http://localhost:8080/api/admin/workflow/1/transitions/all
Authorization: Bearer <token>
```

Should return 12 transitions.

### 4. Verify Permissions for a Transition
```bash
GET http://localhost:8080/api/admin/workflow/transitions/7/permissions
Authorization: Bearer <token>
```

Should return permissions for APPROVE transition.

---

## Link Workflow to Case Type

After workflow is configured, link it to the case type:

```bash
PUT http://localhost:8080/api/case-types/{caseTypeId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "workflowCode": "MUTATION_GIFT_SALE"
}
```

---

## Notes

1. **State IDs**: Replace state IDs (1, 2, 3...) with actual IDs from your API responses
2. **Transition IDs**: Replace transition IDs (1, 2, 3...) with actual IDs from your API responses
3. **Workflow ID**: Replace workflow ID (1) with actual ID from workflow creation response
4. **Token**: Use the same admin token for all requests (valid for 1 hour)

---

## Complete Workflow Visualization

```
┌─────────────────────┐
│ CITIZEN_APPLICATION │ (Initial)
└──────────┬──────────┘
           │ SUBMIT_APPLICATION (CITIZEN)
           ▼
┌─────────────────────┐
│     DA_ENTRY        │
└──────────┬──────────┘
           │ ENTER_IN_REGISTER (DEALING_ASSISTANT)
           ▼
┌─────────────────────┐
│   MANDOL_RECEIVED   │
└──────────┬──────────┘
           │ RECEIVE_BY_MANDOL (CIRCLE_MANDOL)
           ▼
┌─────────────────────┐
│  NOTICE_GENERATED   │
└──────────┬──────────┘
           │ GENERATE_NOTICE (CIRCLE_MANDOL)
           ▼
┌─────────────────────┐
│  HEARING_SCHEDULED  │
└──────────┬──────────┘
           │ SCHEDULE_HEARING (CIRCLE_OFFICER)
           ▼
┌─────────────────────┐
│  HEARING_COMPLETED  │
└──────────┬──────────┘
           │ COMPLETE_HEARING (CIRCLE_OFFICER)
           ▼
┌─────────────────────┐
│  DECISION_PENDING   │
└──────┬───────────┬──┘
       │           │
       │ APPROVE   │ REJECT
       │ (SDC)     │ (SDC)
       ▼           ▼
┌──────────┐   ┌──────────┐
│ APPROVED │   │ REJECTED │ (Final)
└────┬─────┘   └──────────┘
     │ PASS_TO_MANDOL (CIRCLE_OFFICER)
     ▼
┌─────────────────────┐
│   MANDOL_UPDATE     │
└──────────┬──────────┘
           │ UPDATE_LAND_RECORD (CIRCLE_MANDOL)
           ▼
┌─────────────────────┐
│ LAND_RECORD_UPDATED │
└──────────┬──────────┘
           │ PREPARE_PATTA (DEALING_ASSISTANT)
           ▼
┌─────────────────────┐
│ PATTA_PREPARATION   │
└──────────┬──────────┘
           │ COMPLETE (DEALING_ASSISTANT)
           ▼
┌─────────────────────┐
│     COMPLETED       │ (Final)
└─────────────────────┘
```

---

**This completes the full workflow configuration for Mutation (after Gift/Sale Deeds)!**

---

## Add-on: Return for Correction (Best Approach)

To support returning an application to the citizen for correction (without making citizen a workflow role), use the **RETURNED_FOR_CORRECTION** state and these transitions:

### New State

```json
{
  "stateCode": "RETURNED_FOR_CORRECTION",
  "stateName": "Returned for Correction",
  "stateOrder": 14,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Application returned to citizen for correction"
}
```

### New Transitions

**Return by DA**
```json
{
  "transitionCode": "RETURN_FROM_DA",
  "transitionName": "Return for Correction (DA)",
  "fromStateId": <ID_OF_DA_ENTRY>,
  "toStateId": <ID_OF_RETURNED_FOR_CORRECTION>,
  "requiresComment": true,
  "isActive": true,
  "description": "DA returns application for correction"
}
```

**Return by Mandol**
```json
{
  "transitionCode": "RETURN_FROM_MANDOL",
  "transitionName": "Return for Correction (Mandol)",
  "fromStateId": <ID_OF_MANDOL_RECEIVED>,
  "toStateId": <ID_OF_RETURNED_FOR_CORRECTION>,
  "requiresComment": true,
  "isActive": true,
  "description": "Mandol returns application for correction"
}
```

**Return by SDC**
```json
{
  "transitionCode": "RETURN_FROM_SDC",
  "transitionName": "Return for Correction (SDC)",
  "fromStateId": <ID_OF_DECISION_PENDING>,
  "toStateId": <ID_OF_RETURNED_FOR_CORRECTION>,
  "requiresComment": true,
  "isActive": true,
  "description": "SDC returns application for correction"
}
```

**Review correction (DA)**
```json
{
  "transitionCode": "REVIEW_CORRECTION",
  "transitionName": "Review Correction",
  "fromStateId": <ID_OF_RETURNED_FOR_CORRECTION>,
  "toStateId": <ID_OF_DA_ENTRY>,
  "requiresComment": false,
  "isActive": true,
  "description": "DA reviews corrected application and continues workflow"
}
```

### Permissions for these transitions

```json
// RETURN_FROM_DA
{
  "roleCode": "DEALING_ASSISTANT",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "isActive": true
}
```

```json
// RETURN_FROM_MANDOL
{
  "roleCode": "CIRCLE_MANDOL",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "isActive": true
}
```

```json
// RETURN_FROM_SDC
{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "isActive": true
}
```

```json
// REVIEW_CORRECTION
{
  "roleCode": "DEALING_ASSISTANT",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "isActive": true
}
```

### Citizen Resubmission API

Citizen updates case data without triggering workflow transition:

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
