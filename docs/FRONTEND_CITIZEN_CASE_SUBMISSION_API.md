# Citizen Case Submission API Documentation

## Overview

This document provides complete request and response parameters for citizen case submission and workflow-related APIs used by the frontend.

All responses follow the standard format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-01-20T12:00:00"
}
```

---

## 1. Get Active Case Types

**Endpoint:** `GET /api/case-types/active`

**Headers:** None

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Active case types retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Mutation (after Gift/Sale Deeds)",
      "code": "MUTATION_GIFT_SALE",
      "description": "Mutation after gift or sale deeds",
      "isActive": true
    }
  ]
}
```

---

## 2. Get Form Schema for Case Type

**Endpoint:** `GET /api/cases/form-schema/{caseTypeId}`

**Path Params:**
- `caseTypeId` (Long) – case type id

**Headers:** None

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Form schema retrieved successfully",
  "data": {
    "caseTypeId": 1,
    "caseTypeName": "Mutation (after Gift/Sale Deeds)",
    "fields": [
      {
        "id": 1,
        "fieldName": "registeredDeedNumber",
        "fieldLabel": "Registered Deed Number",
        "fieldType": "TEXT",
        "isRequired": true,
        "displayOrder": 1,
        "placeholder": "Enter deed number"
      }
    ]
  }
}
```

---

## 3. Submit Case (Citizen)

**Endpoint:** `POST /api/cases`

**Headers:**
```
Authorization: Bearer <citizenToken>
X-User-Id: <citizenId>
Content-Type: application/json
```

**Request Body:**
```json
{
  "caseTypeId": 1,
  "unitId": 101,
  "subject": "Mutation after gift/sale",
  "description": "Brief description",
  "priority": "MEDIUM",
  "caseData": "{ \"registeredDeedNumber\": \"123/2024\", \"deedRegistrationDate\": \"2024-01-15\" }"
}
```

**Request Parameters:**
- `caseTypeId` (Long, required) – case type id
- `unitId` (Long, required) – unit where case is filed
- `subject` (String, required)
- `description` (String, optional)
- `priority` (String, optional) – LOW | MEDIUM | HIGH | URGENT
- `caseData` (String JSON, optional) – form data

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Case created successfully",
  "data": {
    "id": 10,
    "caseNumber": "MUTATION_GIFT_SALE-IMW001-20260120-0001",
    "caseTypeId": 1,
    "applicantId": 5,
    "unitId": 101,
    "status": "CITIZEN_APPLICATION",
    "subject": "Mutation after gift/sale",
    "description": "Brief description",
    "priority": "MEDIUM",
    "caseData": "{...}"
  }
}
```

**Errors (400 Bad Request):**
```json
{
  "timestamp": "2026-01-20T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Form validation failed: registeredDeedNumber - Field is required",
  "path": "/api/cases"
}
```

---

## 4. Get Citizen Cases

**Endpoint:** `GET /api/cases/applicant/{applicantId}`

**Path Params:**
- `applicantId` (Long) – citizen id

**Headers:**
```
Authorization: Bearer <citizenToken>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Cases retrieved successfully",
  "data": [
    {
      "id": 10,
      "caseNumber": "MUTATION_GIFT_SALE-IMW001-20260120-0001",
      "status": "DA_ENTRY",
      "subject": "Mutation after gift/sale",
      "priority": "MEDIUM"
    }
  ]
}
```

---

## 5. Get Workflow History (Citizen View)

**Endpoint:** `GET /api/cases/{caseId}/history`

**Path Params:**
- `caseId` (Long)

**Headers:**
```
Authorization: Bearer <citizenToken>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Workflow history retrieved successfully",
  "data": [
    {
      "id": 1,
      "caseId": 10,
      "fromStateId": 2,
      "toStateId": 14,
      "performedByRole": "DEALING_ASSISTANT",
      "comments": "Please upload clear copy of deed",
      "performedAt": "2026-01-20T13:00:00"
    }
  ]
}
```

Use this to show **return for correction reason** to citizen.

---

## 6. Resubmit Case (Citizen Correction)

**Endpoint:** `PUT /api/cases/{caseId}/resubmit`

**Headers:**
```
Authorization: Bearer <citizenToken>
X-User-Id: <citizenId>
Content-Type: application/json
```

**Request Body:**
```json
{
  "caseData": "{ \"registeredDeedNumber\": \"123/2024\", \"deedRegistrationDate\": \"2024-01-15\" }",
  "remarks": "Updated deed scan and corrected details"
}
```

**Request Parameters:**
- `caseData` (String JSON, required)
- `remarks` (String, optional)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Case resubmitted successfully",
  "data": {
    "id": 10,
    "caseNumber": "MUTATION_GIFT_SALE-IMW001-20260120-0001",
    "status": "RETURNED_FOR_CORRECTION",
    "caseData": "{...}",
    "remarks": "Updated deed scan and corrected details"
  }
}
```

**Error if not in correction state:**
```json
{
  "timestamp": "2026-01-20T12:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Case is not in RETURNED_FOR_CORRECTION state",
  "path": "/api/cases/10/resubmit"
}
```

---

## 7. Workflow Status Behavior (Frontend Logic)

- `case.status` = current workflow state code
- Use it directly for badges and display
- When status is `RETURNED_FOR_CORRECTION`, show “Edit & Resubmit”

---

## Summary

| Purpose | API |
|---------|-----|
| Get case types | `GET /api/case-types/active` |
| Get form schema | `GET /api/cases/form-schema/{caseTypeId}` |
| Submit case | `POST /api/cases` |
| List citizen cases | `GET /api/cases/applicant/{applicantId}` |
| View history | `GET /api/cases/{caseId}/history` |
| Resubmit correction | `PUT /api/cases/{caseId}/resubmit` |

---

**Note:** Citizen does not perform workflow transitions. All workflow transitions are handled by officers, based on role permissions and conditions.
