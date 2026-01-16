# Form Schema Management - Simple Guide

## Overview

This guide explains how to manage form fields using **individual CRUD operations only**. Each field operation is independent and mapped to a case type when created.

---

## Table of Contents

1. [API Endpoints](#api-endpoints)
2. [Basic Operations](#basic-operations)
3. [Frontend Implementation](#frontend-implementation)
4. [Best Practices](#best-practices)

---

## API Endpoints

### Get Form Schema (Read-Only)
```
GET /api/admin/form-schemas/case-types/{caseTypeId}
```
Returns all active form fields for a case type.

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
        "fieldName": "registeredDeedNumber",
        "fieldLabel": "Registered Deed Number",
        "fieldType": "TEXT",
        "isRequired": true,
        ...
      }
    ],
    "totalFields": 10
  }
}
```

---

### Create Field
```
POST /api/admin/form-schemas/fields
```

**Request Body:**
```json
{
  "caseTypeId": 1,
  "fieldName": "witnessName",
  "fieldLabel": "Witness Name",
  "fieldType": "TEXT",
  "isRequired": false,
  "displayOrder": 9,
  "isActive": true,
  "placeholder": "Enter witness name",
  "helpText": "Name of the witness"
}
```

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
    ...
  }
}
```

---

### Update Field
```
PUT /api/admin/form-schemas/fields/{fieldId}
```

**Request Body:**
```json
{
  "fieldLabel": "Updated Witness Name",
  "isRequired": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Form field updated successfully",
  "data": {
    "id": 11,
    "fieldLabel": "Updated Witness Name",
    ...
  }
}
```

---

### Delete Field
```
DELETE /api/admin/form-schemas/fields/{fieldId}
```

**Response:**
```json
{
  "success": true,
  "message": "Form field deleted successfully",
  "data": null
}
```

---

### Get Single Field
```
GET /api/admin/form-schemas/fields/{fieldId}
```

---

### Get All Fields (Including Inactive)
```
GET /api/admin/form-schemas/case-types/{caseTypeId}/fields
```

---

## Basic Operations

### 1. Create a New Field

```typescript
// Angular Service
createField(field: CreateFormFieldDTO): Observable<FormFieldDefinitionDTO> {
  return this.http.post<ApiResponse<FormFieldDefinitionDTO>>(
    `${this.baseUrl}/api/admin/form-schemas/fields`,
    field,
    { headers: this.getAuthHeaders() }
  ).pipe(map(response => response.data));
}

// Usage
const newField = {
  caseTypeId: 1,
  fieldName: 'applicantPhone',
  fieldLabel: 'Applicant Phone',
  fieldType: 'PHONE',
  isRequired: true,
  displayOrder: 5,
  isActive: true
};

this.formSchemaService.createField(newField).subscribe({
  next: (created) => {
    console.log('Field created:', created);
    this.refreshSchema(); // Reload schema to see new field
  },
  error: (error) => {
    if (error.status === 409) {
      alert('Field name already exists for this case type');
    } else {
      alert('Failed to create field: ' + error.message);
    }
  }
});
```

---

### 2. Update a Field

```typescript
// Angular Service
  updateField(fieldId: number, updates: UpdateFormFieldDTO): Observable<FormFieldDefinitionDTO> {
  return this.http.put<ApiResponse<FormFieldDefinitionDTO>>(
    `${this.baseUrl}/api/admin/form-schemas/fields/${fieldId}`,
    updates,
    { headers: this.getAuthHeaders() }
  ).pipe(map(response => response.data));
}

// Usage
updateField(field: FormFieldDefinitionDTO, newLabel: string) {
  const updates = {
    fieldLabel: newLabel
  };

  this.formSchemaService.updateField(field.id!, updates).subscribe({
    next: (updated) => {
      console.log('Field updated:', updated);
      // Update local field
      this.refreshSchema();
    },
    error: (error) => {
      alert('Update failed: ' + error.message);
    }
  });
}
```

---

### 3. Delete a Field

```typescript
// Angular Service
deleteField(fieldId: number): Observable<void> {
  return this.http.delete<ApiResponse<void>>(
    `${this.baseUrl}/api/admin/form-schemas/fields/${fieldId}`,
    { headers: this.getAuthHeaders() }
  ).pipe(map(() => undefined));
}

// Usage
deleteField(fieldId: number) {
  if (confirm('Are you sure you want to delete this field?')) {
    this.formSchemaService.deleteField(fieldId).subscribe({
      next: () => {
        console.log('Field deleted');
        this.refreshSchema();
      },
      error: (error) => {
        alert('Delete failed: ' + error.message);
      }
    });
  }
}
```

---

---

## Frontend Implementation

### Complete Angular Service

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface FormFieldDefinition {
  id?: number;
  caseTypeId: number;
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  isRequired: boolean;
  displayOrder: number;
  isActive: boolean;
  // ... other fields
}

export interface FormSchema {
  caseTypeId: number;
  caseTypeName: string;
  fields: FormFieldDefinition[];
  totalFields: number;
}

@Injectable({ providedIn: 'root' })
export class FormSchemaService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  /**
   * Get form schema
   */
  getFormSchema(caseTypeId: number): Observable<FormSchema> {
    return this.http.get<ApiResponse<FormSchema>>(
      `${this.baseUrl}/api/admin/form-schemas/case-types/${caseTypeId}`
    ).pipe(map(response => response.data));
  }

  /**
   * Create field
   */
  createField(field: CreateFormFieldDTO): Observable<FormFieldDefinition> {
    return this.http.post<ApiResponse<FormFieldDefinition>>(
      `${this.baseUrl}/api/admin/form-schemas/fields`,
      field,
      { headers: this.getAuthHeaders() }
    ).pipe(map(response => response.data));
  }

  /**
   * Update field
   */
  updateField(fieldId: number, updates: UpdateFormFieldDTO): Observable<FormFieldDefinition> {
    return this.http.put<ApiResponse<FormFieldDefinition>>(
      `${this.baseUrl}/api/admin/form-schemas/fields/${fieldId}`,
      updates,
      { headers: this.getAuthHeaders() }
    ).pipe(map(response => response.data));
  }

  /**
   * Delete field
   */
  deleteField(fieldId: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/api/admin/form-schemas/fields/${fieldId}`,
      { headers: this.getAuthHeaders() }
    ).pipe(map(() => undefined));
  }

  /**
   * Get single field
   */
  getFieldById(fieldId: number): Observable<FormFieldDefinition> {
    return this.http.get<ApiResponse<FormFieldDefinition>>(
      `${this.baseUrl}/api/admin/form-schemas/fields/${fieldId}`,
      { headers: this.getAuthHeaders() }
    ).pipe(map(response => response.data));
  }
}
```

### Complete Angular Component

```typescript
import { Component, OnInit } from '@angular/core';
import { FormSchemaService, FormSchema, FormFieldDefinition } from './form-schema.service';

@Component({
  selector: 'app-form-builder',
  templateUrl: './form-builder.component.html'
})
export class FormBuilderComponent implements OnInit {
  formSchema: FormSchema | null = null;
  caseTypeId: number = 1;

  constructor(private formSchemaService: FormSchemaService) {}

  ngOnInit() {
    this.loadSchema();
  }

  /**
   * Load form schema
   */
  loadSchema() {
    this.formSchemaService.getFormSchema(this.caseTypeId).subscribe({
      next: (schema) => {
        this.formSchema = schema;
      },
      error: (error) => {
        console.error('Error loading schema:', error);
        alert('Failed to load form schema');
      }
    });
  }

  /**
   * Create new field
   */
  createField() {
    const newField = {
      caseTypeId: this.caseTypeId,
      fieldName: 'newField',
      fieldLabel: 'New Field',
      fieldType: 'TEXT',
      isRequired: false,
      displayOrder: this.formSchema!.fields.length + 1,
      isActive: true
    };

    this.formSchemaService.createField(newField).subscribe({
      next: (created) => {
        console.log('Field created:', created);
        this.loadSchema(); // Refresh to see new field
      },
      error: (error) => {
        if (error.status === 409) {
          alert('Field name already exists');
        } else {
          alert('Failed to create field: ' + error.message);
        }
      }
    });
  }

  /**
   * Update field
   */
  updateField(field: FormFieldDefinition, updates: Partial<UpdateFormFieldDTO>) {
    if (!field.id) {
      alert('Cannot update field without ID');
      return;
    }

    const updateData: UpdateFormFieldDTO = {
      ...updates
    };

    this.formSchemaService.updateField(field.id, updateData).subscribe({
      next: (updated) => {
        // Update local field
        const index = this.formSchema!.fields.findIndex(f => f.id === field.id);
        if (index !== -1) {
          this.formSchema!.fields[index] = updated;
        }
        this.showSuccess('Field updated successfully');
      },
      error: (error) => {
        alert('Update failed: ' + error.message);
      }
    });
  }

  /**
   * Delete field
   */
  deleteField(fieldId: number) {
    if (confirm('Are you sure you want to delete this field?')) {
      this.formSchemaService.deleteField(fieldId).subscribe({
        next: () => {
          console.log('Field deleted');
          this.loadSchema(); // Refresh to remove deleted field
        },
        error: (error) => {
          alert('Delete failed: ' + error.message);
        }
      });
    }
  }

  /**
   * Quick edit - update field label
   */
  onFieldLabelChange(field: FormFieldDefinition, newLabel: string) {
    this.updateField(field, { fieldLabel: newLabel });
  }

  /**
   * Toggle field required status
   */
  toggleRequired(field: FormFieldDefinition) {
    this.updateField(field, { isRequired: !field.isRequired });
  }

  /**
   * Toggle field active status
   */
  toggleActive(field: FormFieldDefinition) {
    this.updateField(field, { isActive: !field.isActive });
  }

  showSuccess(message: string) {
    // Implement your success notification
    console.log('Success:', message);
  }
}
```

---

## Best Practices

### ✅ Do:

1. **Refresh schema after operations**
   ```typescript
   this.loadSchema(); // After create, update, delete
   ```

2. **Validate field name uniqueness**
   - Backend will return 409 if field name already exists

3. **Store caseTypeId when creating**
   - Field is automatically mapped to case type

### ❌ Don't:

1. **Don't mix case types**
   - Each field belongs to one case type (set when creating)

2. **Don't ignore errors**
   - Always handle errors and show user-friendly messages

---

## Summary

### Simple Workflow

```
1. Load Schema
   GET /api/admin/form-schemas/case-types/{id}
   → Get all fields

2. Create Field
   POST /api/admin/form-schemas/fields
   → Include caseTypeId

3. Update Field
   PUT /api/admin/form-schemas/fields/{id}
   → Just send the fields to update

4. Delete Field
   DELETE /api/admin/form-schemas/fields/{id}

5. Refresh Schema
   → Reload to see changes
```

### Key Points

- ✅ **Simple**: One operation = One API call
- ✅ **Clear**: Each field operation is independent
- ✅ **Easy**: No version checking needed
- ✅ **Mapped**: Field is linked to case type when created

---

**Last Updated**: After simplification to individual CRUD only

