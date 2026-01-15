# Dynamic Workflow Engine API Documentation

## Table of Contents
1. [Overview](#overview)
2. [Key Concepts](#key-concepts)
3. [API Endpoints](#api-endpoints)
4. [Frontend Implementation Guide](#frontend-implementation-guide)
5. [Complete Flow Examples](#complete-flow-examples)

---

## Overview

The RCCMS Dynamic Workflow Engine is a flexible, configurable system that manages case workflows for different case types. Each case type can have its own workflow definition with custom states, transitions, and permissions.

### Features
- **Dynamic Workflow Configuration**: Each case type can have its own workflow
- **Role-Based Permissions**: Different roles can perform different transitions
- **Hierarchy-Based Access Control**: Supports SAME_UNIT, PARENT_UNIT, ANY_UNIT rules
- **Complete Audit Trail**: All workflow transitions are logged
- **Flexible State Management**: Cases move through configurable states

---

## Key Concepts

### 1. Workflow Definition
A workflow definition represents a complete workflow for a case type (e.g., "Mutation (Gift/Sale)", "Partition").

### 2. Workflow State
A state represents a stage in the workflow (e.g., "CITIZEN_APPLICATION", "DA_ENTRY", "APPROVED").

### 3. Workflow Transition
A transition represents an action that moves a case from one state to another (e.g., "SUBMIT", "APPROVE", "REJECT").

### 4. Workflow Permission
Permissions define which roles can perform which transitions, with hierarchy rules.

### 5. Case Workflow Instance
Each case has a workflow instance that tracks its current state and assignment.

### 6. Workflow History
Complete audit trail of all transitions performed on a case.

---

## API Endpoints

### Base URL
```
http://localhost:8080/api
```

### Authentication
All endpoints require JWT Bearer token in Authorization header:
```
Authorization: Bearer <token>
```

---

### Case Management APIs

#### 1. Create Case
**POST** `/api/cases`

Creates a new case and automatically initializes its workflow instance.

**Request Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "caseTypeId": 1,
  "applicantId": 123,
  "unitId": 5,
  "subject": "Mutation Application for Land Transfer",
  "description": "Applying for mutation after gift deed registration",
  "priority": "MEDIUM",
  "applicationDate": "2026-01-15",
  "remarks": "Additional remarks if any",
  "caseData": "{\"deedNumber\":\"DEED123\",\"registrationDate\":\"2026-01-10\"}"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Case created successfully",
  "data": {
    "id": 1,
    "caseNumber": "MUTATION_GIFT_SALE-PATSOI-20260115-0001",
    "caseTypeId": 1,
    "caseTypeName": "Mutation (after Gift/Sale Deeds)",
    "caseTypeCode": "MUTATION_GIFT_SALE",
    "applicantId": 123,
    "applicantName": "John Doe",
    "applicantMobile": "9876543210",
    "applicantEmail": "john@example.com",
    "unitId": 5,
    "unitName": "Patsoi",
    "unitCode": "PATSOI",
    "subject": "Mutation Application for Land Transfer",
    "description": "Applying for mutation after gift deed registration",
    "status": "CITIZEN_APPLICATION",
    "statusName": "Citizen Application",
    "priority": "MEDIUM",
    "applicationDate": "2026-01-15",
    "isActive": true,
    "workflowInstanceId": 1,
    "workflowCode": "MUTATION_GIFT_SALE",
    "currentStateId": 1,
    "currentStateCode": "CITIZEN_APPLICATION",
    "currentStateName": "Citizen Application",
    "assignedToUnitId": 5,
    "assignedToUnitName": "Patsoi"
  }
}
```

#### 2. Get Case by ID
**GET** `/api/cases/{id}`

Retrieves case details by case ID.

**Response:**
```json
{
  "success": true,
  "message": "Case retrieved successfully",
  "data": {
    "id": 1,
    "caseNumber": "MUTATION_GIFT_SALE-PATSOI-20260115-0001",
    ...
  }
}
```

#### 3. Get Case by Case Number
**GET** `/api/cases/number/{caseNumber}`

Retrieves case details by case number.

#### 4. Get Cases by Applicant
**GET** `/api/cases/applicant/{applicantId}`

Retrieves all cases filed by an applicant.

**Response:**
```json
{
  "success": true,
  "message": "Cases retrieved successfully",
  "data": [
    {
      "id": 1,
      "caseNumber": "MUTATION_GIFT_SALE-PATSOI-20260115-0001",
      ...
    }
  ]
}
```

#### 5. Get Cases by Unit
**GET** `/api/cases/unit/{unitId}`

Retrieves all cases in a unit.

#### 6. Get Cases by Status
**GET** `/api/cases/status/{status}`

Retrieves all cases with a specific status.

#### 7. Get Cases Assigned to Officer
**GET** `/api/cases/assigned/{officerId}`

Retrieves all cases assigned to an officer.

---

### Workflow APIs

#### 8. Get Available Transitions
**GET** `/api/cases/{caseId}/transitions`

Gets all available workflow transitions for the current user based on their role and permissions.

**Response:**
```json
{
  "success": true,
  "message": "Available transitions retrieved successfully",
  "data": [
    {
      "id": 1,
      "transitionCode": "SUBMIT_APPLICATION",
      "transitionName": "Submit Application",
      "fromStateCode": "CITIZEN_APPLICATION",
      "toStateCode": "DA_ENTRY",
      "requiresComment": false,
      "description": "Submit application to DA for entry"
    },
    {
      "id": 2,
      "transitionCode": "CANCEL",
      "transitionName": "Cancel Application",
      "fromStateCode": "CITIZEN_APPLICATION",
      "toStateCode": "CANCELLED",
      "requiresComment": true,
      "description": "Cancel the application"
    }
  ]
}
```

#### 9. Execute Transition
**POST** `/api/cases/{caseId}/transitions/execute`

Executes a workflow transition for a case.

**Request Body:**
```json
{
  "caseId": 1,
  "transitionCode": "SUBMIT_APPLICATION",
  "comments": "Application submitted for processing"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transition executed successfully",
  "data": {
    "caseId": 1,
    "transitionCode": "SUBMIT_APPLICATION",
    "message": "Transition executed successfully"
  }
}
```

#### 10. Get Workflow History
**GET** `/api/cases/{caseId}/history`

Gets complete workflow history/audit trail for a case.

**Response:**
```json
{
  "success": true,
  "message": "Workflow history retrieved successfully",
  "data": [
    {
      "id": 1,
      "caseId": 1,
      "fromStateCode": "CITIZEN_APPLICATION",
      "toStateCode": "DA_ENTRY",
      "transitionCode": "SUBMIT_APPLICATION",
      "performedByOfficerId": 10,
      "performedByRole": "CITIZEN",
      "performedAtUnitId": 5,
      "comments": "Application submitted for processing",
      "performedAt": "2026-01-15T10:30:00"
    }
  ]
}
```

---

### Workflow Configuration APIs (Admin Only)

#### 11. Get All Workflows
**GET** `/api/admin/workflow/definitions`

Gets all workflow definitions.

#### 12. Get Workflow by Code
**GET** `/api/admin/workflow/definitions/{workflowCode}`

Gets workflow definition by workflow code.

#### 13. Get Workflow States
**GET** `/api/admin/workflow/{workflowId}/states`

Gets all states for a workflow.

#### 14. Get Workflow Transitions
**GET** `/api/admin/workflow/{workflowId}/transitions`

Gets all transitions for a workflow.

#### 15. Get Transition Permissions
**GET** `/api/admin/workflow/transitions/{transitionId}/permissions`

Gets all permissions for a transition.

---

## Frontend Implementation Guide

### Angular/TypeScript Implementation

#### 1. Case Service

Create a service to handle case operations:

```typescript
// case.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface CreateCaseRequest {
  caseTypeId: number;
  applicantId: number;
  unitId: number;
  subject: string;
  description?: string;
  priority?: string;
  applicationDate?: string;
  remarks?: string;
  caseData?: string;
}

export interface CaseDTO {
  id: number;
  caseNumber: string;
  caseTypeId: number;
  caseTypeName: string;
  caseTypeCode: string;
  applicantId: number;
  applicantName: string;
  applicantMobile: string;
  applicantEmail: string;
  unitId: number;
  unitName: string;
  unitCode: string;
  subject: string;
  description: string;
  status: string;
  statusName: string;
  priority: string;
  applicationDate: string;
  resolvedDate?: string;
  remarks?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  workflowInstanceId?: number;
  workflowCode?: string;
  currentStateId?: number;
  currentStateCode?: string;
  currentStateName?: string;
  assignedToOfficerId?: number;
  assignedToOfficerName?: string;
  assignedToRole?: string;
  assignedToUnitId?: number;
  assignedToUnitName?: string;
}

export interface WorkflowTransitionDTO {
  id: number;
  transitionCode: string;
  transitionName: string;
  fromStateCode: string;
  toStateCode: string;
  requiresComment: boolean;
  description: string;
}

export interface ExecuteTransitionRequest {
  caseId: number;
  transitionCode: string;
  comments?: string;
}

export interface WorkflowHistory {
  id: number;
  caseId: number;
  fromStateCode: string;
  toStateCode: string;
  transitionCode: string;
  performedByOfficerId?: number;
  performedByRole: string;
  performedAtUnitId?: number;
  comments?: string;
  performedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class CaseService {
  private apiUrl = `${environment.apiUrl}/api/cases`;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  createCase(request: CreateCaseRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}`, request, {
      headers: this.getHeaders()
    });
  }

  getCaseById(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}`, {
      headers: this.getHeaders()
    });
  }

  getCaseByCaseNumber(caseNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/number/${caseNumber}`, {
      headers: this.getHeaders()
    });
  }

  getCasesByApplicant(applicantId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/applicant/${applicantId}`, {
      headers: this.getHeaders()
    });
  }

  getCasesByUnit(unitId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/unit/${unitId}`, {
      headers: this.getHeaders()
    });
  }

  getCasesByStatus(status: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/status/${status}`, {
      headers: this.getHeaders()
    });
  }

  getCasesAssignedToOfficer(officerId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/assigned/${officerId}`, {
      headers: this.getHeaders()
    });
  }

  getAvailableTransitions(caseId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${caseId}/transitions`, {
      headers: this.getHeaders()
    });
  }

  executeTransition(caseId: number, request: ExecuteTransitionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${caseId}/transitions/execute`, request, {
      headers: this.getHeaders()
    });
  }

  getWorkflowHistory(caseId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${caseId}/history`, {
      headers: this.getHeaders()
    });
  }
}
```

#### 2. Case Creation Component

```typescript
// case-creation.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CaseService, CreateCaseRequest } from '../services/case.service';
import { CaseTypeService } from '../services/case-type.service';
import { AdminUnitService } from '../services/admin-unit.service';

@Component({
  selector: 'app-case-creation',
  templateUrl: './case-creation.component.html'
})
export class CaseCreationComponent implements OnInit {
  caseForm: FormGroup;
  caseTypes: any[] = [];
  units: any[] = [];
  loading = false;

  constructor(
    private fb: FormBuilder,
    private caseService: CaseService,
    private caseTypeService: CaseTypeService,
    private adminUnitService: AdminUnitService
  ) {
    this.caseForm = this.fb.group({
      caseTypeId: ['', Validators.required],
      unitId: ['', Validators.required],
      subject: ['', [Validators.required, Validators.maxLength(500)]],
      description: [''],
      priority: ['MEDIUM'],
      applicationDate: [new Date().toISOString().split('T')[0]],
      remarks: [''],
      caseData: ['']
    });
  }

  ngOnInit() {
    this.loadCaseTypes();
    this.loadUnits();
  }

  loadCaseTypes() {
    this.caseTypeService.getActiveCaseTypes().subscribe({
      next: (response) => {
        if (response.success) {
          this.caseTypes = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading case types:', error);
      }
    });
  }

  loadUnits() {
    // Load units based on user's access level
    this.adminUnitService.getUnitsByLevel('CIRCLE').subscribe({
      next: (response) => {
        if (response.success) {
          this.units = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading units:', error);
      }
    });
  }

  onSubmit() {
    if (this.caseForm.valid) {
      this.loading = true;
      const formValue = this.caseForm.value;
      
      // Get applicant ID from current user (stored in token/localStorage)
      const applicantId = this.getCurrentUserId();
      
      const request: CreateCaseRequest = {
        caseTypeId: formValue.caseTypeId,
        applicantId: applicantId,
        unitId: formValue.unitId,
        subject: formValue.subject,
        description: formValue.description,
        priority: formValue.priority,
        applicationDate: formValue.applicationDate,
        remarks: formValue.remarks,
        caseData: formValue.caseData ? JSON.stringify(formValue.caseData) : undefined
      };

      this.caseService.createCase(request).subscribe({
        next: (response) => {
          if (response.success) {
            alert('Case created successfully! Case Number: ' + response.data.caseNumber);
            this.caseForm.reset();
            // Navigate to case details or case list
          } else {
            alert('Error: ' + response.message);
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error creating case:', error);
          alert('Error creating case. Please try again.');
          this.loading = false;
        }
      });
    }
  }

  private getCurrentUserId(): number {
    // Extract from JWT token or localStorage
    const token = localStorage.getItem('token');
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.userId || payload.id;
    }
    throw new Error('User not authenticated');
  }
}
```

#### 3. Case Details Component with Workflow Actions

```typescript
// case-details.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CaseService, CaseDTO, WorkflowTransitionDTO, ExecuteTransitionRequest } from '../services/case.service';

@Component({
  selector: 'app-case-details',
  templateUrl: './case-details.component.html'
})
export class CaseDetailsComponent implements OnInit {
  caseId: number;
  case: CaseDTO;
  availableTransitions: WorkflowTransitionDTO[] = [];
  workflowHistory: any[] = [];
  loading = false;
  showTransitionModal = false;
  selectedTransition: WorkflowTransitionDTO | null = null;
  transitionComment = '';

  constructor(
    private route: ActivatedRoute,
    private caseService: CaseService
  ) {}

  ngOnInit() {
    this.caseId = +this.route.snapshot.params['id'];
    this.loadCaseDetails();
    this.loadAvailableTransitions();
    this.loadWorkflowHistory();
  }

  loadCaseDetails() {
    this.loading = true;
    this.caseService.getCaseById(this.caseId).subscribe({
      next: (response) => {
        if (response.success) {
          this.case = response.data;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading case:', error);
        this.loading = false;
      }
    });
  }

  loadAvailableTransitions() {
    this.caseService.getAvailableTransitions(this.caseId).subscribe({
      next: (response) => {
        if (response.success) {
          this.availableTransitions = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading transitions:', error);
      }
    });
  }

  loadWorkflowHistory() {
    this.caseService.getWorkflowHistory(this.caseId).subscribe({
      next: (response) => {
        if (response.success) {
          this.workflowHistory = response.data;
        }
      },
      error: (error) => {
        console.error('Error loading workflow history:', error);
      }
    });
  }

  openTransitionModal(transition: WorkflowTransitionDTO) {
    this.selectedTransition = transition;
    this.transitionComment = '';
    this.showTransitionModal = true;
  }

  executeTransition() {
    if (!this.selectedTransition) {
      return;
    }

    if (this.selectedTransition.requiresComment && !this.transitionComment.trim()) {
      alert('Comment is required for this transition');
      return;
    }

    const request: ExecuteTransitionRequest = {
      caseId: this.caseId,
      transitionCode: this.selectedTransition.transitionCode,
      comments: this.transitionComment || undefined
    };

    this.loading = true;
    this.caseService.executeTransition(this.caseId, request).subscribe({
      next: (response) => {
        if (response.success) {
          alert('Transition executed successfully!');
          this.showTransitionModal = false;
          this.selectedTransition = null;
          this.transitionComment = '';
          // Reload case details and transitions
          this.loadCaseDetails();
          this.loadAvailableTransitions();
          this.loadWorkflowHistory();
        } else {
          alert('Error: ' + response.message);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error executing transition:', error);
        alert('Error executing transition. Please try again.');
        this.loading = false;
      }
    });
  }

  cancelTransition() {
    this.showTransitionModal = false;
    this.selectedTransition = null;
    this.transitionComment = '';
  }
}
```

#### 4. Case Details Template

```html
<!-- case-details.component.html -->
<div class="case-details-container" *ngIf="case">
  <div class="case-header">
    <h2>Case Details</h2>
    <div class="case-number">Case Number: {{ case.caseNumber }}</div>
  </div>

  <div class="case-info">
    <div class="info-section">
      <h3>Case Information</h3>
      <p><strong>Case Type:</strong> {{ case.caseTypeName }}</p>
      <p><strong>Subject:</strong> {{ case.subject }}</p>
      <p><strong>Description:</strong> {{ case.description }}</p>
      <p><strong>Priority:</strong> {{ case.priority }}</p>
      <p><strong>Status:</strong> {{ case.statusName }} ({{ case.status }})</p>
      <p><strong>Application Date:</strong> {{ case.applicationDate | date }}</p>
    </div>

    <div class="info-section">
      <h3>Applicant Information</h3>
      <p><strong>Name:</strong> {{ case.applicantName }}</p>
      <p><strong>Mobile:</strong> {{ case.applicantMobile }}</p>
      <p><strong>Email:</strong> {{ case.applicantEmail }}</p>
    </div>

    <div class="info-section">
      <h3>Unit Information</h3>
      <p><strong>Unit:</strong> {{ case.unitName }}</p>
      <p><strong>Unit Code:</strong> {{ case.unitCode }}</p>
    </div>
  </div>

  <!-- Available Transitions -->
  <div class="workflow-actions" *ngIf="availableTransitions.length > 0">
    <h3>Available Actions</h3>
    <div class="transition-buttons">
      <button 
        *ngFor="let transition of availableTransitions"
        class="btn btn-primary"
        (click)="openTransitionModal(transition)">
        {{ transition.transitionName }}
      </button>
    </div>
  </div>

  <!-- Workflow History -->
  <div class="workflow-history">
    <h3>Workflow History</h3>
    <table class="table">
      <thead>
        <tr>
          <th>Date</th>
          <th>From State</th>
          <th>To State</th>
          <th>Action</th>
          <th>Performed By</th>
          <th>Comments</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let history of workflowHistory">
          <td>{{ history.performedAt | date:'short' }}</td>
          <td>{{ history.fromStateCode }}</td>
          <td>{{ history.toStateCode }}</td>
          <td>{{ history.transitionCode }}</td>
          <td>{{ history.performedByRole }}</td>
          <td>{{ history.comments }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>

<!-- Transition Modal -->
<div class="modal" *ngIf="showTransitionModal" (click)="cancelTransition()">
  <div class="modal-content" (click)="$event.stopPropagation()">
    <h3>Execute Transition: {{ selectedTransition?.transitionName }}</h3>
    <p>{{ selectedTransition?.description }}</p>
    <div class="form-group" *ngIf="selectedTransition?.requiresComment">
      <label>Comments (Required):</label>
      <textarea 
        [(ngModel)]="transitionComment" 
        class="form-control" 
        rows="4"
        placeholder="Enter comments for this transition">
      </textarea>
    </div>
    <div class="modal-actions">
      <button class="btn btn-primary" (click)="executeTransition()" [disabled]="loading">
        {{ loading ? 'Processing...' : 'Execute' }}
      </button>
      <button class="btn btn-secondary" (click)="cancelTransition()">Cancel</button>
    </div>
  </div>
</div>
```

---

## Complete Flow Examples

### Example 1: Citizen Creates a Case

1. **Citizen logs in** (using citizen login API)
2. **Citizen fills case creation form**:
   - Selects case type: "Mutation (after Gift/Sale Deeds)"
   - Selects unit: "Patsoi Circle"
   - Enters subject, description
3. **Frontend calls** `POST /api/cases`
4. **Backend**:
   - Creates case with status "CITIZEN_APPLICATION"
   - Initializes workflow instance
   - Returns case with workflow details
5. **Frontend displays** case number and current status

### Example 2: DA Processes Case

1. **DA logs in** (using officer login API)
2. **DA views assigned cases** (calls `GET /api/cases/assigned/{officerId}`)
3. **DA opens case details** (calls `GET /api/cases/{id}`)
4. **Frontend loads available transitions** (calls `GET /api/cases/{id}/transitions`)
5. **DA sees available action**: "Enter in Register"
6. **DA clicks action** and executes transition:
   - Frontend calls `POST /api/cases/{id}/transitions/execute`
   - Backend validates permission (DA at same unit)
   - Backend executes transition: CITIZEN_APPLICATION → DA_ENTRY
   - Backend creates history record
7. **Frontend refreshes** case details and shows updated status

### Example 3: Circle Mandol Receives and Processes

1. **Circle Mandol logs in**
2. **Circle Mandol views cases** in their unit
3. **Circle Mandol opens case** in "DA_ENTRY" state
4. **Available transitions**: "Receive by Mandol"
5. **Circle Mandol executes transition**: DA_ENTRY → MANDOL_RECEIVED
6. **Next available transition**: "Generate Notice"
7. **Circle Mandol executes**: MANDOL_RECEIVED → NOTICE_GENERATED

### Example 4: SDC Approves Case

1. **SDC (Circle Officer) logs in**
2. **SDC views cases** assigned to them
3. **SDC opens case** in "DECISION_PENDING" state
4. **Available transitions**: "Approve" or "Reject"
5. **SDC executes "Approve"**: DECISION_PENDING → APPROVED
6. **Case moves to** "MANDOL_UPDATE" state
7. **Circle Mandol updates** land records: MANDOL_UPDATE → LAND_RECORD_UPDATED

---

## Error Handling

### Common Errors

1. **401 Unauthorized**: Token expired or invalid
   - Solution: Re-login and get new token

2. **403 Forbidden**: User doesn't have permission for transition
   - Solution: Check user role and unit assignment

3. **404 Not Found**: Case or workflow not found
   - Solution: Verify case ID and workflow configuration

4. **400 Bad Request**: Invalid request data
   - Solution: Validate form data before submission

### Error Response Format

```json
{
  "success": false,
  "message": "Error message",
  "errors": [
    {
      "field": "fieldName",
      "message": "Error message for field"
    }
  ]
}
```

---

## Best Practices

1. **Always check available transitions** before showing action buttons
2. **Validate required fields** before submitting transitions
3. **Show loading states** during API calls
4. **Handle errors gracefully** with user-friendly messages
5. **Refresh case details** after executing transitions
6. **Maintain workflow history** for audit purposes
7. **Use proper authentication** headers in all requests
8. **Cache case types and units** to reduce API calls

---

## Notes

- Workflows are configured by admin users via workflow configuration APIs
- Each case type must have a workflow code configured
- Transitions are role and hierarchy-based
- All transitions are logged in workflow history
- Cases cannot move backward unless explicitly configured
- Final states (APPROVED, REJECTED, COMPLETED) cannot be transitioned from

---

## Support

For issues or questions, contact the development team or refer to the Swagger API documentation at:
```
http://localhost:8080/swagger-ui.html
```

