# Dynamic Form Schema API Documentation

## Overview

The Dynamic Form Schema system allows **admins to create and manage form fields for different case types** without code changes. Forms are rendered dynamically on the frontend based on field definitions stored in the database.

### Key Features

- ✅ **Dynamic Form Fields**: Admin can add/modify/delete form fields for any case type
- ✅ **Field Validation**: Automatic validation based on field type and rules
- ✅ **Field Types**: TEXT, NUMBER, DATE, EMAIL, PHONE, TEXTAREA, SELECT, RADIO, CHECKBOX, FILE
- ✅ **Field Ordering**: Admin can reorder fields
- ✅ **Conditional Fields**: Support for conditional field display (future enhancement)
- ✅ **Field Groups**: Organize fields into groups
- ✅ **Help Text & Placeholders**: User-friendly form guidance

---

## Table of Contents

1. [Base URL](#base-url)
2. [Authentication](#authentication)
3. [Public APIs (Citizen/Frontend)](#public-apis-citizenfrontend)
4. [Admin APIs](#admin-apis)
5. [Field Types & Validation](#field-types--validation)
6. [Frontend Implementation Guide](#frontend-implementation-guide)
7. [Examples](#examples)

---

## Base URL

```
http://localhost:8080
```

---

## Authentication

### For Public APIs (Form Schema)
- **No authentication required** for getting form schema
- Used by citizens to view form structure

### For Admin APIs
- **JWT Token required** in Authorization header
- Only users with `ADMIN` authority can access admin endpoints

```http
Authorization: Bearer <access_token>
```

---

## Public APIs (Citizen/Frontend)

### 1. Get Form Schema for Case Type

Get all active form fields for a case type. Use this to render the form dynamically.

**Endpoint:** `GET /api/admin/form-schemas/case-types/{caseTypeId}`

**Authentication:** Not required

**Path Parameters:**
- `caseTypeId` (Long, required): ID of the case type

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Form schema retrieved successfully",
  "data": {
    "caseTypeId": 1,
    "caseTypeName": "Mutation (after Gift/Sale Deeds)",
    "caseTypeCode": "MUTATION_GIFT_SALE",
    "totalFields": 8,
    "fields": [
      {
        "id": 1,
        "caseTypeId": 1,
        "caseTypeName": "Mutation (after Gift/Sale Deeds)",
        "caseTypeCode": "MUTATION_GIFT_SALE",
        "fieldName": "registeredDeedNumber",
        "fieldLabel": "Registered Deed Number",
        "fieldType": "TEXT",
        "isRequired": true,
        "validationRules": "{\"minLength\": 5, \"maxLength\": 50, \"pattern\": \"^[A-Z0-9/-]+$\"}",
        "displayOrder": 1,
        "isActive": true,
        "defaultValue": null,
        "fieldOptions": null,
        "placeholder": "Enter the registered deed number",
        "helpText": "Deed number as per registration certificate",
        "fieldGroup": null,
        "conditionalLogic": null,
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00"
      },
      {
        "id": 2,
        "fieldName": "deedRegistrationDate",
        "fieldLabel": "Deed Registration Date",
        "fieldType": "DATE",
        "isRequired": true,
        "validationRules": "{\"maxDate\": \"today\"}",
        "displayOrder": 2,
        "placeholder": "Select registration date",
        "helpText": "Date when the deed was registered"
      }
    ]
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

**Frontend Usage:**
```typescript
// Angular example
getFormSchema(caseTypeId: number): Observable<FormSchemaDTO> {
  return this.http.get<ApiResponse<FormSchemaDTO>>(
    `${this.baseUrl}/api/admin/form-schemas/case-types/${caseTypeId}`
  ).pipe(
    map(response => response.data)
  );
}
```

---

### 2. Validate Form Data

Validate form data before submitting. This is optional but recommended for better UX.

**Endpoint:** `POST /api/admin/form-schemas/validate`

**Authentication:** Not required

**Request Body:**
```json
{
  "caseTypeId": 1,
  "formData": {
    "registeredDeedNumber": "DEED-12345",
    "deedRegistrationDate": "2024-01-15",
    "deedType": "SALE",
    "sellerName": "John Doe",
    "buyerName": "Jane Smith",
    "landDetails": "Patta No: 123, Dag No: 456",
    "subRegistrarOffice": "Imphal East Sub-Registrar",
    "deedCopy": "base64_encoded_file_or_file_id"
  }
}
```

**Response (200 OK) - Valid:**
```json
{
  "success": true,
  "message": "Form data is valid",
  "data": {},
  "timestamp": "2024-01-01T10:00:00"
}
```

**Response (400 Bad Request) - Invalid:**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "registeredDeedNumber": "Minimum length is 5 characters",
    "deedRegistrationDate": "Date must be on or before today"
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

---

## Admin APIs

### 1. Get All Fields for Case Type (Including Inactive)

Get all form fields for a case type, including inactive ones.

**Endpoint:** `GET /api/admin/form-schemas/case-types/{caseTypeId}/fields`

**Authentication:** Required (ADMIN)

**Path Parameters:**
- `caseTypeId` (Long, required): ID of the case type

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fields retrieved successfully",
  "data": [
    {
      "id": 1,
      "fieldName": "registeredDeedNumber",
      "fieldLabel": "Registered Deed Number",
      "fieldType": "TEXT",
      "isRequired": true,
      "isActive": true,
      "displayOrder": 1
    }
  ]
}
```

---

### 2. Get Form Field by ID

Get details of a specific form field.

**Endpoint:** `GET /api/admin/form-schemas/fields/{fieldId}`

**Authentication:** Required (ADMIN)

**Path Parameters:**
- `fieldId` (Long, required): ID of the form field

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Field retrieved successfully",
  "data": {
    "id": 1,
    "fieldName": "registeredDeedNumber",
    "fieldLabel": "Registered Deed Number",
    "fieldType": "TEXT",
    "isRequired": true,
    "validationRules": "{\"minLength\": 5, \"maxLength\": 50}",
    "displayOrder": 1,
    "isActive": true,
    "placeholder": "Enter the registered deed number",
    "helpText": "Deed number as per registration certificate"
  }
}
```

---

### 3. Create Form Field

Create a new form field for a case type.

**Endpoint:** `POST /api/admin/form-schemas/fields`

**Authentication:** Required (ADMIN)

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
  "placeholder": "Enter witness name",
  "helpText": "Name of the witness if any",
  "fieldGroup": "additional_details"
}
```

**Field Types:**
- `TEXT` - Single line text input
- `NUMBER` - Numeric input
- `DATE` - Date picker
- `DATETIME` - Date and time picker
- `EMAIL` - Email input
- `PHONE` - Phone number input
- `TEXTAREA` - Multi-line text input
- `SELECT` - Dropdown select
- `RADIO` - Radio buttons
- `CHECKBOX` - Checkbox
- `FILE` - File upload

**Validation Rules (JSON string):**
```json
{
  "minLength": 5,           // For TEXT, TEXTAREA
  "maxLength": 50,          // For TEXT, TEXTAREA
  "pattern": "^[A-Z0-9]+$", // Regex pattern
  "min": 0,                 // For NUMBER
  "max": 10000,             // For NUMBER
  "minDate": "1900-01-01",  // For DATE
  "maxDate": "today"        // For DATE (special value)
}
```

**Field Options (for SELECT/RADIO - JSON string):**
```json
[
  {"value": "SALE", "label": "Sale Deed"},
  {"value": "GIFT", "label": "Gift Deed"}
]
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Form field created successfully",
  "data": {
    "id": 10,
    "fieldName": "witnessName",
    "fieldLabel": "Witness Name",
    "fieldType": "TEXT",
    "isRequired": false,
    "displayOrder": 9,
    "isActive": true
  }
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

### 4. Update Form Field

Update an existing form field.

**Endpoint:** `PUT /api/admin/form-schemas/fields/{fieldId}`

**Authentication:** Required (ADMIN)

**Path Parameters:**
- `fieldId` (Long, required): ID of the form field

**Request Body (all fields optional):**
```json
{
  "fieldLabel": "Updated Field Label",
  "fieldType": "TEXTAREA",
  "isRequired": true,
  "validationRules": "{\"minLength\": 10, \"maxLength\": 500}",
  "displayOrder": 5,
  "isActive": true,
  "placeholder": "Updated placeholder",
  "helpText": "Updated help text"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Form field updated successfully",
  "data": {
    "id": 1,
    "fieldLabel": "Updated Field Label",
    "fieldType": "TEXTAREA",
    "isRequired": true
  }
}
```

---

### 5. Delete Form Field

Delete a form field (permanent deletion).

**Endpoint:** `DELETE /api/admin/form-schemas/fields/{fieldId}`

**Authentication:** Required (ADMIN)

**Path Parameters:**
- `fieldId` (Long, required): ID of the form field

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Form field deleted successfully",
  "data": null
}
```

---

### 6. Reorder Form Fields

Change the display order of form fields.

**Endpoint:** `PUT /api/admin/form-schemas/case-types/{caseTypeId}/fields/reorder`

**Authentication:** Required (ADMIN)

**Path Parameters:**
- `caseTypeId` (Long, required): ID of the case type

**Request Body:**
```json
{
  "fieldIds": [3, 1, 2, 5, 4]
}
```

The order of IDs in the array determines the new display order.

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fields reordered successfully",
  "data": null
}
```

---

## Field Types & Validation

### Field Types

| Type | Description | HTML Input Type | Validation |
|------|-------------|----------------|-------------|
| `TEXT` | Single line text | `text` | minLength, maxLength, pattern |
| `NUMBER` | Numeric value | `number` | min, max |
| `DATE` | Date picker | `date` | minDate, maxDate |
| `DATETIME` | Date and time | `datetime-local` | minDate, maxDate |
| `EMAIL` | Email address | `email` | Email format |
| `PHONE` | Phone number | `tel` | 10 digits, starts with 6-9 |
| `TEXTAREA` | Multi-line text | `textarea` | minLength, maxLength |
| `SELECT` | Dropdown | `select` | Must match fieldOptions |
| `RADIO` | Radio buttons | `radio` | Must match fieldOptions |
| `CHECKBOX` | Checkbox | `checkbox` | Boolean value |
| `FILE` | File upload | `file` | File size/type (future) |

### Validation Rules

**TEXT/TEXTAREA:**
```json
{
  "minLength": 5,
  "maxLength": 100,
  "pattern": "^[A-Za-z0-9 ]+$"
}
```

**NUMBER:**
```json
{
  "min": 0,
  "max": 10000
}
```

**DATE:**
```json
{
  "minDate": "1900-01-01",
  "maxDate": "today"
}
```

**EMAIL:**
- Automatically validates email format
- No additional rules needed

**PHONE:**
- Automatically validates 10-digit Indian phone number
- Must start with 6-9

**SELECT/RADIO:**
- Value must match one of the options in `fieldOptions`
- `fieldOptions` format:
```json
[
  {"value": "OPTION1", "label": "Option 1"},
  {"value": "OPTION2", "label": "Option 2"}
]
```

---

## Frontend Implementation Guide

### Step 1: Get Form Schema

When user selects a case type, fetch the form schema:

```typescript
// Angular Service
@Injectable({ providedIn: 'root' })
export class FormSchemaService {
  private baseUrl = 'http://localhost:8080';

  getFormSchema(caseTypeId: number): Observable<FormSchemaDTO> {
    return this.http.get<ApiResponse<FormSchemaDTO>>(
      `${this.baseUrl}/api/admin/form-schemas/case-types/${caseTypeId}`
    ).pipe(
      map(response => response.data)
    );
  }
}
```

### Step 2: Render Form Dynamically

```typescript
// Angular Component
export class CreateCaseComponent {
  formSchema: FormSchemaDTO | null = null;
  formData: { [key: string]: any } = {};

  constructor(private formSchemaService: FormSchemaService) {}

  ngOnInit() {
    const caseTypeId = this.route.snapshot.params['caseTypeId'];
    this.loadFormSchema(caseTypeId);
  }

  loadFormSchema(caseTypeId: number) {
    this.formSchemaService.getFormSchema(caseTypeId).subscribe({
      next: (schema) => {
        this.formSchema = schema;
        this.initializeFormData(schema);
      },
      error: (error) => {
        console.error('Error loading form schema:', error);
      }
    });
  }

  initializeFormData(schema: FormSchemaDTO) {
    schema.fields.forEach(field => {
      if (field.defaultValue) {
        this.formData[field.fieldName] = field.defaultValue;
      } else {
        this.formData[field.fieldName] = '';
      }
    });
  }
}
```

### Step 3: Template - Dynamic Form Rendering

```html
<!-- Angular Template -->
<form [formGroup]="caseForm" (ngSubmit)="onSubmit()">
  <div *ngFor="let field of formSchema?.fields" 
       [ngSwitch]="field.fieldType"
       class="form-group">
    
    <!-- TEXT Input -->
    <div *ngSwitchCase="'TEXT'">
      <label [for]="field.fieldName">
        {{ field.fieldLabel }}
        <span *ngIf="field.isRequired" class="required">*</span>
      </label>
      <input 
        type="text"
        [id]="field.fieldName"
        [formControlName]="field.fieldName"
        [placeholder]="field.placeholder"
        [required]="field.isRequired"
        class="form-control">
      <small *ngIf="field.helpText" class="form-text text-muted">
        {{ field.helpText }}
      </small>
    </div>

    <!-- NUMBER Input -->
    <div *ngSwitchCase="'NUMBER'">
      <label [for]="field.fieldName">
        {{ field.fieldLabel }}
        <span *ngIf="field.isRequired" class="required">*</span>
      </label>
      <input 
        type="number"
        [id]="field.fieldName"
        [formControlName]="field.fieldName"
        [placeholder]="field.placeholder"
        [required]="field.isRequired"
        class="form-control">
    </div>

    <!-- DATE Input -->
    <div *ngSwitchCase="'DATE'">
      <label [for]="field.fieldName">
        {{ field.fieldLabel }}
        <span *ngIf="field.isRequired" class="required">*</span>
      </label>
      <input 
        type="date"
        [id]="field.fieldName"
        [formControlName]="field.fieldName"
        [required]="field.isRequired"
        class="form-control">
    </div>

    <!-- TEXTAREA -->
    <div *ngSwitchCase="'TEXTAREA'">
      <label [for]="field.fieldName">
        {{ field.fieldLabel }}
        <span *ngIf="field.isRequired" class="required">*</span>
      </label>
      <textarea 
        [id]="field.fieldName"
        [formControlName]="field.fieldName"
        [placeholder]="field.placeholder"
        [required]="field.isRequired"
        rows="4"
        class="form-control"></textarea>
    </div>

    <!-- SELECT Dropdown -->
    <div *ngSwitchCase="'SELECT'">
      <label [for]="field.fieldName">
        {{ field.fieldLabel }}
        <span *ngIf="field.isRequired" class="required">*</span>
      </label>
      <select 
        [id]="field.fieldName"
        [formControlName]="field.fieldName"
        [required]="field.isRequired"
        class="form-control">
        <option value="">-- Select --</option>
        <option *ngFor="let option of getFieldOptions(field)" 
                [value]="option.value">
          {{ option.label }}
        </option>
      </select>
    </div>

    <!-- FILE Upload -->
    <div *ngSwitchCase="'FILE'">
      <label [for]="field.fieldName">
        {{ field.fieldLabel }}
        <span *ngIf="field.isRequired" class="required">*</span>
      </label>
      <input 
        type="file"
        [id]="field.fieldName"
        (change)="onFileChange($event, field.fieldName)"
        [required]="field.isRequired"
        class="form-control">
    </div>

  </div>

  <button type="submit" [disabled]="caseForm.invalid" class="btn btn-primary">
    Submit Case
  </button>
</form>
```

### Step 4: Helper Methods

```typescript
// Component methods
getFieldOptions(field: FormFieldDefinitionDTO): any[] {
  if (!field.fieldOptions) return [];
  try {
    return JSON.parse(field.fieldOptions);
  } catch (e) {
    return [];
  }
}

onFileChange(event: any, fieldName: string) {
  const file = event.target.files[0];
  if (file) {
    // Convert to base64 or upload to file server
    const reader = new FileReader();
    reader.onload = () => {
      this.formData[fieldName] = reader.result; // base64 string
    };
    reader.readAsDataURL(file);
  }
}

onSubmit() {
  if (this.caseForm.valid) {
    const formData = this.caseForm.value;
    
    // Convert form data to JSON string for caseData
    const caseDataJson = JSON.stringify(formData);
    
    const createCaseDTO: CreateCaseDTO = {
      caseTypeId: this.selectedCaseTypeId,
      applicantId: this.currentUserId,
      unitId: this.selectedUnitId,
      subject: formData.subject || 'Case Application',
      description: formData.description || '',
      caseData: caseDataJson, // JSON string of all form fields
      priority: 'MEDIUM',
      applicationDate: new Date()
    };

    // Submit case
    this.caseService.createCase(createCaseDTO).subscribe({
      next: (response) => {
        console.log('Case created:', response);
        this.router.navigate(['/cases', response.data.id]);
      },
      error: (error) => {
        console.error('Error creating case:', error);
      }
    });
  }
}
```

### Step 5: Client-Side Validation (Optional)

You can implement client-side validation based on validation rules:

```typescript
applyValidationRules(field: FormFieldDefinitionDTO, control: FormControl) {
  const validators: any[] = [];

  if (field.isRequired) {
    validators.push(Validators.required);
  }

  if (field.validationRules) {
    try {
      const rules = JSON.parse(field.validationRules);

      switch (field.fieldType) {
        case 'TEXT':
        case 'TEXTAREA':
          if (rules.minLength) {
            validators.push(Validators.minLength(rules.minLength));
          }
          if (rules.maxLength) {
            validators.push(Validators.maxLength(rules.maxLength));
          }
          if (rules.pattern) {
            validators.push(Validators.pattern(rules.pattern));
          }
          break;

        case 'NUMBER':
          if (rules.min !== undefined) {
            validators.push(Validators.min(rules.min));
          }
          if (rules.max !== undefined) {
            validators.push(Validators.max(rules.max));
          }
          break;

        case 'EMAIL':
          validators.push(Validators.email);
          break;

        case 'PHONE':
          validators.push(Validators.pattern(/^[6-9]\d{9}$/));
          break;
      }
    } catch (e) {
      console.error('Error parsing validation rules:', e);
    }
  }

  control.setValidators(validators);
  control.updateValueAndValidity();
}
```

---

## Examples

### Example 1: Create Case with Dynamic Form

**1. Get Form Schema:**
```http
GET /api/admin/form-schemas/case-types/1
```

**2. User fills form with fields:**
- registeredDeedNumber: "DEED-12345"
- deedRegistrationDate: "2024-01-15"
- deedType: "SALE"
- sellerName: "John Doe"
- buyerName: "Jane Smith"
- landDetails: "Patta No: 123, Dag No: 456"
- subRegistrarOffice: "Imphal East"
- deedCopy: (file uploaded)

**3. Submit Case:**
```http
POST /api/cases
Content-Type: application/json
Authorization: Bearer <token>

{
  "caseTypeId": 1,
  "applicantId": 10,
  "unitId": 5,
  "subject": "Mutation Application - DEED-12345",
  "description": "Application for mutation after sale deed",
  "caseData": "{\"registeredDeedNumber\":\"DEED-12345\",\"deedRegistrationDate\":\"2024-01-15\",\"deedType\":\"SALE\",\"sellerName\":\"John Doe\",\"buyerName\":\"Jane Smith\",\"landDetails\":\"Patta No: 123, Dag No: 456\",\"subRegistrarOffice\":\"Imphal East\",\"deedCopy\":\"base64_encoded_file\"}",
  "priority": "MEDIUM",
  "applicationDate": "2024-01-20"
}
```

**4. Backend validates `caseData` against form schema automatically**

---

### Example 2: Admin Adds New Field

**Admin wants to add "Witness Name" field to Mutation (Gift/Sale) case type:**

```http
POST /api/admin/form-schemas/fields
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "caseTypeId": 1,
  "fieldName": "witnessName",
  "fieldLabel": "Witness Name",
  "fieldType": "TEXT",
  "isRequired": false,
  "validationRules": "{\"minLength\": 3, \"maxLength\": 100}",
  "displayOrder": 9,
  "isActive": true,
  "placeholder": "Enter witness name if any",
  "helpText": "Name of witness who signed the deed"
}
```

**Result:**
- Field is immediately available in form schema
- Frontend will show this field on next form load
- No code deployment needed!

---

### Example 3: Admin Reorders Fields

**Admin wants to move "Deed Registration Date" before "Registered Deed Number":**

```http
PUT /api/admin/form-schemas/case-types/1/fields/reorder
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "fieldIds": [2, 1, 3, 4, 5, 6, 7, 8]
}
```

**Result:**
- Field order updated immediately
- Frontend will show fields in new order on next form load

---

## Error Handling

### Common Errors

**1. Case Type Not Found:**
```json
{
  "success": false,
  "message": "Case type not found: 999"
}
```

**2. Field Name Already Exists:**
```json
{
  "success": false,
  "message": "Field name already exists for this case type: registeredDeedNumber"
}
```

**3. Validation Failed:**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "registeredDeedNumber": "Minimum length is 5 characters",
    "deedRegistrationDate": "Date must be on or before today"
  }
}
```

**4. Unauthorized (Admin APIs):**
```json
{
  "success": false,
  "message": "Access denied. Admin authority required."
}
```

---

## Best Practices

1. **Cache Form Schema**: Cache form schema on frontend to reduce API calls
2. **Validate Before Submit**: Use validation API before submitting case
3. **Handle File Uploads**: Convert files to base64 or upload to file server first
4. **Error Messages**: Display field-specific error messages from validation
5. **Loading States**: Show loading indicators while fetching form schema
6. **Field Groups**: Use `fieldGroup` to organize related fields visually
7. **Conditional Logic**: Implement conditional field display based on `conditionalLogic` (future enhancement)

---

## Summary

The Dynamic Form Schema system provides:

- ✅ **Flexibility**: Admin can manage forms without code changes
- ✅ **Type Safety**: Validation based on field definitions
- ✅ **User Experience**: Dynamic forms adapt to case type
- ✅ **Maintainability**: Centralized form management
- ✅ **Scalability**: Easy to add new case types and fields

**Next Steps:**
1. Implement frontend form rendering based on schema
2. Add file upload handling
3. Implement conditional field logic
4. Add field groups UI
5. Create admin UI for form field management

---

**Last Updated:** 2024-01-01

