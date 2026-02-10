# Workflow Configuration API Documentation

## Overview

Complete API documentation for admin-configurable workflow management. Admins can now create, update, and delete workflows, states, transitions, and permissions through REST APIs without code changes.

## Base URL
```
http://localhost:8080/api/admin/workflow
```

## Authentication
All endpoints require ADMIN authority:
```
Authorization: Bearer <admin_token>
```

---

## 1. Workflow Definition Management

### 1.1 Get All Workflows
**Endpoint:** `GET /api/admin/workflow/definitions`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Workflows retrieved successfully",
  "data": [
    {
      "id": 1,
      "workflowCode": "MUTATION_GIFT_SALE",
      "workflowName": "Mutation (after Gift/Sale Deeds)",
      "description": "Workflow for mutation after gift/sale deeds registration",
      "isActive": true,
      "version": 1,
      "createdAt": "2026-01-09T10:00:00",
      "updatedAt": "2026-01-09T10:00:00"
    }
  ]
}
```

### 1.2 Get Active Workflows
**Endpoint:** `GET /api/admin/workflow/definitions/active`

**Response (200 OK):** Same format as above, but only active workflows

### 1.3 Get Workflow by ID
**Endpoint:** `GET /api/admin/workflow/definitions/id/{id}`

**Response (200 OK):** Single workflow object

### 1.4 Get Workflow by Code
**Endpoint:** `GET /api/admin/workflow/definitions/{workflowCode}`

**Example:** `GET /api/admin/workflow/definitions/MUTATION_GIFT_SALE`

**Response (200 OK):** Single workflow object

### 1.5 Create Workflow
**Endpoint:** `POST /api/admin/workflow/definitions`

**Request Body:**
```json
{
  "workflowCode": "MUTATION_GIFT_SALE",
  "workflowName": "Mutation (after Gift/Sale Deeds)",
  "description": "Workflow for mutation after gift/sale deeds registration",
  "isActive": true
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Workflow created successfully",
  "data": {
    "id": 1,
    "workflowCode": "MUTATION_GIFT_SALE",
    "workflowName": "Mutation (after Gift/Sale Deeds)",
    "description": "Workflow for mutation after gift/sale deeds registration",
    "isActive": true,
    "version": 1
  }
}
```

**Validation:**
- `workflowCode`: Required, max 50 characters, must be unique
- `workflowName`: Required, max 200 characters
- `description`: Optional, max 1000 characters

### 1.6 Update Workflow
**Endpoint:** `PUT /api/admin/workflow/definitions/{id}`

**Request Body:** Same as Create Workflow

**Response (200 OK):** Updated workflow object

**Note:** Version is automatically incremented on update

### 1.7 Delete Workflow (Soft Delete)
**Endpoint:** `DELETE /api/admin/workflow/definitions/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Workflow deleted successfully",
  "data": null
}
```

**Validation:**
- Cannot delete if linked to any case types
- Cannot delete if has active case instances

---

## 2. Workflow State Management

### 2.1 Get States for Workflow
**Endpoint:** `GET /api/admin/workflow/{workflowId}/states`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "States retrieved successfully",
  "data": [
    {
      "id": 1,
      "workflowId": 1,
      "workflowCode": "MUTATION_GIFT_SALE",
      "stateCode": "CITIZEN_APPLICATION",
      "stateName": "Citizen Application",
      "stateOrder": 1,
      "isInitialState": true,
      "isFinalState": false,
      "description": "Landowner applies for mutation"
    }
  ]
}
```

### 2.2 Get State by ID
**Endpoint:** `GET /api/admin/workflow/states/{id}`

**Response (200 OK):** Single state object

### 2.3 Create State
**Endpoint:** `POST /api/admin/workflow/{workflowId}/states`

**Request Body:**
```json
{
  "stateCode": "CITIZEN_APPLICATION",
  "stateName": "Citizen Application",
  "stateOrder": 1,
  "isInitialState": true,
  "isFinalState": false,
  "description": "Landowner applies for mutation in Form no. 16"
}
```

**Response (201 Created):** Created state object

**Validation:**
- `stateCode`: Required, max 50 characters, must be unique within workflow
- `stateName`: Required, max 200 characters
- `stateOrder`: Required, integer
- If `isInitialState` is set to true, other initial states in the workflow are automatically unset

### 2.4 Update State
**Endpoint:** `PUT /api/admin/workflow/states/{id}`

**Request Body:** Same as Create State

**Response (200 OK):** Updated state object

### 2.5 Delete State
**Endpoint:** `DELETE /api/admin/workflow/states/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "State deleted successfully",
  "data": null
}
```

**Validation:**
- Cannot delete if used in any transitions
- Cannot delete if currently used in any case instances

---

## 3. Workflow Transition Management

### 3.1 Get Transitions for Workflow (Active Only)
**Endpoint:** `GET /api/admin/workflow/{workflowId}/transitions`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Transitions retrieved successfully",
  "data": [
    {
      "id": 1,
      "workflowId": 1,
      "fromStateId": 1,
      "toStateId": 2,
      "transitionCode": "SUBMIT_APPLICATION",
      "transitionName": "Submit Application",
      "requiresComment": false,
      "isActive": true,
      "description": "Landowner submits mutation application"
    }
  ]
}
```

### 3.2 Get All Transitions for Workflow (Including Inactive)
**Endpoint:** `GET /api/admin/workflow/{workflowId}/transitions/all`

**Response (200 OK):** Same format as above, but includes inactive transitions

### 3.3 Get Transition by ID
**Endpoint:** `GET /api/admin/workflow/transitions/{id}`

**Response (200 OK):** Single transition object

### 3.4 Create Transition
**Endpoint:** `POST /api/admin/workflow/{workflowId}/transitions`

**Request Body:**
```json
{
  "transitionCode": "SUBMIT_APPLICATION",
  "transitionName": "Submit Application",
  "fromStateId": 1,
  "toStateId": 2,
  "requiresComment": false,
  "isActive": true,
  "description": "Landowner submits mutation application"
}
```

**Response (201 Created):** Created transition object

**Validation:**
- `transitionCode`: Required, max 50 characters, must be unique within workflow
- `transitionName`: Required, max 200 characters
- `fromStateId`: Required, must exist and belong to the workflow
- `toStateId`: Required, must exist and belong to the workflow
- Both states must belong to the same workflow

### 3.5 Update Transition
**Endpoint:** `PUT /api/admin/workflow/transitions/{id}`

**Request Body:** Same as Create Transition

**Response (200 OK):** Updated transition object

### 3.6 Delete Transition
**Endpoint:** `DELETE /api/admin/workflow/transitions/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Transition deleted successfully",
  "data": null
}
```

**Validation:**
- Cannot delete if has active permissions (delete permissions first)

---

## 4. Workflow Permission Management

### 4.1 Get Permissions for Transition
**Endpoint:** `GET /api/admin/workflow/transitions/{transitionId}/permissions`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Permissions retrieved successfully",
  "data": [
    {
      "id": 1,
      "transitionId": 1,
      "transitionCode": "SUBMIT_APPLICATION",
      "roleCode": "CITIZEN",
      "unitLevel": null,
      "canInitiate": true,
      "canApprove": false,
      "hierarchyRule": "ANY_UNIT",
      "conditions": null,
      "isActive": true
    }
  ]
}
```

### 4.2 Get Permission by ID
**Endpoint:** `GET /api/admin/workflow/permissions/{id}`

**Response (200 OK):** Single permission object

### 4.3 Create Permission
**Endpoint:** `POST /api/admin/workflow/transitions/{transitionId}/permissions`

**Request Body:**
```json
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

**Response (201 Created):** Created permission object

**Validation:**
- `roleCode`: Required, must exist in RoleMaster
- `unitLevel`: Optional (null means all levels)
- `hierarchyRule`: Optional, values: "SAME_UNIT", "PARENT_UNIT", "ANY_UNIT", "SUPERVISOR"
- Cannot create duplicate permission for same transition+role+unitLevel combination

**Role Codes Available:**
- `CITIZEN` - Citizen/Landowner
- `DEALING_ASSISTANT` - Clerk/Dealing Assistant
- `CIRCLE_MANDOL` - Circle Mandol
- `CIRCLE_OFFICER` - SDC (Sub-Divisional Circle Officer)
- `SUB_DIVISION_OFFICER` - SDO
- `DISTRICT_OFFICER` - DC
- `STATE_ADMIN` - State Administrator
- `SUPER_ADMIN` - Super Administrator

**Unit Levels:**
- `STATE`
- `DISTRICT`
- `SUB_DIVISION`
- `CIRCLE`

### 4.4 Update Permission
**Endpoint:** `PUT /api/admin/workflow/permissions/{id}`

**Request Body:** Same as Create Permission

**Response (200 OK):** Updated permission object

### 4.5 Delete Permission
**Endpoint:** `DELETE /api/admin/workflow/permissions/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Permission deleted successfully",
  "data": null
}
```

---

## 5. Complete Workflow Configuration Example

### Step-by-Step: Creating Mutation Gift/Sale Workflow

#### Step 1: Create Workflow
```bash
POST /api/admin/workflow/definitions
{
  "workflowCode": "MUTATION_GIFT_SALE",
  "workflowName": "Mutation (after Gift/Sale Deeds)",
  "description": "Workflow for mutation after gift/sale deeds registration",
  "isActive": true
}
```

#### Step 2: Create States (in order)
```bash
# State 1: Initial State
POST /api/admin/workflow/{workflowId}/states
{
  "stateCode": "CITIZEN_APPLICATION",
  "stateName": "Citizen Application",
  "stateOrder": 1,
  "isInitialState": true,
  "isFinalState": false,
  "description": "Landowner applies for mutation in Form no. 16"
}

# State 2: DA Entry
POST /api/admin/workflow/{workflowId}/states
{
  "stateCode": "DA_ENTRY",
  "stateName": "DA Entry",
  "stateOrder": 2,
  "isInitialState": false,
  "isFinalState": false,
  "description": "Clerk makes entry in Mutation Register"
}

# ... Continue for all states
```

#### Step 3: Create Transitions
```bash
# Transition 1: Submit Application
POST /api/admin/workflow/{workflowId}/transitions
{
  "transitionCode": "SUBMIT_APPLICATION",
  "transitionName": "Submit Application",
  "fromStateId": <CITIZEN_APPLICATION_STATE_ID>,
  "toStateId": <DA_ENTRY_STATE_ID>,
  "requiresComment": false,
  "isActive": true,
  "description": "Landowner submits mutation application"
}

# ... Continue for all transitions
```

#### Step 4: Create Permissions
```bash
# Permission for SUBMIT_APPLICATION
POST /api/admin/workflow/transitions/{transitionId}/permissions
{
  "roleCode": "CITIZEN",
  "unitLevel": null,
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "ANY_UNIT",
  "isActive": true
}

# Permission for ENTER_IN_REGISTER
POST /api/admin/workflow/transitions/{transitionId}/permissions
{
  "roleCode": "DEALING_ASSISTANT",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "isActive": true
}

# ... Continue for all transitions
```

#### Step 5: Link Workflow to Case Type
```bash
PUT /api/case-types/{caseTypeId}
{
  "workflowCode": "MUTATION_GIFT_SALE"
}
```

---

## 6. Error Responses

### Validation Error (400 Bad Request)
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "workflowCode",
      "message": "Workflow code is required"
    }
  ],
  "path": "/api/admin/workflow/definitions"
}
```

### Not Found Error (404 Not Found)
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Workflow not found: 999",
  "path": "/api/admin/workflow/definitions/999"
}
```

### Conflict Error (400 Bad Request)
```json
{
  "success": false,
  "message": "Workflow with code 'MUTATION_GIFT_SALE' already exists"
}
```

### Cannot Delete Error (400 Bad Request)
```json
{
  "success": false,
  "message": "Cannot delete workflow. It is linked to case types: Mutation (after Gift/Sale Deeds)"
}
```

---

## 7. Important Notes

1. **Workflow Initialization**: The `WorkflowDataInitializer` automatically creates all 9 workflows on application startup. You can modify them through APIs.

2. **State Order**: States should be created in order (stateOrder: 1, 2, 3, ...) for better visualization.

3. **Initial State**: Only one state per workflow can be marked as initial state. Setting a new initial state automatically unsets the previous one.

4. **Final States**: Multiple final states are allowed (e.g., APPROVED, REJECTED).

5. **Hierarchy Rules**:
   - `SAME_UNIT`: User must be in the same unit as the case
   - `PARENT_UNIT`: User must be in the parent unit of the case's unit
   - `ANY_UNIT`: User can be in any unit
   - `SUPERVISOR`: User must be supervisor (can be enhanced)

6. **Permissions**: Each transition can have multiple permissions for different roles/unit levels.

7. **Soft Delete**: Workflows are soft-deleted (isActive=false), not permanently deleted.

8. **Versioning**: Workflow version is automatically incremented on update.

---

## 8. Transition Conditions (Optional)

You can attach extra rules to a transition using `WorkflowPermission.conditions` (JSON string).
If conditions are not met, the transition is **not allowed**.

### Supported keys

- `caseTypeCodesAllowed`: list of case type codes
- `casePriorityIn`: list of allowed priorities
- `caseDataFieldsRequired`: list of fields that must exist in `caseData`
- `caseDataFieldEquals`: map of field → expected value
- `workflowDataFieldsRequired`: list of fields required in `workflowData`

### Example

```json
{
  "caseTypeCodesAllowed": ["MUTATION_GIFT_SALE", "PARTITION"],
  "casePriorityIn": ["HIGH", "URGENT"],
  "caseDataFieldsRequired": ["registeredDeedNumber", "deedRegistrationDate"],
  "caseDataFieldEquals": {"ownershipType": "INDIVIDUAL"},
  "workflowDataFieldsRequired": ["noticeIssued"]
}
```

### How to use in API

When creating/updating a permission:

```json
{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": true,
  "hierarchyRule": "SAME_UNIT",
  "conditions": "{\"casePriorityIn\":[\"HIGH\"],\"caseDataFieldsRequired\":[\"registeredDeedNumber\"]}",
  "isActive": true
}
```

---

## 9. API Endpoint Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/workflow/definitions` | Get all workflows |
| GET | `/api/admin/workflow/definitions/active` | Get active workflows |
| GET | `/api/admin/workflow/definitions/id/{id}` | Get workflow by ID |
| GET | `/api/admin/workflow/definitions/{code}` | Get workflow by code |
| POST | `/api/admin/workflow/definitions` | Create workflow |
| PUT | `/api/admin/workflow/definitions/{id}` | Update workflow |
| DELETE | `/api/admin/workflow/definitions/{id}` | Delete workflow |
| GET | `/api/admin/workflow/{workflowId}/states` | Get workflow states |
| GET | `/api/admin/workflow/states/{id}` | Get state by ID |
| POST | `/api/admin/workflow/{workflowId}/states` | Create state |
| PUT | `/api/admin/workflow/states/{id}` | Update state |
| DELETE | `/api/admin/workflow/states/{id}` | Delete state |
| GET | `/api/admin/workflow/{workflowId}/transitions` | Get active transitions |
| GET | `/api/admin/workflow/{workflowId}/transitions/all` | Get all transitions |
| GET | `/api/admin/workflow/transitions/{id}` | Get transition by ID |
| POST | `/api/admin/workflow/{workflowId}/transitions` | Create transition |
| PUT | `/api/admin/workflow/transitions/{id}` | Update transition |
| DELETE | `/api/admin/workflow/transitions/{id}` | Delete transition |
| GET | `/api/admin/workflow/transitions/{transitionId}/permissions` | Get permissions |
| GET | `/api/admin/workflow/permissions/{id}` | Get permission by ID |
| POST | `/api/admin/workflow/transitions/{transitionId}/permissions` | Create permission |
| PUT | `/api/admin/workflow/permissions/{id}` | Update permission |
| DELETE | `/api/admin/workflow/permissions/{id}` | Delete permission |

---

## 10. Swagger UI

Access interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

All endpoints are documented with request/response examples in Swagger UI.

---

**Last Updated**: After workflow configuration APIs implementation and conditions support
