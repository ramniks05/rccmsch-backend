# Registration Form Schema APIs (Citizen + Lawyer)

## Overview

Admin can configure dynamic registration forms for **Citizen** and **Lawyer**.
Frontend loads the schema and renders the form dynamically.

Unit dropdown is driven by **Admin Unit APIs** (hierarchy).

---

## Public API (Frontend)

### 1) Get Registration Form Schema

**Endpoint:** `GET /api/public/registration-forms/{type}`

**Path Params:**
- `type` = `CITIZEN` or `LAWYER`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration form schema retrieved successfully",
  "data": {
    "registrationType": "CITIZEN",
    "fields": [
      {
        "id": 1,
        "registrationType": "CITIZEN",
        "fieldName": "firstName",
        "fieldLabel": "First Name",
        "fieldType": "TEXT",
        "isRequired": true,
        "validationRules": "{\"minLength\":2,\"maxLength\":50}",
        "displayOrder": 1,
        "isActive": true,
        "defaultValue": null,
        "fieldOptions": null,
        "dataSource": null,
        "placeholder": "Enter first name",
        "helpText": null,
        "fieldGroup": "basic",
        "conditionalLogic": null
      },
      {
        "id": 8,
        "registrationType": "CITIZEN",
        "fieldName": "unitId",
        "fieldLabel": "Select Unit",
        "fieldType": "DROPDOWN",
        "isRequired": true,
        "validationRules": null,
        "displayOrder": 8,
        "isActive": true,
        "defaultValue": null,
        "fieldOptions": null,
        "dataSource": "{\"type\":\"ADMIN_UNITS\",\"level\":\"CIRCLE\"}",
        "placeholder": "Select unit",
        "helpText": "Choose the circle where the case will be submitted",
        "fieldGroup": "location",
        "conditionalLogic": null
      }
    ]
  }
}
```

**Frontend Behavior:**
- Render fields in `displayOrder`
- If `fieldType = DROPDOWN` and `dataSource.type = ADMIN_UNITS` → use unit APIs

---

## Admin Unit APIs (Hierarchy Dropdown)

### Get Root Units
```
GET /api/admin-units/root
```

### Get Child Units by Parent
```
GET /api/admin-units/parent/{parentId}
```

**Frontend Flow:**
State → District → Sub Division → Circle  
Use cascading dropdowns to finally store `unitId`

---

## Admin APIs (Schema Management)

All admin APIs require:
```
Authorization: Bearer <adminToken>
Content-Type: application/json
```

### 1) List All Fields (Admin)

**Endpoint:** `GET /api/admin/registration-forms/{type}/fields`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration form fields retrieved successfully",
  "data": [ { ...field objects... } ]
}
```

---

### 1.1) Get Field Group Options (Admin)

Use this to populate **field group dropdown** while creating/editing a field.

**Endpoint:** `GET /api/admin/registration-forms/{type}/field-groups`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration form field groups retrieved successfully",
  "data": [
    {
      "id": 1,
      "registrationType": "CITIZEN",
      "groupCode": "basic",
      "groupLabel": "Basic Info",
      "description": "Basic identity details",
      "displayOrder": 1,
      "isActive": true
    }
  ]
}
```

---

### 1.2) List All Field Groups (Admin)

**Endpoint:** `GET /api/admin/registration-forms/{type}/groups`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration field groups retrieved successfully",
  "data": [ { ...group objects... } ]
}
```

---

### 1.3) Create Field Group (Admin)

**Endpoint:** `POST /api/admin/registration-forms/groups`

**Request Body:**
```json
{
  "registrationType": "LAWYER",
  "groupCode": "location",
  "groupLabel": "Location",
  "description": "Location details",
  "displayOrder": 2,
  "isActive": true
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration field group created successfully",
  "data": {
    "id": 4,
    "registrationType": "LAWYER",
    "groupCode": "location",
    "groupLabel": "Location",
    "description": "Location details",
    "displayOrder": 2,
    "isActive": true
  }
}
```

---

### 1.4) Update Field Group (Admin)

**Endpoint:** `PUT /api/admin/registration-forms/groups/{id}`

**Request Body:** Same as Create

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration field group updated successfully",
  "data": { ...updated group... }
}
```

---

### 1.5) Delete Field Group (Admin)

**Endpoint:** `DELETE /api/admin/registration-forms/groups/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration field group deleted successfully",
  "data": null
}
```

---

### 2) Create Field (Admin)

**Endpoint:** `POST /api/admin/registration-forms/fields`

**Request Body:**
```json
{
  "registrationType": "LAWYER",
  "fieldName": "unitId",
  "fieldLabel": "Select Unit",
  "fieldType": "DROPDOWN",
  "isRequired": true,
  "validationRules": null,
  "displayOrder": 8,
  "isActive": true,
  "defaultValue": null,
  "fieldOptions": null,
  "dataSource": "{\"type\":\"ADMIN_UNITS\",\"level\":\"CIRCLE\"}",
  "placeholder": "Select unit",
  "helpText": "Choose the circle where the case will be submitted",
  "fieldGroup": "location",
  "conditionalLogic": null
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration form field created successfully",
  "data": {
    "id": 12,
    "registrationType": "LAWYER",
    "fieldName": "unitId",
    "fieldLabel": "Select Unit",
    "fieldType": "DROPDOWN",
    "isRequired": true,
    "displayOrder": 8,
    "isActive": true,
    "dataSource": "{\"type\":\"ADMIN_UNITS\",\"level\":\"CIRCLE\"}"
  }
}
```

---

### 3) Update Field (Admin)

**Endpoint:** `PUT /api/admin/registration-forms/fields/{id}`

**Request Body:** Same as Create

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration form field updated successfully",
  "data": { ...updated field... }
}
```

---

### 4) Delete Field (Admin)

**Endpoint:** `DELETE /api/admin/registration-forms/fields/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Registration form field deleted successfully",
  "data": null
}
```

---

## Validation Behavior

During registration, backend checks:
- Required fields (`isRequired = true`)
- Validation rules (`minLength`, `maxLength`, `pattern`)

Dynamic fields can be sent directly in the registration request body as top-level keys.
They will be validated using the admin-configured registration schema.

Dynamic registration fields are stored in database as JSON:
- `citizens.registration_data`
- `lawyers.registration_data`

If validation fails, response:
```json
{
  "timestamp": "2026-01-22T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: firstName is required",
  "path": "/api/auth/citizen/register"
}
```

---

## Notes

- Use **same schema API** for Citizen and Lawyer
- Unit dropdown options are always fetched dynamically via Admin Unit APIs
- Admin can fully control required/optional fields without code change

---

**Last Updated:** After registration form schema implementation
