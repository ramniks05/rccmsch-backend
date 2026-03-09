# Frontend Implementation Guide: Field Report Workflow

## Overview

This guide explains how to implement the **dynamic field report request workflow** in the frontend. The system allows Tehsildar to request field reports from field officers (Patwari, Kanungo, etc.) who are posted to units (not courts). The workflow is fully dynamic and aligned with the case workflow system.

---

## Business Flow

### 1. Tehsildar Requests Field Report

**When:** Tehsildar is viewing a case in `PROCEEDINGS_IN_PROGRESS` state

**Steps:**
1. Tehsildar clicks "Request Field Report" button
2. System shows dialog with list of **all field officers below Tehsildar** (all roles: PATWARI, KANUNGO, etc.)
3. Tehsildar selects a field officer
4. System executes `REQUEST_FIELD_REPORT` transition
5. System assigns case to selected field officer
6. Case moves to `FIELD_REPORT_REQUESTED` state

### 2. Field Officer Submits Report

**When:** Field officer logs in and sees assigned case

**Steps:**
1. Field officer sees case in their dashboard (`FIELD_REPORT_REQUESTED` state)
2. Field officer opens case and fills field report form
3. Field officer executes `SUBMIT_FIELD_REPORT` transition
4. Case moves to `FIELD_REPORT_SUBMITTED` state
5. Case auto-assigns back to Tehsildar (based on workflow permissions)

### 3. Tehsildar Reviews Report

**When:** Tehsildar sees case in `FIELD_REPORT_SUBMITTED` state

**Steps:**
1. Tehsildar opens case and reviews submitted report
2. Tehsildar executes `REVIEW_FIELD_REPORT` transition
3. Case moves back to `PROCEEDINGS_IN_PROGRESS` state
4. Case continues with normal proceedings

---

## API Endpoints

### 1. Get Available Transitions for Case

**Endpoint:** `GET /api/cases/{caseId}/transitions`

**Purpose:** Check if `REQUEST_FIELD_REPORT` transition is available

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 123,
      "transitionCode": "REQUEST_FIELD_REPORT",
      "transitionName": "Request Field Report",
      "fromStateCode": "PROCEEDINGS_IN_PROGRESS",
      "toStateCode": "FIELD_REPORT_REQUESTED",
      "requiresComment": false,
      "description": "Request field report from field officer",
      "checklist": {
        "canExecute": true,
        "conditions": [],
        "blockingReasons": []
      }
    }
  ]
}
```

**Usage:** Check if transition exists before showing "Request Field Report" button

---

### 2. Get All Field Officers Below Unit

**Endpoint:** `GET /api/admin/postings/field-officers/unit/{unitId}`

**Purpose:** Get **all field officers** (all roles) in units below the given unit

**Path Parameters:**
- `unitId` - Unit ID (district ID for Tehsildar)

**Response:**
```json
{
  "success": true,
  "message": "Field officers retrieved successfully",
  "data": [
    {
      "id": 15,
      "courtId": null,
      "unitId": 5,
      "unitName": "Chandigarh Circle 1",
      "unitCode": "CHD_CIRCLE_01",
      "unitLgdCode": "400101",
      "roleCode": "PATWARI",
      "roleName": "Patwari",
      "officerId": 25,
      "officerName": "Ram Kumar",
      "mobileNo": "9876543210",
      "postingUserid": "PATWARI@400101",
      "postingType": "UNIT_BASED",
      "isCurrent": true
    },
    {
      "id": 16,
      "courtId": null,
      "unitId": 6,
      "unitName": "Chandigarh Circle 2",
      "unitCode": "CHD_CIRCLE_02",
      "unitLgdCode": "400102",
      "roleCode": "PATWARI",
      "roleName": "Patwari",
      "officerId": 26,
      "officerName": "Shyam Singh",
      "mobileNo": "9876543211",
      "postingUserid": "PATWARI@400102",
      "postingType": "UNIT_BASED",
      "isCurrent": true
    },
    {
      "id": 17,
      "courtId": null,
      "unitId": 5,
      "unitName": "Chandigarh Circle 1",
      "unitCode": "CHD_CIRCLE_01",
      "unitLgdCode": "400101",
      "roleCode": "KANUNGO",
      "roleName": "Kanungo",
      "officerId": 27,
      "officerName": "Mohan Das",
      "mobileNo": "9876543212",
      "postingUserid": "KANUNGO@400101",
      "postingType": "UNIT_BASED",
      "isCurrent": true
    }
  ]
}
```

**Usage:** Show all field officers below Tehsildar's district

---

### 3. Get Field Officers for Court (Alternative)

**Endpoint:** `GET /api/admin/postings/field-officers/court/{courtId}?roleCode={roleCode}`

**Purpose:** Get field officers for a specific court, filtered by role (optional)

**Path Parameters:**
- `courtId` - Court ID

**Query Parameters:**
- `roleCode` (optional) - Filter by role (e.g., "PATWARI"). If omitted, returns all roles

**Response:** Same format as above

**Usage:** Alternative way to get field officers if you have courtId instead of unitId

---

### 4. Execute Transition

**Endpoint:** `POST /api/cases/{caseId}/transitions/execute`

**Purpose:** Execute workflow transition (e.g., `REQUEST_FIELD_REPORT`)

**Request Body:**
```json
{
  "transitionCode": "REQUEST_FIELD_REPORT",
  "comments": "Need field verification for land details"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transition executed successfully",
  "data": {
    "caseId": 123,
    "transitionCode": "REQUEST_FIELD_REPORT",
    "message": "Transition executed successfully"
  }
}
```

**Usage:** Execute `REQUEST_FIELD_REPORT` transition

---

### 5. Assign Case to Officer

**Endpoint:** `PUT /api/admin/cases/{caseId}/assign-officer`

**Purpose:** Manually assign case to a specific field officer

**Request Body:**
```json
{
  "officerId": 25,
  "roleCode": "PATWARI"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Case assigned to officer successfully",
  "data": {
    "id": 123,
    "caseNumber": "PARTITION_CASES-CHD_DIST_01-20260224-0001",
    "currentStateCode": "FIELD_REPORT_REQUESTED",
    "assignedToOfficerId": 25,
    "assignedToOfficerName": "Ram Kumar",
    "assignedToRole": "PATWARI",
    ...
  }
}
```

**Usage:** Assign case to selected field officer after transition

---

### 6. Get My Cases (Field Officer Dashboard)

**Endpoint:** `GET /api/cases/my-cases`

**Purpose:** Get cases assigned to current logged-in officer

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 123,
      "caseNumber": "PARTITION_CASES-CHD_DIST_01-20260224-0001",
      "currentStateCode": "FIELD_REPORT_REQUESTED",
      "currentStateName": "Field Report Requested",
      "assignedToOfficerId": 25,
      "assignedToRole": "PATWARI",
      "subject": "Land partition case",
      ...
    }
  ]
}
```

**Usage:** Field officer sees their assigned cases

---

## Frontend Implementation

### Step 1: Case Detail Component - Show "Request Field Report" Button

**Location:** Case detail page component

**Logic:**
```typescript
// In your case detail component
export class CaseDetailComponent {
  caseId: number;
  case: CaseDTO;
  availableTransitions: WorkflowTransitionDTO[] = [];
  showRequestFieldReportButton = false;

  ngOnInit() {
    this.loadCaseDetails();
    this.loadAvailableTransitions();
  }

  // Load available transitions
  async loadAvailableTransitions() {
    try {
      const response = await this.http.get<ApiResponse<WorkflowTransitionDTO[]>>(
        `/api/cases/${this.caseId}/transitions`
      ).toPromise();
      
      this.availableTransitions = response.data;
      
      // Check if REQUEST_FIELD_REPORT is available
      this.showRequestFieldReportButton = this.availableTransitions.some(
        t => t.transitionCode === 'REQUEST_FIELD_REPORT' && t.checklist.canExecute
      );
    } catch (error) {
      console.error('Error loading transitions:', error);
    }
  }

  // Open field report request dialog
  openRequestFieldReportDialog() {
    const dialogRef = this.dialog.open(FieldReportRequestDialogComponent, {
      width: '700px',
      data: {
        caseId: this.caseId,
        case: this.case,
        unitId: this.case.unitId, // District unit ID
        courtId: this.case.courtId
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === 'success') {
        // Refresh case details
        this.loadCaseDetails();
        this.loadAvailableTransitions();
      }
    });
  }
}
```

**Template:**
```html
<!-- Show button only if transition is available -->
<button 
  *ngIf="showRequestFieldReportButton"
  (click)="openRequestFieldReportDialog()"
  class="btn btn-primary">
  Request Field Report
</button>
```

---

### Step 2: Field Report Request Dialog Component

**Purpose:** Show dialog to select field officer and request report

**Component Structure:**
```typescript
export class FieldReportRequestDialogComponent implements OnInit {
  caseId: number;
  unitId: number;
  courtId: number;
  
  fieldOfficers: PostingDTO[] = [];
  loadingOfficers = false;
  selectedOfficerId: number | null = null;
  selectedOfficer: PostingDTO | null = null;
  comments: string = '';
  submitting = false;

  // Group officers by role for better display
  officersByRole: Map<string, PostingDTO[]> = new Map();

  constructor(
    private http: HttpClient,
    private dialogRef: MatDialogRef<FieldReportRequestDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.caseId = data.caseId;
    this.unitId = data.unitId;
    this.courtId = data.courtId;
  }

  ngOnInit() {
    this.loadFieldOfficers();
  }

  // Load all field officers below Tehsildar's unit
  async loadFieldOfficers() {
    this.loadingOfficers = true;
    try {
      // Option 1: Use unitId (recommended)
      const response = await this.http.get<ApiResponse<PostingDTO[]>>(
        `/api/admin/postings/field-officers/unit/${this.unitId}`
      ).toPromise();
      
      // Option 2: Use courtId (alternative)
      // const response = await this.http.get<ApiResponse<PostingDTO[]>>(
      //   `/api/admin/postings/field-officers/court/${this.courtId}`
      // ).toPromise();
      
      this.fieldOfficers = response.data;
      this.groupOfficersByRole();
    } catch (error) {
      console.error('Error loading field officers:', error);
      this.showError('Failed to load field officers. Please try again.');
    } finally {
      this.loadingOfficers = false;
    }
  }

  // Group officers by role for organized display
  groupOfficersByRole() {
    this.officersByRole.clear();
    this.fieldOfficers.forEach(officer => {
      const role = officer.roleCode;
      if (!this.officersByRole.has(role)) {
        this.officersByRole.set(role, []);
      }
      this.officersByRole.get(role)!.push(officer);
    });
  }

  // Handle officer selection
  onOfficerSelect(officer: PostingDTO) {
    this.selectedOfficerId = officer.officerId;
    this.selectedOfficer = officer;
  }

  // Submit request
  async submitRequest() {
    if (!this.selectedOfficerId || !this.selectedOfficer) {
      this.showError('Please select a field officer');
      return;
    }

    this.submitting = true;
    try {
      // Step 1: Execute REQUEST_FIELD_REPORT transition
      await this.executeTransition();
      
      // Step 2: Assign case to selected officer
      await this.assignCaseToOfficer();
      
      this.showSuccess('Field report requested successfully');
      this.dialogRef.close('success');
    } catch (error) {
      console.error('Error requesting field report:', error);
      this.showError('Failed to request field report. Please try again.');
    } finally {
      this.submitting = false;
    }
  }

  // Execute workflow transition
  private async executeTransition() {
    await this.http.post<ApiResponse<any>>(
      `/api/cases/${this.caseId}/transitions/execute`,
      {
        transitionCode: 'REQUEST_FIELD_REPORT',
        comments: this.comments || 'Field report requested'
      }
    ).toPromise();
  }

  // Assign case to field officer
  private async assignCaseToOfficer() {
    await this.http.put<ApiResponse<CaseDTO>>(
      `/api/admin/cases/${this.caseId}/assign-officer`,
      {
        officerId: this.selectedOfficerId,
        roleCode: this.selectedOfficer!.roleCode
      }
    ).toPromise();
  }

  closeDialog() {
    this.dialogRef.close();
  }
}
```

**Template:**
```html
<div class="dialog-container">
  <h2 mat-dialog-title>Request Field Report</h2>
  
  <mat-dialog-content>
    <!-- Loading state -->
    <div *ngIf="loadingOfficers" class="loading-container">
      <mat-spinner diameter="40"></mat-spinner>
      <p>Loading field officers...</p>
    </div>

    <!-- Field officers list (grouped by role) -->
    <div *ngIf="!loadingOfficers && officersByRole.size > 0">
      <p class="info-text">Select a field officer to request field report:</p>
      
      <!-- Display by role groups -->
      <div *ngFor="let roleGroup of officersByRole | keyvalue" class="role-group">
        <h3 class="role-header">{{ roleGroup.value[0].roleName }}s</h3>
        
        <mat-selection-list [(ngModel)]="selectedOfficerId">
          <mat-list-option 
            *ngFor="let officer of roleGroup.value" 
            [value]="officer.officerId"
            (click)="onOfficerSelect(officer)">
            <div class="officer-item">
              <div class="officer-name">{{ officer.officerName }}</div>
              <div class="officer-details">
                <span class="unit-name">{{ officer.unitName }}</span>
                <span class="mobile">Mobile: {{ officer.mobileNo }}</span>
              </div>
            </div>
          </mat-list-option>
        </mat-selection-list>
      </div>
    </div>

    <!-- No officers available -->
    <div *ngIf="!loadingOfficers && officersByRole.size === 0" class="no-officers">
      <p>No field officers available for this district.</p>
      <p class="help-text">Please contact administrator to assign field officers to units.</p>
    </div>

    <!-- Comments field -->
    <div class="form-group" style="margin-top: 20px;">
      <label>Instructions/Comments (Optional):</label>
      <textarea 
        [(ngModel)]="comments"
        class="form-control"
        rows="3"
        placeholder="Enter any specific instructions for field verification">
      </textarea>
    </div>
  </mat-dialog-content>

  <mat-dialog-actions align="end">
    <button mat-button (click)="closeDialog()" [disabled]="submitting">Cancel</button>
    <button 
      mat-raised-button 
      color="primary" 
      (click)="submitRequest()"
      [disabled]="!selectedOfficerId || submitting">
      <span *ngIf="submitting">Requesting...</span>
      <span *ngIf="!submitting">Request Report</span>
    </button>
  </mat-dialog-actions>
</div>
```

**Styles (CSS):**
```css
.dialog-container {
  min-width: 600px;
  max-width: 800px;
}

.role-group {
  margin-bottom: 20px;
}

.role-header {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 2px solid #e0e0e0;
}

.officer-item {
  padding: 8px 0;
}

.officer-name {
  font-weight: 500;
  font-size: 14px;
  color: #212121;
}

.officer-details {
  display: flex;
  gap: 15px;
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}

.unit-name {
  font-style: italic;
}

.loading-container {
  text-align: center;
  padding: 40px;
}

.no-officers {
  text-align: center;
  padding: 40px;
  color: #666;
}

.help-text {
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}
```

---

### Step 3: Field Officer Dashboard Component

**Purpose:** Show cases assigned to field officer

**Component:**
```typescript
export class FieldOfficerDashboardComponent implements OnInit {
  myCases: CaseDTO[] = [];
  pendingReports: CaseDTO[] = [];
  loading = false;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadMyCases();
  }

  // Load cases assigned to this field officer
  async loadMyCases() {
    this.loading = true;
    try {
      const response = await this.http.get<ApiResponse<CaseDTO[]>>(
        '/api/cases/my-cases'
      ).toPromise();
      
      this.myCases = response.data;
      
      // Filter cases in FIELD_REPORT_REQUESTED state
      this.pendingReports = this.myCases.filter(
        c => c.currentStateCode === 'FIELD_REPORT_REQUESTED'
      );
    } catch (error) {
      console.error('Error loading cases:', error);
    } finally {
      this.loading = false;
    }
  }

  // Open case detail to submit report
  openCaseDetail(caseId: number) {
    this.router.navigate(['/cases', caseId]);
  }
}
```

**Template:**
```html
<div class="dashboard-container">
  <h2>My Assigned Cases</h2>
  
  <!-- Pending Field Reports Section -->
  <div class="section" *ngIf="pendingReports.length > 0">
    <h3>Pending Field Reports ({{ pendingReports.length }})</h3>
    <mat-card *ngFor="let case of pendingReports" class="case-card">
      <mat-card-header>
        <mat-card-title>{{ case.caseNumber }}</mat-card-title>
        <mat-card-subtitle>{{ case.subject }}</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p><strong>State:</strong> {{ case.currentStateName }}</p>
        <p><strong>Application Date:</strong> {{ case.applicationDate }}</p>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button color="primary" (click)="openCaseDetail(case.id)">
          Submit Report
        </button>
      </mat-card-actions>
    </mat-card>
  </div>

  <!-- All Cases Section -->
  <div class="section">
    <h3>All Assigned Cases ({{ myCases.length }})</h3>
    <mat-card *ngFor="let case of myCases" class="case-card">
      <mat-card-header>
        <mat-card-title>{{ case.caseNumber }}</mat-card-title>
        <mat-card-subtitle>{{ case.subject }}</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p><strong>State:</strong> {{ case.currentStateName }}</p>
        <p><strong>Status:</strong> {{ case.status }}</p>
      </mat-card-content>
      <mat-card-actions>
        <button mat-button (click)="openCaseDetail(case.id)">View Details</button>
      </mat-card-actions>
    </mat-card>
  </div>
</div>
```

---

### Step 4: Field Report Submission (Case Detail Page)

**Purpose:** Field officer submits field report form and executes transition

**Component Logic:**
```typescript
export class CaseDetailComponent {
  caseId: number;
  case: CaseDTO;
  availableTransitions: WorkflowTransitionDTO[] = [];
  showSubmitReportButton = false;
  fieldReportForm: FormGroup;

  ngOnInit() {
    this.loadCaseDetails();
    this.loadAvailableTransitions();
    this.initFieldReportForm();
  }

  // Check if SUBMIT_FIELD_REPORT transition is available
  loadAvailableTransitions() {
    this.http.get<ApiResponse<WorkflowTransitionDTO[]>>(
      `/api/cases/${this.caseId}/transitions`
    ).subscribe(response => {
      this.availableTransitions = response.data;
      this.showSubmitReportButton = this.availableTransitions.some(
        t => t.transitionCode === 'SUBMIT_FIELD_REPORT' && t.checklist.canExecute
      );
    });
  }

  // Submit field report
  async submitFieldReport() {
    if (this.fieldReportForm.invalid) {
      return;
    }

    try {
      // Step 1: Submit module form (if you have FIELD_REPORT module form)
      const formData = this.fieldReportForm.value;
      await this.http.post(
        `/api/cases/${this.caseId}/module-forms/FIELD_REPORT`,
        {
          formData: formData,
          remarks: formData.remarks
        }
      ).toPromise();

      // Step 2: Execute transition
      await this.http.post(
        `/api/cases/${this.caseId}/transitions/execute`,
        {
          transitionCode: 'SUBMIT_FIELD_REPORT',
          comments: 'Field report submitted'
        }
      ).toPromise();

      this.showSuccess('Field report submitted successfully');
      this.loadCaseDetails();
      this.loadAvailableTransitions();
    } catch (error) {
      console.error('Error submitting report:', error);
      this.showError('Failed to submit report');
    }
  }
}
```

---

## Key Points for Frontend Developers

### 1. Dynamic Officer Loading

- **Always load officers dynamically** from API - never hardcode
- Use `unitId` from Tehsildar's posting (district level)
- API automatically finds all field officers in child units (Circles)

### 2. Workflow Alignment

- **Check transitions** before showing buttons
- Use `GET /api/cases/{caseId}/transitions` to check if action is available
- Only show "Request Field Report" if `REQUEST_FIELD_REPORT` transition exists and `canExecute = true`

### 3. Two-Step Process

- **Step 1:** Execute transition (`REQUEST_FIELD_REPORT`)
- **Step 2:** Assign case to officer
- Both steps must succeed for complete workflow

### 4. Field Officer Visibility

- Field officers see cases via `GET /api/cases/my-cases`
- Filter by `currentStateCode === 'FIELD_REPORT_REQUESTED'` for pending reports
- After submission, case auto-assigns back to Tehsildar

### 5. Error Handling

- Handle cases where no field officers are available
- Handle transition execution failures
- Handle assignment failures
- Show appropriate error messages to user

---

## Example API Call Sequence

### Complete Flow: Request Field Report

```
1. User clicks "Request Field Report"
   ↓
2. GET /api/cases/{caseId}/transitions
   → Check if REQUEST_FIELD_REPORT is available
   ↓
3. GET /api/admin/postings/field-officers/unit/{unitId}
   → Load all field officers below Tehsildar
   ↓
4. User selects officer + enters comments
   ↓
5. POST /api/cases/{caseId}/transitions/execute
   {
     "transitionCode": "REQUEST_FIELD_REPORT",
     "comments": "..."
   }
   → Execute transition
   ↓
6. PUT /api/admin/cases/{caseId}/assign-officer
   {
     "officerId": 25,
     "roleCode": "PATWARI"
   }
   → Assign case to officer
   ↓
7. GET /api/cases/{caseId}
   → Refresh case details
   ↓
8. Done! Case now shows:
   - State: FIELD_REPORT_REQUESTED
   - Assigned to: Patwari Name
```

---

## Testing Checklist

- [ ] "Request Field Report" button shows only when transition is available
- [ ] Dialog loads all field officers below Tehsildar's unit
- [ ] Officers are grouped by role (PATWARI, KANUNGO, etc.)
- [ ] User can select any officer
- [ ] Transition executes successfully
- [ ] Case assigns to selected officer
- [ ] Field officer sees case in their dashboard
- [ ] Field officer can submit report
- [ ] After submission, Tehsildar sees case again
- [ ] Tehsildar can review and continue proceedings

---

## Summary

This implementation provides a **fully dynamic** field report workflow that:

1. ✅ Shows all field officers below Tehsildar (not hardcoded)
2. ✅ Works with any role (PATWARI, KANUNGO, etc.)
3. ✅ Aligns with workflow system (uses transitions)
4. ✅ Handles case assignment automatically
5. ✅ Provides visibility to both Tehsildar and field officers

The system is **scalable** and **maintainable** - adding new field officer roles or units requires no code changes, only configuration.
