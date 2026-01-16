# Frontend Dynamic Form Implementation Guide

## Overview

This guide will help you implement **dynamic form rendering** in your frontend application. Forms are generated automatically based on the form schema retrieved from the backend API. No hardcoded forms needed!

### Key Concepts

1. **Form Schema**: JSON structure defining all form fields for a case type
2. **Dynamic Rendering**: Generate form HTML/JSX based on schema
3. **Validation**: Client-side and server-side validation
4. **Form Data**: Submit form data as JSON string

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Architecture Overview](#architecture-overview)
3. [Step-by-Step Implementation](#step-by-step-implementation)
4. [Complete Code Examples](#complete-code-examples)
5. [Field Type Handlers](#field-type-handlers)
6. [Validation Implementation](#validation-implementation)
7. [File Upload Handling](#file-upload-handling)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Frontend framework (Angular, React, Vue, etc.)
- HTTP client (Axios, Fetch, HttpClient)
- Form validation library (Angular Reactive Forms, Formik, VeeValidate, etc.)
- Understanding of JSON parsing

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│  1. User Selects Case Type                      │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  2. Fetch Form Schema from API                  │
│     GET /api/admin/form-schemas/case-types/{id} │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  3. Parse Schema & Build Form Structure         │
│     - Create form controls                      │
│     - Apply validation rules                   │
│     - Set up field options                      │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  4. Render Form Dynamically                     │
│     - Loop through fields                       │
│     - Render based on fieldType                 │
│     - Apply styling & layout                    │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  5. User Fills Form                             │
│     - Client-side validation                    │
│     - Real-time error display                   │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  6. Submit Form Data                            │
│     - Convert to JSON string                    │
│     - POST /api/cases                           │
└─────────────────────────────────────────────────┘
```

---

## Step-by-Step Implementation

### Step 1: Create Form Schema Service

**Purpose**: Fetch form schema from backend API

#### Angular Example

```typescript
// services/form-schema.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface FormFieldDefinition {
  id: number;
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  isRequired: boolean;
  validationRules?: string;
  displayOrder: number;
  isActive: boolean;
  defaultValue?: string;
  fieldOptions?: string;
  placeholder?: string;
  helpText?: string;
  fieldGroup?: string;
}

export interface FormSchema {
  caseTypeId: number;
  caseTypeName: string;
  caseTypeCode: string;
  totalFields: number;
  fields: FormFieldDefinition[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class FormSchemaService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  /**
   * Get form schema for a case type
   */
  getFormSchema(caseTypeId: number): Observable<FormSchema> {
    return this.http.get<ApiResponse<FormSchema>>(
      `${this.baseUrl}/api/admin/form-schemas/case-types/${caseTypeId}`
    ).pipe(
      map(response => response.data)
    );
  }

  /**
   * Validate form data before submission
   */
  validateFormData(caseTypeId: number, formData: any): Observable<any> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/api/admin/form-schemas/validate`,
      {
        caseTypeId,
        formData
      }
    ).pipe(
      map(response => response.data)
    );
  }
}
```

#### React Example

```typescript
// services/formSchemaService.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

export interface FormFieldDefinition {
  id: number;
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  isRequired: boolean;
  validationRules?: string;
  displayOrder: number;
  isActive: boolean;
  defaultValue?: string;
  fieldOptions?: string;
  placeholder?: string;
  helpText?: string;
}

export interface FormSchema {
  caseTypeId: number;
  caseTypeName: string;
  caseTypeCode: string;
  totalFields: number;
  fields: FormFieldDefinition[];
}

export const formSchemaService = {
  /**
   * Get form schema for a case type
   */
  async getFormSchema(caseTypeId: number): Promise<FormSchema> {
    const response = await axios.get<{ success: boolean; data: FormSchema }>(
      `${API_BASE_URL}/api/admin/form-schemas/case-types/${caseTypeId}`
    );
    return response.data.data;
  },

  /**
   * Validate form data before submission
   */
  async validateFormData(caseTypeId: number, formData: any): Promise<any> {
    const response = await axios.post<{ success: boolean; data: any }>(
      `${API_BASE_URL}/api/admin/form-schemas/validate`,
      { caseTypeId, formData }
    );
    return response.data.data;
  }
};
```

---

### Step 2: Create Dynamic Form Component

**Purpose**: Component that renders form based on schema

#### Angular Example

```typescript
// components/create-case/create-case.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FormSchemaService, FormSchema, FormFieldDefinition } from '../../services/form-schema.service';
import { CaseService } from '../../services/case.service';

@Component({
  selector: 'app-create-case',
  templateUrl: './create-case.component.html',
  styleUrls: ['./create-case.component.css']
})
export class CreateCaseComponent implements OnInit {
  formSchema: FormSchema | null = null;
  caseForm: FormGroup = this.fb.group({});
  loading = false;
  error: string | null = null;
  caseTypeId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private formSchemaService: FormSchemaService,
    private caseService: CaseService
  ) {}

  ngOnInit() {
    // Get case type ID from route
    this.route.params.subscribe(params => {
      this.caseTypeId = +params['caseTypeId'];
      if (this.caseTypeId) {
        this.loadFormSchema(this.caseTypeId);
      }
    });
  }

  /**
   * Load form schema from backend
   */
  loadFormSchema(caseTypeId: number) {
    this.loading = true;
    this.error = null;

    this.formSchemaService.getFormSchema(caseTypeId).subscribe({
      next: (schema) => {
        this.formSchema = schema;
        this.buildForm(schema);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading form schema:', error);
        this.error = 'Failed to load form. Please try again.';
        this.loading = false;
      }
    });
  }

  /**
   * Build reactive form based on schema
   */
  buildForm(schema: FormSchema) {
    const formControls: { [key: string]: FormControl } = {};

    schema.fields.forEach(field => {
      // Get initial value
      let initialValue = field.defaultValue || '';
      
      // Create validators array
      const validators = this.buildValidators(field);

      // Create form control
      formControls[field.fieldName] = new FormControl(initialValue, validators);
    });

    // Create form group
    this.caseForm = this.fb.group(formControls);
  }

  /**
   * Build validators based on field definition
   */
  buildValidators(field: FormFieldDefinition): any[] {
    const validators: any[] = [];

    // Required validator
    if (field.isRequired) {
      validators.push(Validators.required);
    }

    // Parse validation rules
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

    return validators;
  }

  /**
   * Get field options for SELECT/RADIO
   */
  getFieldOptions(field: FormFieldDefinition): any[] {
    if (!field.fieldOptions) {
      return [];
    }
    try {
      return JSON.parse(field.fieldOptions);
    } catch (e) {
      console.error('Error parsing field options:', e);
      return [];
    }
  }

  /**
   * Handle file upload
   */
  onFileChange(event: any, fieldName: string) {
    const file = event.target.files[0];
    if (file) {
      // Convert to base64
      const reader = new FileReader();
      reader.onload = () => {
        this.caseForm.patchValue({
          [fieldName]: reader.result // base64 string
        });
      };
      reader.readAsDataURL(file);
    }
  }

  /**
   * Get sorted fields (by displayOrder)
   */
  getSortedFields(): FormFieldDefinition[] {
    if (!this.formSchema) {
      return [];
    }
    return [...this.formSchema.fields].sort((a, b) => 
      a.displayOrder - b.displayOrder
    );
  }

  /**
   * Submit form
   */
  onSubmit() {
    if (this.caseForm.valid) {
      this.loading = true;

      // Get form values
      const formData = this.caseForm.value;

      // Convert to JSON string for caseData
      const caseDataJson = JSON.stringify(formData);

      // Prepare case creation DTO
      const createCaseDTO = {
        caseTypeId: this.caseTypeId!,
        applicantId: this.getCurrentUserId(), // Get from auth service
        unitId: this.getSelectedUnitId(), // Get from user selection
        subject: formData.subject || `${this.formSchema?.caseTypeName} Application`,
        description: formData.description || '',
        caseData: caseDataJson,
        priority: 'MEDIUM',
        applicationDate: new Date().toISOString().split('T')[0]
      };

      // Submit case
      this.caseService.createCase(createCaseDTO).subscribe({
        next: (response) => {
          console.log('Case created:', response);
          this.router.navigate(['/cases', response.data.id]);
        },
        error: (error) => {
          console.error('Error creating case:', error);
          this.error = error.error?.message || 'Failed to create case';
          this.loading = false;
        }
      });
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.caseForm.controls).forEach(key => {
        this.caseForm.get(key)?.markAsTouched();
      });
    }
  }

  /**
   * Get current user ID (implement based on your auth service)
   */
  private getCurrentUserId(): number {
    // TODO: Get from auth service
    return 1;
  }

  /**
   * Get selected unit ID (implement based on your UI)
   */
  private getSelectedUnitId(): number {
    // TODO: Get from user selection or user profile
    return 1;
  }

  /**
   * Check if field has error
   */
  hasFieldError(fieldName: string): boolean {
    const control = this.caseForm.get(fieldName);
    return !!(control && control.invalid && control.touched);
  }

  /**
   * Get field error message
   */
  getFieldError(fieldName: string): string {
    const control = this.caseForm.get(fieldName);
    if (control && control.errors) {
      if (control.errors['required']) {
        return 'This field is required';
      }
      if (control.errors['minlength']) {
        return `Minimum length is ${control.errors['minlength'].requiredLength}`;
      }
      if (control.errors['maxlength']) {
        return `Maximum length is ${control.errors['maxlength'].requiredLength}`;
      }
      if (control.errors['pattern']) {
        return 'Invalid format';
      }
      if (control.errors['min']) {
        return `Minimum value is ${control.errors['min'].min}`;
      }
      if (control.errors['max']) {
        return `Maximum value is ${control.errors['max'].max}`;
      }
      if (control.errors['email']) {
        return 'Invalid email format';
      }
    }
    return '';
  }
}
```

#### Angular Template

```html
<!-- create-case.component.html -->
<div class="container mt-4">
  <h2>Create New Case</h2>

  <!-- Loading State -->
  <div *ngIf="loading && !formSchema" class="text-center">
    <div class="spinner-border" role="status">
      <span class="sr-only">Loading...</span>
    </div>
    <p>Loading form...</p>
  </div>

  <!-- Error State -->
  <div *ngIf="error" class="alert alert-danger" role="alert">
    {{ error }}
  </div>

  <!-- Form -->
  <form [formGroup]="caseForm" (ngSubmit)="onSubmit()" *ngIf="formSchema">
    <div class="card">
      <div class="card-header">
        <h4>{{ formSchema.caseTypeName }}</h4>
      </div>
      <div class="card-body">
        <!-- Dynamic Fields -->
        <div *ngFor="let field of getSortedFields()" class="mb-3">
          
          <!-- TEXT Input -->
          <div *ngIf="field.fieldType === 'TEXT'">
            <label [for]="field.fieldName" class="form-label">
              {{ field.fieldLabel }}
              <span *ngIf="field.isRequired" class="text-danger">*</span>
            </label>
            <input
              type="text"
              class="form-control"
              [id]="field.fieldName"
              [formControlName]="field.fieldName"
              [placeholder]="field.placeholder || ''"
              [class.is-invalid]="hasFieldError(field.fieldName)">
            <div *ngIf="hasFieldError(field.fieldName)" class="invalid-feedback">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

          <!-- NUMBER Input -->
          <div *ngIf="field.fieldType === 'NUMBER'">
            <label [for]="field.fieldName" class="form-label">
              {{ field.fieldLabel }}
              <span *ngIf="field.isRequired" class="text-danger">*</span>
            </label>
            <input
              type="number"
              class="form-control"
              [id]="field.fieldName"
              [formControlName]="field.fieldName"
              [placeholder]="field.placeholder || ''"
              [class.is-invalid]="hasFieldError(field.fieldName)">
            <div *ngIf="hasFieldError(field.fieldName)" class="invalid-feedback">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

          <!-- DATE Input -->
          <div *ngIf="field.fieldType === 'DATE'">
            <label [for]="field.fieldName" class="form-label">
              {{ field.fieldLabel }}
              <span *ngIf="field.isRequired" class="text-danger">*</span>
            </label>
            <input
              type="date"
              class="form-control"
              [id]="field.fieldName"
              [formControlName]="field.fieldName"
              [class.is-invalid]="hasFieldError(field.fieldName)">
            <div *ngIf="hasFieldError(field.fieldName)" class="invalid-feedback">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

          <!-- TEXTAREA -->
          <div *ngIf="field.fieldType === 'TEXTAREA'">
            <label [for]="field.fieldName" class="form-label">
              {{ field.fieldLabel }}
              <span *ngIf="field.isRequired" class="text-danger">*</span>
            </label>
            <textarea
              class="form-control"
              [id]="field.fieldName"
              [formControlName]="field.fieldName"
              [placeholder]="field.placeholder || ''"
              rows="4"
              [class.is-invalid]="hasFieldError(field.fieldName)"></textarea>
            <div *ngIf="hasFieldError(field.fieldName)" class="invalid-feedback">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

          <!-- SELECT Dropdown -->
          <div *ngIf="field.fieldType === 'SELECT'">
            <label [for]="field.fieldName" class="form-label">
              {{ field.fieldLabel }}
              <span *ngIf="field.isRequired" class="text-danger">*</span>
            </label>
            <select
              class="form-control"
              [id]="field.fieldName"
              [formControlName]="field.fieldName"
              [class.is-invalid]="hasFieldError(field.fieldName)">
              <option value="">-- Select --</option>
              <option *ngFor="let option of getFieldOptions(field)" [value]="option.value">
                {{ option.label }}
              </option>
            </select>
            <div *ngIf="hasFieldError(field.fieldName)" class="invalid-feedback">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

          <!-- RADIO Buttons -->
          <div *ngIf="field.fieldType === 'RADIO'">
            <label class="form-label">
              {{ field.fieldLabel }}
              <span *ngIf="field.isRequired" class="text-danger">*</span>
            </label>
            <div>
              <div *ngFor="let option of getFieldOptions(field)" class="form-check">
                <input
                  class="form-check-input"
                  type="radio"
                  [name]="field.fieldName"
                  [id]="field.fieldName + '_' + option.value"
                  [value]="option.value"
                  [formControlName]="field.fieldName">
                <label class="form-check-label" [for]="field.fieldName + '_' + option.value">
                  {{ option.label }}
                </label>
              </div>
            </div>
            <div *ngIf="hasFieldError(field.fieldName)" class="text-danger small">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

          <!-- CHECKBOX -->
          <div *ngIf="field.fieldType === 'CHECKBOX'">
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                [id]="field.fieldName"
                [formControlName]="field.fieldName">
              <label class="form-check-label" [for]="field.fieldName">
                {{ field.fieldLabel }}
                <span *ngIf="field.isRequired" class="text-danger">*</span>
              </label>
            </div>
            <div *ngIf="hasFieldError(field.fieldName)" class="text-danger small">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

          <!-- FILE Upload -->
          <div *ngIf="field.fieldType === 'FILE'">
            <label [for]="field.fieldName" class="form-label">
              {{ field.fieldLabel }}
              <span *ngIf="field.isRequired" class="text-danger">*</span>
            </label>
            <input
              type="file"
              class="form-control"
              [id]="field.fieldName"
              (change)="onFileChange($event, field.fieldName)"
              [class.is-invalid]="hasFieldError(field.fieldName)">
            <div *ngIf="hasFieldError(field.fieldName)" class="invalid-feedback">
              {{ getFieldError(field.fieldName) }}
            </div>
            <small *ngIf="field.helpText" class="form-text text-muted">
              {{ field.helpText }}
            </small>
          </div>

        </div>
      </div>
      <div class="card-footer">
        <button type="submit" class="btn btn-primary" [disabled]="caseForm.invalid || loading">
          <span *ngIf="loading" class="spinner-border spinner-border-sm mr-2"></span>
          Submit Case
        </button>
        <button type="button" class="btn btn-secondary ml-2" (click)="router.navigate(['/cases'])">
          Cancel
        </button>
      </div>
    </div>
  </form>
</div>
```

---

### Step 3: React Implementation Example

```typescript
// components/CreateCase.tsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { formSchemaService, FormSchema, FormFieldDefinition } from '../services/formSchemaService';
import { caseService } from '../services/caseService';

const CreateCase: React.FC = () => {
  const { caseTypeId } = useParams<{ caseTypeId: string }>();
  const navigate = useNavigate();
  
  const [formSchema, setFormSchema] = useState<FormSchema | null>(null);
  const [formData, setFormData] = useState<{ [key: string]: any }>({});
  const [errors, setErrors] = useState<{ [key: string]: string }>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (caseTypeId) {
      loadFormSchema(parseInt(caseTypeId));
    }
  }, [caseTypeId]);

  const loadFormSchema = async (id: number) => {
    try {
      setLoading(true);
      const schema = await formSchemaService.getFormSchema(id);
      setFormSchema(schema);
      initializeFormData(schema);
    } catch (error) {
      console.error('Error loading form schema:', error);
    } finally {
      setLoading(false);
    }
  };

  const initializeFormData = (schema: FormSchema) => {
    const initialData: { [key: string]: any } = {};
    schema.fields.forEach(field => {
      initialData[field.fieldName] = field.defaultValue || '';
    });
    setFormData(initialData);
  };

  const handleChange = (fieldName: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [fieldName]: value
    }));
    // Clear error for this field
    if (errors[fieldName]) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[fieldName];
        return newErrors;
      });
    }
  };

  const handleFileChange = (fieldName: string, file: File) => {
    const reader = new FileReader();
    reader.onload = () => {
      handleChange(fieldName, reader.result);
    };
    reader.readAsDataURL(file);
  };

  const validateField = (field: FormFieldDefinition, value: any): string => {
    if (field.isRequired && (!value || value.toString().trim() === '')) {
      return `${field.fieldLabel} is required`;
    }
    // Add more validation logic here
    return '';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validate all fields
    const newErrors: { [key: string]: string } = {};
    formSchema?.fields.forEach(field => {
      const error = validateField(field, formData[field.fieldName]);
      if (error) {
        newErrors[field.fieldName] = error;
      }
    });

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    try {
      setLoading(true);
      const caseDataJson = JSON.stringify(formData);
      const createCaseDTO = {
        caseTypeId: parseInt(caseTypeId!),
        applicantId: getCurrentUserId(),
        unitId: getSelectedUnitId(),
        subject: `${formSchema?.caseTypeName} Application`,
        description: '',
        caseData: caseDataJson,
        priority: 'MEDIUM',
        applicationDate: new Date().toISOString().split('T')[0]
      };

      const response = await caseService.createCase(createCaseDTO);
      navigate(`/cases/${response.data.id}`);
    } catch (error) {
      console.error('Error creating case:', error);
    } finally {
      setLoading(false);
    }
  };

  const getFieldOptions = (field: FormFieldDefinition): any[] => {
    if (!field.fieldOptions) return [];
    try {
      return JSON.parse(field.fieldOptions);
    } catch {
      return [];
    }
  };

  const getSortedFields = (): FormFieldDefinition[] => {
    if (!formSchema) return [];
    return [...formSchema.fields].sort((a, b) => a.displayOrder - b.displayOrder);
  };

  const renderField = (field: FormFieldDefinition) => {
    const value = formData[field.fieldName] || '';
    const error = errors[field.fieldName];

    switch (field.fieldType) {
      case 'TEXT':
        return (
          <div key={field.id} className="mb-3">
            <label className="form-label">
              {field.fieldLabel}
              {field.isRequired && <span className="text-danger">*</span>}
            </label>
            <input
              type="text"
              className={`form-control ${error ? 'is-invalid' : ''}`}
              value={value}
              onChange={(e) => handleChange(field.fieldName, e.target.value)}
              placeholder={field.placeholder}
            />
            {error && <div className="invalid-feedback">{error}</div>}
            {field.helpText && <small className="form-text text-muted">{field.helpText}</small>}
          </div>
        );

      case 'NUMBER':
        return (
          <div key={field.id} className="mb-3">
            <label className="form-label">
              {field.fieldLabel}
              {field.isRequired && <span className="text-danger">*</span>}
            </label>
            <input
              type="number"
              className={`form-control ${error ? 'is-invalid' : ''}`}
              value={value}
              onChange={(e) => handleChange(field.fieldName, parseFloat(e.target.value))}
              placeholder={field.placeholder}
            />
            {error && <div className="invalid-feedback">{error}</div>}
            {field.helpText && <small className="form-text text-muted">{field.helpText}</small>}
          </div>
        );

      case 'DATE':
        return (
          <div key={field.id} className="mb-3">
            <label className="form-label">
              {field.fieldLabel}
              {field.isRequired && <span className="text-danger">*</span>}
            </label>
            <input
              type="date"
              className={`form-control ${error ? 'is-invalid' : ''}`}
              value={value}
              onChange={(e) => handleChange(field.fieldName, e.target.value)}
            />
            {error && <div className="invalid-feedback">{error}</div>}
            {field.helpText && <small className="form-text text-muted">{field.helpText}</small>}
          </div>
        );

      case 'TEXTAREA':
        return (
          <div key={field.id} className="mb-3">
            <label className="form-label">
              {field.fieldLabel}
              {field.isRequired && <span className="text-danger">*</span>}
            </label>
            <textarea
              className={`form-control ${error ? 'is-invalid' : ''}`}
              value={value}
              onChange={(e) => handleChange(field.fieldName, e.target.value)}
              placeholder={field.placeholder}
              rows={4}
            />
            {error && <div className="invalid-feedback">{error}</div>}
            {field.helpText && <small className="form-text text-muted">{field.helpText}</small>}
          </div>
        );

      case 'SELECT':
        return (
          <div key={field.id} className="mb-3">
            <label className="form-label">
              {field.fieldLabel}
              {field.isRequired && <span className="text-danger">*</span>}
            </label>
            <select
              className={`form-control ${error ? 'is-invalid' : ''}`}
              value={value}
              onChange={(e) => handleChange(field.fieldName, e.target.value)}
            >
              <option value="">-- Select --</option>
              {getFieldOptions(field).map((option: any) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {error && <div className="invalid-feedback">{error}</div>}
            {field.helpText && <small className="form-text text-muted">{field.helpText}</small>}
          </div>
        );

      case 'FILE':
        return (
          <div key={field.id} className="mb-3">
            <label className="form-label">
              {field.fieldLabel}
              {field.isRequired && <span className="text-danger">*</span>}
            </label>
            <input
              type="file"
              className={`form-control ${error ? 'is-invalid' : ''}`}
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleFileChange(field.fieldName, file);
              }}
            />
            {error && <div className="invalid-feedback">{error}</div>}
            {field.helpText && <small className="form-text text-muted">{field.helpText}</small>}
          </div>
        );

      default:
        return null;
    }
  };

  if (loading && !formSchema) {
    return <div>Loading form...</div>;
  }

  if (!formSchema) {
    return <div>Form not found</div>;
  }

  return (
    <div className="container mt-4">
      <h2>Create New Case</h2>
      <form onSubmit={handleSubmit}>
        <div className="card">
          <div className="card-header">
            <h4>{formSchema.caseTypeName}</h4>
          </div>
          <div className="card-body">
            {getSortedFields().map(field => renderField(field))}
          </div>
          <div className="card-footer">
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Submitting...' : 'Submit Case'}
            </button>
            <button type="button" className="btn btn-secondary ml-2" onClick={() => navigate('/cases')}>
              Cancel
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default CreateCase;
```

---

## Field Type Handlers

### Complete Field Type Implementation Guide

| Field Type | HTML Element | Value Type | Special Handling |
|------------|--------------|------------|------------------|
| TEXT | `<input type="text">` | string | Pattern validation |
| NUMBER | `<input type="number">` | number | Min/max validation |
| DATE | `<input type="date">` | string (YYYY-MM-DD) | Date range validation |
| EMAIL | `<input type="email">` | string | Email format validation |
| PHONE | `<input type="tel">` | string | 10-digit Indian format |
| TEXTAREA | `<textarea>` | string | Multi-line text |
| SELECT | `<select>` | string | Options from fieldOptions |
| RADIO | `<input type="radio">` | string | Options from fieldOptions |
| CHECKBOX | `<input type="checkbox">` | boolean | Checked/unchecked |
| FILE | `<input type="file">` | base64 string | File to base64 conversion |

---

## Validation Implementation

### Client-Side Validation Rules

```typescript
// validation-helper.ts
export const validateField = (
  field: FormFieldDefinition,
  value: any
): string => {
  // Required check
  if (field.isRequired) {
    if (value === null || value === undefined || value === '') {
      return `${field.fieldLabel} is required`;
    }
  }

  // Skip other validations if empty and not required
  if (!value || value.toString().trim() === '') {
    return '';
  }

  // Parse validation rules
  if (!field.validationRules) {
    return '';
  }

  try {
    const rules = JSON.parse(field.validationRules);

    switch (field.fieldType) {
      case 'TEXT':
      case 'TEXTAREA':
        const strValue = value.toString();
        if (rules.minLength && strValue.length < rules.minLength) {
          return `Minimum length is ${rules.minLength} characters`;
        }
        if (rules.maxLength && strValue.length > rules.maxLength) {
          return `Maximum length is ${rules.maxLength} characters`;
        }
        if (rules.pattern && !new RegExp(rules.pattern).test(strValue)) {
          return 'Invalid format';
        }
        break;

      case 'NUMBER':
        const numValue = parseFloat(value);
        if (isNaN(numValue)) {
          return 'Must be a valid number';
        }
        if (rules.min !== undefined && numValue < rules.min) {
          return `Minimum value is ${rules.min}`;
        }
        if (rules.max !== undefined && numValue > rules.max) {
          return `Maximum value is ${rules.max}`;
        }
        break;

      case 'DATE':
        const dateValue = new Date(value);
        if (isNaN(dateValue.getTime())) {
          return 'Must be a valid date';
        }
        if (rules.minDate) {
          const minDate = rules.minDate === 'today' 
            ? new Date() 
            : new Date(rules.minDate);
          if (dateValue < minDate) {
            return `Date must be on or after ${rules.minDate}`;
          }
        }
        if (rules.maxDate) {
          const maxDate = rules.maxDate === 'today' 
            ? new Date() 
            : new Date(rules.maxDate);
          if (dateValue > maxDate) {
            return `Date must be on or before ${rules.maxDate}`;
          }
        }
        break;

      case 'EMAIL':
        const emailPattern = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        if (!emailPattern.test(value)) {
          return 'Must be a valid email address';
        }
        break;

      case 'PHONE':
        const phonePattern = /^[6-9]\d{9}$/;
        if (!phonePattern.test(value.toString().replace(/\D/g, ''))) {
          return 'Must be a valid 10-digit phone number starting with 6-9';
        }
        break;
    }
  } catch (e) {
    console.error('Error parsing validation rules:', e);
  }

  return '';
};
```

---

## File Upload Handling

### Base64 Conversion (Simple Approach)

```typescript
const handleFileUpload = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      resolve(reader.result as string); // base64 string
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
};

// Usage
const fileInput = document.querySelector('input[type="file"]');
fileInput.addEventListener('change', async (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (file) {
    const base64 = await handleFileUpload(file);
    // Store base64 in form data
    setFormData(prev => ({
      ...prev,
      documentFile: base64
    }));
  }
});
```

### File Server Upload (Recommended for Large Files)

```typescript
// Upload file to server first, then store file ID in form
const uploadFile = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch('/api/files/upload', {
    method: 'POST',
    body: formData
  });

  const data = await response.json();
  return data.fileId; // Store this ID in form
};
```

---

## Best Practices

### 1. **Cache Form Schema**
```typescript
// Cache schema to avoid repeated API calls
const schemaCache = new Map<number, FormSchema>();

const getCachedSchema = async (caseTypeId: number): Promise<FormSchema> => {
  if (schemaCache.has(caseTypeId)) {
    return schemaCache.get(caseTypeId)!;
  }
  const schema = await formSchemaService.getFormSchema(caseTypeId);
  schemaCache.set(caseTypeId, schema);
  return schema;
};
```

### 2. **Loading States**
Always show loading indicators while fetching schema or submitting form.

### 3. **Error Handling**
Handle API errors gracefully and show user-friendly messages.

### 4. **Form Validation**
- Validate on blur (better UX)
- Show errors immediately after user interaction
- Validate before submission

### 5. **Accessibility**
- Proper labels for all inputs
- ARIA attributes for screen readers
- Keyboard navigation support

### 6. **Performance**
- Lazy load form components
- Debounce validation for text inputs
- Optimize re-renders

---

## Troubleshooting

### Common Issues

**1. Form Schema Not Loading**
- Check API endpoint URL
- Verify case type ID is correct
- Check network tab for errors
- Verify CORS settings

**2. Validation Not Working**
- Check validation rules JSON format
- Verify field type matches validation logic
- Check console for parsing errors

**3. File Upload Issues**
- Check file size limits
- Verify base64 conversion
- Check MIME type restrictions

**4. Form Submission Fails**
- Verify caseData is valid JSON
- Check all required fields are filled
- Verify user authentication
- Check API response for error details

---

## Quick Start Checklist

- [ ] Create FormSchemaService
- [ ] Create dynamic form component
- [ ] Implement field type handlers
- [ ] Add validation logic
- [ ] Handle file uploads
- [ ] Test with different case types
- [ ] Add error handling
- [ ] Add loading states
- [ ] Style the form
- [ ] Test form submission

---

## Summary

This guide provides everything you need to implement dynamic forms in your frontend application. The key is to:

1. **Fetch** form schema from API
2. **Build** form structure dynamically
3. **Render** fields based on fieldType
4. **Validate** using field rules
5. **Submit** form data as JSON string

The form will automatically adapt when admin adds/modifies fields - no code changes needed!

---

**Need Help?** Refer to `FORM_SCHEMA_API_DOCUMENTATION.md` for API details.

