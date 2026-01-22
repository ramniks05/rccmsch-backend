# RCCMS Backend API Documentation

## Overview
This document describes all API endpoints for the RCCMS (Revenue Court Case Monitoring System) backend, including new endpoints for Act, Court, and Case Nature management.

**Base URL**: `http://localhost:8080`

---

## Table of Contents
1. [Administrative Units API](#administrative-units-api)
2. [Case Types API](#case-types-api) - **UPDATED**
3. [Acts API](#acts-api) - **NEW**
4. [Case Natures API](#case-natures-api) - **NEW**
5. [Courts API](#courts-api) - **NEW**
6. [Public APIs](#public-apis) - **NEW**
7. [Cases API](#cases-api) - **UPDATED**

---

## Administrative Units API

### Get Root Units (Public)
**GET** `/api/admin-units/root`

**Description**: Get all root administrative units (State level)

**Response**:
```json
{
  "success": true,
  "message": "Root admin units retrieved successfully",
  "data": [
    {
      "unitId": 1,
      "unitCode": "MANIPUR",
      "unitName": "Manipur",
      "unitLevel": "STATE",
      "lgdCode": 1001,
      "parentUnitId": null,
      "parentUnitName": null,
      "isActive": true
    }
  ]
}
```

### Get Units by Parent
**GET** `/api/admin-units/parent/{parentId}`

**Description**: Get child units of a parent unit

**Response**: Same structure as above

---

## Case Types API

### Get All Active Case Types
**GET** `/api/case-types/active`

**Description**: Get all active case types (used by frontend)

**Response**:
```json
{
  "success": true,
  "message": "Active case types retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Mutation (after Gift/Sale Deeds)",
      "code": "MUTATION_GIFT_SALE",
      "description": "Mutation after Gift or Sale Deeds",
      "actId": 1,
      "actName": "Manipur Land Revenue and Land Reforms Act",
      "actCode": "MLR_LR_ACT_1960",
      "actYear": 1960,
      "workflowCode": "MUTATION_WORKFLOW",
      "isActive": true,
      "createdAt": "2024-01-15T10:00:00",
      "updatedAt": "2024-01-15T10:00:00"
    }
  ]
}
```

**Changes**: 
- Added `actId`, `actName`, `actCode`, `actYear` fields
- Added `stateCode` field
- Added `createdAt`, `updatedAt` fields

### Create Case Type
**POST** `/api/case-types`

**Request Body**:
```json
{
  "name": "Mutation (after Gift/Sale Deeds)",
  "code": "MUTATION_GIFT_SALE",
  "description": "Mutation after Gift or Sale Deeds",
  "actId": 1,
  "workflowCode": "MUTATION_WORKFLOW",
  "isActive": true
}
```

**Changes**: 
- Added `actId` field (optional, links to Act)
- Added `stateCode` field (optional, for multi-state support)

---

## Acts API (NEW)

### Get All Active Acts
**GET** `/api/admin/acts`

**Description**: Get all active acts

**Response**:
```json
{
  "success": true,
  "message": "Acts retrieved successfully",
  "data": [
    {
      "id": 1,
      "actCode": "MLR_LR_ACT_1960",
      "actName": "Manipur Land Revenue and Land Reforms Act",
      "actYear": 1960,
      "description": "Main act governing land revenue in Manipur",
      "sections": "{\"93\":\"Appeals\",\"95\":\"Revision\",\"96\":\"Review\"}",
      "isActive": true,
      "createdAt": "2024-01-15T10:00:00",
      "updatedAt": "2024-01-15T10:00:00"
    }
  ]
}
```

### Create Act
**POST** `/api/admin/acts`

**Request Body**:
```json
{
  "actCode": "MLR_LR_ACT_1960",
  "actName": "Manipur Land Revenue and Land Reforms Act",
  "actYear": 1960,
  "description": "Main act governing land revenue in Manipur",
  "sections": "{\"93\":\"Appeals\",\"95\":\"Revision\",\"96\":\"Review\"}",
  "isActive": true
}
```

---

## Case Natures API (NEW)

### Get Case Natures by Case Type (Public)
**GET** `/api/public/case-natures/case-type/{caseTypeId}`

**Description**: Get all active case natures for a specific case type. Used by frontend when user selects a case type.

**Parameters**:
- `caseTypeId` (path, required): Case type ID

**Response**:
```json
{
  "success": true,
  "message": "Case natures retrieved successfully",
  "data": [
    {
      "id": 1,
      "caseTypeId": 1,
      "caseTypeName": "Mutation (after Gift/Sale Deeds)",
      "caseTypeCode": "MUTATION_GIFT_SALE",
      "natureCode": "NEW_FILE",
      "natureName": "New File",
      "courtLevel": "CIRCLE",
      "courtTypes": ["SDC_COURT"],
      "fromLevel": null,
      "isAppeal": false,
      "appealOrder": 0,
      "description": "Original filing of a new case",
      "stateCode": "MANIPUR",
      "isActive": true,
      "displayOrder": 1,
      "createdAt": "2024-01-15T10:00:00",
      "updatedAt": "2024-01-15T10:00:00"
    },
    {
      "id": 2,
      "caseTypeId": 1,
      "caseTypeName": "Mutation (after Gift/Sale Deeds)",
      "caseTypeCode": "MUTATION_GIFT_SALE",
      "natureCode": "FIRST_APPEAL_SDO",
      "natureName": "First Appeal (from SDO order)",
      "courtLevel": "DISTRICT",
      "courtTypes": ["DC_COURT"],
      "fromLevel": "SUB_DIVISION",
      "isAppeal": true,
      "appealOrder": 1,
      "description": "First appeal from Sub-Division Officer order",
      "isActive": true,
      "displayOrder": 2,
      "createdAt": "2024-01-15T10:00:00",
      "updatedAt": "2024-01-15T10:00:00"
    }
  ]
}
```

**Frontend Usage**: 
1. User selects Case Type → Call this API
2. Show dropdown of case natures
3. User selects Case Nature → Call `/api/public/courts/available` to get courts

### Create Case Nature (Admin)
**POST** `/api/admin/case-natures`

**Request Body**:
```json
{
  "caseTypeId": 1,
  "natureCode": "NEW_FILE",
  "natureName": "New File",
  "courtLevel": "CIRCLE",
  "courtTypes": ["SDC_COURT"],
  "fromLevel": null,
  "isAppeal": false,
  "appealOrder": 0,
  "description": "Original filing of a new case",
  "isActive": true,
  "displayOrder": 1
}
```

**Field Descriptions**:
- `courtLevel`: `CIRCLE`, `SUB_DIVISION`, `DISTRICT`, or `STATE`
- `courtTypes`: Array of court types (e.g., `["DC_COURT", "REVENUE_TRIBUNAL"]`)
- `fromLevel`: For appeals - level of original order (null for new files)
- `isAppeal`: true for appeals, false for new files/revisions/reviews
- `appealOrder`: 1 for first appeal, 2 for second appeal, 0 for others

---

## Courts API (NEW)

### Get Available Courts (Public)
**GET** `/api/public/courts/available?caseNatureId={caseNatureId}&unitId={unitId}`

**Description**: Get available courts based on case nature selection and user's administrative unit. This is the key endpoint for frontend court selection.

**Parameters**:
- `caseNatureId` (query, required): Case nature ID selected by user
- `unitId` (query, required): User's current administrative unit ID

**Response**:
```json
{
  "success": true,
  "message": "Available courts retrieved successfully",
  "data": {
    "caseNature": {
      "id": 2,
      "caseTypeId": 1,
      "caseTypeName": "Mutation (after Gift/Sale Deeds)",
      "caseTypeCode": "MUTATION_GIFT_SALE",
      "natureCode": "FIRST_APPEAL_SDO",
      "natureName": "First Appeal (from SDO order)",
      "courtLevel": "DISTRICT",
      "courtTypes": ["DC_COURT"],
      "fromLevel": "SUB_DIVISION",
      "isAppeal": true,
      "appealOrder": 1
    },
    "courts": [
      {
        "id": 10,
        "courtCode": "DIST_IMPWEST_DC",
        "courtName": "Imphal West Deputy Commissioner Court",
        "courtLevel": "DISTRICT",
        "courtType": "DC_COURT",
        "unitId": 2,
        "unitName": "Imphal West",
        "unitCode": "IMPWEST",
        "designation": "Deputy Commissioner",
        "address": "Imphal West District Office",
        "contactNumber": "0385-1234567",
        "email": "dc.impwest@manipur.gov.in",
        "stateCode": "MANIPUR",
        "isActive": true
      }
    ],
    "message": "Available courts retrieved successfully"
  }
}
```

**Frontend Usage**:
1. User selects Case Type → Get case natures
2. User selects Case Nature → Call this API with `caseNatureId` and user's `unitId`
3. Show dropdown of available courts
4. User selects Court → Proceed with case filing

### Get Courts by Level (Admin)
**GET** `/api/admin/courts/level/{level}`

**Parameters**:
- `level`: `CIRCLE`, `SUB_DIVISION`, `DISTRICT`, or `STATE`

**Response**: Array of CourtDTO objects

### Create Court (Admin)
**POST** `/api/admin/courts`

**Request Body**:
```json
{
  "courtCode": "DIST_IMPWEST_DC",
  "courtName": "Imphal West Deputy Commissioner Court",
  "courtLevel": "DISTRICT",
  "courtType": "DC_COURT",
  "unitId": 2,
  "designation": "Deputy Commissioner",
  "address": "Imphal West District Office",
  "contactNumber": "0385-1234567",
  "email": "dc.impwest@manipur.gov.in",
  "isActive": true
}
```

**Field Descriptions**:
- `courtLevel`: Must match the unit's level (e.g., DISTRICT level court for DISTRICT unit)
- `courtType`: `SDC_COURT`, `SDO_COURT`, `DC_COURT`, `REVENUE_COURT`, `REVENUE_TRIBUNAL`, or `STATE_TRIBUNAL`

---

## Cases API

### Create Case
**POST** `/api/cases`

**Request Body** (Updated):
```json
{
  "caseTypeId": 1,
  "applicantId": 10,
  "unitId": 4,
  "caseNatureId": 1,
  "courtId": 5,
  "originalOrderLevel": null,
  "subject": "Mutation application for land parcel",
  "description": "Applying for mutation after gift deed",
  "priority": "MEDIUM",
  "applicationDate": "2024-01-15",
  "remarks": "Urgent case",
  "caseData": "{\"deedNumber\":\"12345\",\"deedDate\":\"2024-01-01\"}"
}
```

**New Fields**:
- `caseNatureId` (optional): Case nature ID (New File, Appeal, etc.)
- `courtId` (optional): Court ID where petition is filed
- `originalOrderLevel` (optional): For appeals - level of original order (`CIRCLE`, `SUB_DIVISION`, `DISTRICT`)

**Response** (Updated):
```json
{
  "success": true,
  "message": "Case created successfully",
  "data": {
    "id": 100,
    "caseNumber": "MUT-GIFT-2024-001",
    "caseTypeId": 1,
    "caseTypeName": "Mutation (after Gift/Sale Deeds)",
    "caseTypeCode": "MUTATION_GIFT_SALE",
    "caseNatureId": 1,
    "caseNatureName": "New File",
    "caseNatureCode": "NEW_FILE",
    "courtId": 5,
    "courtName": "Porompat SDC Court",
    "courtCode": "CIRCLE_POROMPAT_SDC",
    "originalOrderLevel": null,
    "applicantId": 10,
    "applicantName": "John Doe",
    "unitId": 4,
    "unitName": "Porompat Circle",
    "subject": "Mutation application for land parcel",
    "status": "SUBMITTED",
    "priority": "MEDIUM",
    "applicationDate": "2024-01-15"
  }
}
```

**New Fields in Response**:
- `caseNatureId`, `caseNatureName`, `caseNatureCode`
- `courtId`, `courtName`, `courtCode`
- `originalOrderLevel`

---

## Frontend Integration Flow

### Complete Petition Filing Flow

1. **Get Case Types**
   ```
   GET /api/case-types/active
   ```
   → Show dropdown of case types

2. **User selects Case Type** → **Get Case Natures**
   ```
   GET /api/public/case-natures/case-type/{caseTypeId}?stateCode={stateCode}
   ```
   → Show dropdown of case natures (New File, Appeal, Revision, etc.)

3. **User selects Case Nature** → **Get Available Courts**
   ```
   GET /api/public/courts/available?caseNatureId={caseNatureId}&unitId={unitId}
   ```
   → Show dropdown of available courts (filtered by case nature requirements and user's unit)

4. **User selects Court** → **Fill other case details** → **Submit Case**
   ```
   POST /api/cases
   {
     "caseTypeId": 1,
     "caseNatureId": 1,
     "courtId": 5,
     "unitId": 4,
     ...
   }
   ```

---

## Court Level and Court Type Enums

### CourtLevel
- `CIRCLE` - Circle level (SDC)
- `SUB_DIVISION` - Sub-Division level (SDO)
- `DISTRICT` - District level (DC, Revenue Court, Revenue Tribunal)
- `STATE` - State level

### CourtType
- `SDC_COURT` - Sub-Divisional Circle Officer Court
- `SDO_COURT` - Sub-Divisional Officer Court
- `DC_COURT` - Deputy Commissioner Court
- `REVENUE_COURT` - Revenue Court
- `REVENUE_TRIBUNAL` - Revenue Tribunal
- `STATE_TRIBUNAL` - State level Tribunal

---

## Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "caseNatureId",
      "message": "Case nature ID is required"
    }
  ],
  "path": "/api/cases"
}
```

---

## Authentication

Most endpoints require JWT authentication. Include token in header:
```
Authorization: Bearer <jwt-token>
```

**Public Endpoints** (no authentication required):
- `GET /api/admin-units/root`
- `GET /api/admin-units/parent/{parentId}`
- `GET /api/case-types/active`
- `GET /api/public/case-natures/case-type/{caseTypeId}`
- `GET /api/public/courts/available`

---

## Summary of Changes for Frontend

### Updated Endpoints:
1. **Case Types API** - Now includes Act information
2. **Cases API** - Now includes CaseNature and Court fields

### New Endpoints:
1. **Acts API** - `/api/admin/acts/*` (Admin only)
2. **Case Natures API** - `/api/admin/case-natures/*` (Admin) and `/api/public/case-natures/*` (Public)
3. **Courts API** - `/api/admin/courts/*` (Admin) and `/api/public/courts/*` (Public)

### Frontend Changes Required:
1. Update Case Type display to show Act information
2. Add Case Nature selection step in petition filing
3. Add Court selection step in petition filing (based on Case Nature)
4. Update Case creation request to include `caseNatureId` and `courtId`
5. Update Case display to show Case Nature and Court information

---

## Example: Complete Petition Filing Request

```json
POST /api/cases
Authorization: Bearer <token>

{
  "caseTypeId": 1,
  "applicantId": 10,
  "unitId": 4,
  "caseNatureId": 2,
  "courtId": 10,
  "originalOrderLevel": "SUB_DIVISION",
  "subject": "Appeal against SDO order",
  "description": "Appealing against Sub-Division Officer's order dated 2024-01-01",
  "priority": "HIGH",
  "applicationDate": "2024-01-15",
  "caseData": "{\"originalCaseNumber\":\"CASE-2024-001\",\"originalOrderDate\":\"2024-01-01\"}"
}
```

---

## Notes

1. **Court Selection Logic**: The system automatically filters courts based on:
   - Case nature's required court level
   - Case nature's allowed court types
   - User's administrative unit hierarchy

3. **Case Nature Types**: Common case natures include:
   - `NEW_FILE` - Original filing
   - `FIRST_APPEAL_*` - First appeal from various levels
   - `SECOND_APPEAL_*` - Second appeal
   - `REVISION` - Revision petition
   - `REVIEW` - Review petition

4. **Backward Compatibility**: Existing APIs remain functional. New fields are optional in requests.

---

For questions or issues, please contact the backend development team.
