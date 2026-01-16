# Bulk Form Field Operations API Documentation

## Overview

Complete API documentation for bulk CRUD operations on form fields. These endpoints allow admins to create, update, or delete multiple form fields in a single request.

---

## Table of Contents

1. [Bulk Create Fields](#bulk-create-fields)
2. [Bulk Update Fields](#bulk-update-fields)
3. [Bulk Delete Fields](#bulk-delete-fields)
4. [Complete API Summary](#complete-api-summary)
5. [Examples](#examples)

---

## Bulk Create Fields

Create multiple form fields in a single request.

**Endpoint:** `POST /api/admin/form-schemas/fields/bulk`

**Authentication:** Required (ADMIN)

**Request Body:**
```json
{
  "caseTypeId": 1,
  "fields": [
    {
      "fieldName": "witnessName",
      "fieldLabel": "Witness Name",
      "fieldType": "TEXT",
      "isRequired": false,
      "validationRules": "{\"minLength\": 3, \"maxLength\": 100}",
      "displayOrder": 9,
      "isActive": true,
      "placeholder": "Enter witness name",
      "helpText": "Name of witness if any"
    },
    {
      "fieldName": "witnessAddress",
      "fieldLabel": "Witness Address",
      "fieldType": "TEXTAREA",
      "isRequired": false,
      "validationRules": "{\"minLength\": 10, \"maxLength\": 500}",
      "displayOrder": 10,
      "isActive": true,
      "placeholder": "Enter witness address"
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Fields created successfully",
  "data": [
    {
      "id": 9,
      "fieldName": "witnessName",
      "fieldLabel": "Witness Name",
      "fieldType": "TEXT",
      "isRequired": false,
      "displayOrder": 9,
      "isActive": true
    },
    {
      "id": 10,
      "fieldName": "witnessAddress",
      "fieldLabel": "Witness Address",
      "fieldType": "TEXTAREA",
      "isRequired": false,
      "displayOrder": 10,
      "isActive": true
    }
  ]
}
```

**Error (400 Bad Request):**
```json
{
  "success": false,
  "message": "Field name already exists for this case type: witnessName"
}
```

---

## Bulk Update Fields

Update multiple form fields in a single request.

**Endpoint:** `PUT /api/admin/form-schemas/fields/bulk`

**Authentication:** Required (ADMIN)

**Request Body:**
```json
{
  "fields": [
    {
      "fieldId": 1,
      "updateData": {
        "fieldLabel": "Updated Deed Number",
        "isRequired": false,
        "displayOrder": 5
      }
    },
    {
      "fieldId": 2,
      "updateData": {
        "fieldLabel": "Updated Registration Date",
        "validationRules": "{\"maxDate\": \"today\", \"minDate\": \"1900-01-01\"}",
        "helpText": "Updated help text"
      }
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fields updated successfully",
  "data": [
    {
      "id": 1,
      "fieldName": "registeredDeedNumber",
      "fieldLabel": "Updated Deed Number",
      "fieldType": "TEXT",
      "isRequired": false,
      "displayOrder": 5
    },
    {
      "id": 2,
      "fieldName": "deedRegistrationDate",
      "fieldLabel": "Updated Registration Date",
      "fieldType": "DATE",
      "isRequired": true,
      "validationRules": "{\"maxDate\": \"today\", \"minDate\": \"1900-01-01\"}"
    }
  ]
}
```

**Error (400 Bad Request):**
```json
{
  "success": false,
  "message": "Form field not found: 999"
}
```

---

## Bulk Delete Fields

Delete multiple form fields in a single request.

**Endpoint:** `DELETE /api/admin/form-schemas/fields/bulk`

**Authentication:** Required (ADMIN)

**Request Body:**
```json
{
  "fieldIds": [9, 10, 11, 12, 13]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fields deleted successfully",
  "data": {
    "message": "Fields deleted successfully",
    "deletedCount": 5
  }
}
```

**Note:** If some fields cannot be deleted (e.g., don't exist), the operation will still succeed for valid field IDs and log warnings for failed deletions.

---

## Complete API Summary

### Single Field Operations

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/admin/form-schemas/fields/{fieldId}` | Get single field | ✅ Admin |
| POST | `/api/admin/form-schemas/fields` | Create single field | ✅ Admin |
| PUT | `/api/admin/form-schemas/fields/{fieldId}` | Update single field | ✅ Admin |
| DELETE | `/api/admin/form-schemas/fields/{fieldId}` | Delete single field | ✅ Admin |

### Bulk Field Operations

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/admin/form-schemas/fields/bulk` | Bulk create fields | ✅ Admin |
| PUT | `/api/admin/form-schemas/fields/bulk` | Bulk update fields | ✅ Admin |
| DELETE | `/api/admin/form-schemas/fields/bulk` | Bulk delete fields | ✅ Admin |

### Schema Operations

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/admin/form-schemas/case-types/{caseTypeId}` | Get form schema | ❌ Public |
| GET | `/api/admin/form-schemas/case-types/{caseTypeId}/fields` | Get all fields | ✅ Admin |
| PUT | `/api/admin/form-schemas/case-types/{caseTypeId}` | Bulk update schema | ✅ Admin |
| PUT | `/api/admin/form-schemas/case-types/{caseTypeId}/fields/reorder` | Reorder fields | ✅ Admin |
| POST | `/api/admin/form-schemas/validate` | Validate form data | ❌ Public |

---

## Examples

### Example 1: Bulk Create Multiple Fields

**Scenario:** Admin wants to add 3 new fields to a case type.

```typescript
// Angular/TypeScript
const bulkCreateFields = async () => {
  const response = await http.post(
    'http://localhost:8080/api/admin/form-schemas/fields/bulk',
    {
      caseTypeId: 1,
      fields: [
        {
          fieldName: 'witnessName',
          fieldLabel: 'Witness Name',
          fieldType: 'TEXT',
          isRequired: false,
          displayOrder: 9,
          isActive: true
        },
        {
          fieldName: 'witnessPhone',
          fieldLabel: 'Witness Phone',
          fieldType: 'PHONE',
          isRequired: false,
          displayOrder: 10,
          isActive: true
        },
        {
          fieldName: 'additionalNotes',
          fieldLabel: 'Additional Notes',
          fieldType: 'TEXTAREA',
          isRequired: false,
          displayOrder: 11,
          isActive: true
        }
      ]
    },
    {
      headers: {
        'Authorization': `Bearer ${adminToken}`
      }
    }
  );
  return response.data;
};
```

### Example 2: Bulk Update Multiple Fields

**Scenario:** Admin wants to update labels and validation rules for multiple fields.

```typescript
const bulkUpdateFields = async () => {
  const response = await http.put(
    'http://localhost:8080/api/admin/form-schemas/fields/bulk',
    {
      fields: [
        {
          fieldId: 1,
          updateData: {
            fieldLabel: 'Registered Deed Number (Updated)',
            isRequired: true,
            validationRules: '{"minLength": 5, "maxLength": 50, "pattern": "^[A-Z0-9/-]+$"}'
          }
        },
        {
          fieldId: 2,
          updateData: {
            fieldLabel: 'Deed Registration Date (Updated)',
            helpText: 'Please select the date when deed was registered'
          }
        }
      ]
    },
    {
      headers: {
        'Authorization': `Bearer ${adminToken}`
      }
    }
  );
  return response.data;
};
```

### Example 3: Bulk Delete Multiple Fields

**Scenario:** Admin wants to remove multiple obsolete fields.

```typescript
const bulkDeleteFields = async () => {
  const response = await http.delete(
    'http://localhost:8080/api/admin/form-schemas/fields/bulk',
    {
      data: {
        fieldIds: [9, 10, 11, 12]
      },
      headers: {
        'Authorization': `Bearer ${adminToken}`
      }
    }
  );
  return response.data;
};
```

### Example 4: Complete Workflow - Create, Update, Delete

```typescript
// Complete workflow example
const manageFormFields = async (caseTypeId: number) => {
  // 1. Create new fields
  const created = await bulkCreateFields(caseTypeId, [
    { fieldName: 'field1', fieldLabel: 'Field 1', fieldType: 'TEXT', ... },
    { fieldName: 'field2', fieldLabel: 'Field 2', fieldType: 'NUMBER', ... }
  ]);

  // 2. Update existing fields
  const updated = await bulkUpdateFields([
    { fieldId: 1, updateData: { fieldLabel: 'Updated Label' } },
    { fieldId: 2, updateData: { isRequired: true } }
  ]);

  // 3. Delete unwanted fields
  await bulkDeleteFields([9, 10]);

  return { created, updated };
};
```

---

## Error Handling

### Common Errors

**1. Field Name Already Exists (Bulk Create)**
```json
{
  "success": false,
  "message": "Field name already exists for this case type: witnessName"
}
```

**2. Field Not Found (Bulk Update/Delete)**
```json
{
  "success": false,
  "message": "Form field not found: 999"
}
```

**3. Case Type Not Found (Bulk Create)**
```json
{
  "success": false,
  "message": "Case type not found: 999"
}
```

**4. Validation Error**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "fields[0].fieldName",
      "message": "Field name is required"
    }
  ]
}
```

---

## Best Practices

1. **Batch Size**: Limit bulk operations to 50-100 fields per request for better performance
2. **Error Handling**: Handle partial failures gracefully (some fields succeed, some fail)
3. **Validation**: Validate all fields before sending bulk request
4. **Transaction**: Bulk operations are transactional - all succeed or all fail (except bulk delete which is more lenient)
5. **Ordering**: Use `displayOrder` to control field sequence
6. **Testing**: Test with small batches first before bulk operations

---

## Performance Considerations

- **Bulk Create**: Creates fields sequentially (safe but slower for large batches)
- **Bulk Update**: Updates fields sequentially (safe but slower for large batches)
- **Bulk Delete**: Deletes fields sequentially, continues on errors (more resilient)
- **Recommendation**: For 100+ fields, consider splitting into multiple smaller batches

---

## Summary

✅ **Bulk Create**: Create multiple fields in one request  
✅ **Bulk Update**: Update multiple fields in one request  
✅ **Bulk Delete**: Delete multiple fields in one request  
✅ **All operations require ADMIN authentication**  
✅ **Comprehensive error handling and validation**

All bulk operations are now available and ready to use!

---

**Last Updated**: After bulk operations implementation

