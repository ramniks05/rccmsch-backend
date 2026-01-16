# Form Schema API Documentation - Simplified

## Overview

This API provides **simple individual CRUD operations** for managing form fields. Each field is mapped to a case type when created.

**No bulk operations** - Each field is managed independently.

---

## Base URL

```
http://localhost:8080/api/admin/form-schemas
```

---

## Authentication

All endpoints (except GET schema) require Admin authentication:

```
Authorization: Bearer {accessToken}
```

---

## Endpoints

### 1. Get Form Schema

Get all active form fields for a case type.

```
GET /api/admin/form-schemas/case-types/{caseTypeId}
```

**Path Parameters:**
- `caseTypeId` (Long, required) - Case type ID

**Response:**
```json
{
  "success": true,
  "message": "Form schema retrieved successfully",
  "data": {
    "caseTypeId": 1,
    "caseTypeName": "Mutation",
    "caseTypeCode": "MUTATION",
    "fields": [
      {
        "id": 1,
        "caseTypeId": 1,
        "fieldName": "registeredDeedNumber",
        "fieldLabel": "Registered Deed Number",
        "fieldType": "TEXT",
        "isRequired": true,
        "validationRules": "{\"minLength\": 5, \"maxLength\": 50}",
        "displayOrder": 1,
        "isActive": true,
        "version": 5,
        "createdAt": "2024-01-15T10:00:00",
        "updatedAt": "2024-01-20T14:30:00"
      }
    ],
    "totalFields": 10
  }
}
```

---

### 2. Get All Fields (Including Inactive)

Get all fields for a case type, including inactive ones.

```
GET /api/admin/form-schemas/case-types/{caseTypeId}/fields
```

**Authorization:** Admin only

**Path Parameters:**
- `caseTypeId` (Long, required) - Case type ID

**Response:** Same as Get Form Schema, but includes inactive fields

---

### 3. Get Single Field

Get a single field by ID.

```
GET /api/admin/form-schemas/fields/{fieldId}
```

**Authorization:** Admin only

**Path Parameters:**
- `fieldId` (Long, required) - Field ID

**Response:**
```json
{
  "success": true,
  "message": "Field retrieved successfully",
  "data": {
    "id": 1,
    "caseTypeId": 1,
    "fieldName": "registeredDeedNumber",
    "fieldLabel": "Registered Deed Number",
    "fieldType": "TEXT",
    "isRequired": true,
    "version": 5,
    ...
  }
}
```

---

### 4. Create Field

Create a new form field.

```
POST /api/admin/form-schemas/fields
```

**Authorization:** Admin only

**Request Body:**
```json
{
  "caseTypeId": 1,
  "fieldName": "witnessName",
  "fieldLabel": "Witness Name",
  "fieldType": "TEXT",
  "isRequired": false,
  "validationRules": "{\"minLength\": 3, \"maxLength\": 100}",
  "displayOrder": 9,
  "isActive": true,
  "defaultValue": "",
  "placeholder": "Enter witness name",
  "helpText": "Name of the witness",
  "fieldGroup": "witness_info",
  "fieldOptions": null,
  "conditionalLogic": null
}
```

**Required Fields:**
- `caseTypeId` (Long) - Case type ID (field is mapped to this case type)
- `fieldName` (String) - Unique field identifier (e.g., "witnessName")
- `fieldLabel` (String) - Display label (e.g., "Witness Name")
- `fieldType` (String) - Field type: TEXT, NUMBER, DATE, EMAIL, PHONE, TEXTAREA, SELECT, RADIO, CHECKBOX, FILE, DATETIME

**Optional Fields:**
- `isRequired` (Boolean) - Default: false
- `validationRules` (String) - JSON string with validation rules
- `displayOrder` (Integer) - Default: 0
- `isActive` (Boolean) - Default: true
- `defaultValue` (String)
- `placeholder` (String)
- `helpText` (String)
- `fieldGroup` (String) - For grouping fields
- `fieldOptions` (String) - JSON string for SELECT/RADIO options
- `conditionalLogic` (String) - JSON string for conditional display

**Response:**
```json
{
  "success": true,
  "message": "Form field created successfully",
  "data": {
    "id": 11,
    "caseTypeId": 1,
    "fieldName": "witnessName",
    "fieldLabel": "Witness Name",
    "fieldType": "TEXT",
    "version": 0,
    ...
  }
}
```

**Error Responses:**
- `409 Conflict` - Field name already exists for this case type
- `400 Bad Request` - Validation errors

---

### 5. Update Field

Update an existing form field.

```
PUT /api/admin/form-schemas/fields/{fieldId}
```

**Authorization:** Admin only

**Path Parameters:**
- `fieldId` (Long, required) - Field ID

**Request Body:**
```json
{
  "fieldLabel": "Updated Witness Name",
  "isRequired": true,
  "validationRules": "{\"minLength\": 5}",
  "displayOrder": 8,
  "isActive": true,
  "expectedVersion": 5
}
```

**Note:** Include `expectedVersion` to prevent conflicts. If the field was modified by another user, you'll get a 409 Conflict error.

**All fields are optional** - Only include fields you want to update.

**Response:**
```json
{
  "success": true,
  "message": "Form field updated successfully",
  "data": {
    "id": 11,
    "fieldLabel": "Updated Witness Name",
    "version": 6,
    ...
  }
}
```

**Error Responses:**
- `409 Conflict` - Field was modified by another user (version mismatch)
- `404 Not Found` - Field not found
- `400 Bad Request` - Validation errors

---

### 6. Delete Field

Delete a form field.

```
DELETE /api/admin/form-schemas/fields/{fieldId}
```

**Authorization:** Admin only

**Path Parameters:**
- `fieldId` (Long, required) - Field ID

**Response:**
```json
{
  "success": true,
  "message": "Form field deleted successfully",
  "data": null
}
```

**Error Responses:**
- `404 Not Found` - Field not found

---

### 7. Reorder Fields

Update display order of fields.

```
PUT /api/admin/form-schemas/case-types/{caseTypeId}/fields/reorder
```

**Authorization:** Admin only

**Path Parameters:**
- `caseTypeId` (Long, required) - Case type ID

**Request Body:**
```json
{
  "fieldOrders": [
    {"fieldId": 1, "displayOrder": 1},
    {"fieldId": 2, "displayOrder": 2},
    {"fieldId": 3, "displayOrder": 3}
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Fields reordered successfully",
  "data": null
}
```

---

### 8. Validate Form Data

Validate form data against schema.

```
POST /api/admin/form-schemas/validate
```

**Public endpoint** - No authentication required

**Request Body:**
```json
{
  "caseTypeId": 1,
  "formData": {
    "registeredDeedNumber": "DEED123",
    "applicantName": "John Doe",
    "applicantPhone": "9876543210"
  }
}
```

**Response (Valid):**
```json
{
  "success": true,
  "message": "Form data is valid",
  "data": {}
}
```

**Response (Invalid):**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "registeredDeedNumber": "Field is required",
    "applicantPhone": "Invalid phone number format"
  }
}
```

---

## Field Types

| Type | Description | Example |
|------|-------------|---------|
| `TEXT` | Single-line text | Name, Address |
| `NUMBER` | Numeric value | Age, Amount |
| `DATE` | Date only | Birth Date |
| `DATETIME` | Date and time | Appointment Time |
| `EMAIL` | Email address | Email |
| `PHONE` | Phone number | Phone |
| `TEXTAREA` | Multi-line text | Description |
| `SELECT` | Dropdown | Country, State |
| `RADIO` | Radio buttons | Gender |
| `CHECKBOX` | Checkbox | Terms Accepted |
| `FILE` | File upload | Document |

---

## Validation Rules (JSON String)

```json
{
  "minLength": 5,
  "maxLength": 50,
  "min": 0,
  "max": 100,
  "pattern": "^[A-Za-z]+$",
  "minDate": "2024-01-01",
  "maxDate": "2024-12-31"
}
```

---

## Field Options (JSON String) - For SELECT/RADIO

```json
[
  {"value": "SALE", "label": "Sale Deed"},
  {"value": "GIFT", "label": "Gift Deed"},
  {"value": "PARTITION", "label": "Partition Deed"}
]
```

---

## Conditional Logic (JSON String)

```json
{
  "showIf": {
    "field": "deedType",
    "value": "SALE"
  }
}
```

---

## Version Checking (Conflict Prevention)

### How It Works

1. Each field has a `version` number that increments on each update
2. When updating, include `expectedVersion` in the request
3. Backend checks: if current version ≠ expected version → 409 Conflict
4. Frontend should refresh and show conflict message

### Example

```typescript
// Fetch field
GET /api/admin/form-schemas/fields/1
// Response: { "id": 1, "version": 5, ... }

// Update field
PUT /api/admin/form-schemas/fields/1
// Request: { "fieldLabel": "New Label", "expectedVersion": 5 }
// If field was modified → 409 Conflict
// If field not modified → 200 OK, new version: 6
```

---

## Error Codes

| Code | Description |
|------|-------------|
| `200 OK` | Success |
| `201 Created` | Resource created |
| `400 Bad Request` | Validation error |
| `401 Unauthorized` | Missing/invalid token |
| `403 Forbidden` | Insufficient permissions |
| `404 Not Found` | Resource not found |
| `409 Conflict` | Version conflict or duplicate field name |
| `500 Internal Server Error` | Server error |

---

## Complete Example Workflow

### 1. Get Schema
```bash
GET /api/admin/form-schemas/case-types/1
```

### 2. Create Field
```bash
POST /api/admin/form-schemas/fields
{
  "caseTypeId": 1,
  "fieldName": "applicantEmail",
  "fieldLabel": "Applicant Email",
  "fieldType": "EMAIL",
  "isRequired": true,
  "displayOrder": 5
}
```

### 3. Update Field
```bash
PUT /api/admin/form-schemas/fields/11
{
  "fieldLabel": "Email Address",
  "expectedVersion": 0
}
```

### 4. Delete Field
```bash
DELETE /api/admin/form-schemas/fields/11
```

---

## Summary

✅ **Simple**: One operation = One API call  
✅ **Clear**: Each field operation is independent  
✅ **Safe**: Version checking prevents conflicts  
✅ **Mapped**: Field is linked to case type when created  

**No bulk operations** - Keep it simple!

---

**Last Updated**: After simplification to individual CRUD only

