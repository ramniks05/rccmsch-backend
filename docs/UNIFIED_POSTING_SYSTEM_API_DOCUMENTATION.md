# Unified Posting System API Documentation

## Overview

The Unified Posting System supports **TWO types of officer postings**:

1. **Court-Based Postings**: Officers directly posted to courts (TEHSILDAR, READER, etc.)
2. **Unit-Based Postings**: Field officers assigned to administrative units (PATWARI, KANUNGO, etc.)

Both types support login with UserID and password, but serve different purposes in the case management workflow.

---

## Table of Contents

1. [Business Logic Overview](#business-logic-overview)
2. [Posting Types](#posting-types)
3. [API Endpoints](#api-endpoints)
4. [Frontend Implementation Guide](#frontend-implementation-guide)
5. [Use Cases](#use-cases)

---

## Business Logic Overview

### Posting Types

#### 1. Court-Based Postings
- **Purpose**: Officers who work directly at court level
- **Examples**: TEHSILDAR, READER
- **UserID Format**: `ROLE_CODE@COURT_CODE` (e.g., `TEHSILDAR@CHD_TEHSILDAR_COURT`)
- **Case Assignment**: Auto-assigned to cases based on workflow
- **Use Case**: Court-level case processing, approvals, hearings

#### 2. Unit-Based Postings
- **Purpose**: Field officers who work at administrative unit level
- **Examples**: PATWARI, KANUNGO
- **UserID Format**: `ROLE_CODE@UNIT_LGD_CODE` (e.g., `PATWARI@400101`)
- **Case Assignment**: Manually assigned by court officers when field work is needed
- **Use Case**: Field verification, report entry, document uploading

### Key Differences

| Feature | Court-Based | Unit-Based |
|---------|------------|------------|
| **Linked To** | Court | Administrative Unit |
| **UserID Format** | `ROLE@COURT_CODE` | `ROLE@UNIT_LGD_CODE` |
| **Auto-Assignment** | Yes (by workflow) | No (manual only) |
| **Login** | Yes | Yes |
| **Case Access** | All cases in court | Cases assigned to them or their unit |
| **Primary Function** | Court work | Field work |

---

## API Endpoints

### Base URL
```
http://localhost:8080/api/admin
```

### Authentication
All endpoints require JWT token:
```
Authorization: Bearer <token>
```

---

### 1. Assign Person to Post (Unified)

**Endpoint:** `POST /api/admin/postings`

**Description:** Assign an officer to either a court (court-based) or unit (unit-based posting).

**Request Body:**

#### For Court-Based Posting:
```json
{
  "courtId": 1,
  "unitId": null,
  "roleCode": "TEHSILDAR",
  "officerId": 1
}
```

#### For Unit-Based Posting:
```json
{
  "courtId": null,
  "unitId": 2,
  "roleCode": "PATWARI",
  "officerId": 3
}
```

**Validation Rules:**
- Either `courtId` OR `unitId` must be provided (not both, not neither)
- `roleCode` is required
- `officerId` is required
- For court-based: `courtId` must exist
- For unit-based: `unitId` must exist, `courtId` must be null

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Person assigned to post successfully. UserID and temporary password generated.",
  "data": {
    "id": 1,
    "courtId": 1,
    "courtName": "Court of Tehsildar (Revenue)-cum-Assistant Collector/Grade-I/II, UT Chandigarh",
    "courtCode": "CHD_TEHSILDAR_COURT",
    "courtLevel": "DISTRICT",
    "courtType": "REVENUE_COURT",
    "unitId": 2,
    "unitName": "Chandigarh District",
    "unitCode": "CHD_DIST_01",
    "unitLgdCode": "400001",
    "roleCode": "TEHSILDAR",
    "roleName": "Tehsildar (Revenue)-cum-Assistant Collector/Grade-I/II",
    "officerId": 1,
    "officerName": "Tehsildar Officer Name",
    "mobileNo": "9876543210",
    "postingUserid": "TEHSILDAR@CHD_TEHSILDAR_COURT",
    "postingType": "COURT_BASED",
    "fromDate": "2026-01-09",
    "toDate": null,
    "isCurrent": true
  }
}
```

**For Unit-Based Posting Response:**
```json
{
  "success": true,
  "message": "Person assigned to post successfully. UserID and temporary password generated.",
  "data": {
    "id": 2,
    "courtId": null,
    "courtName": null,
    "courtCode": null,
    "courtLevel": null,
    "courtType": null,
    "unitId": 3,
    "unitName": "Chandigarh Circle 1",
    "unitCode": "CHD_CIRCLE_01",
    "unitLgdCode": "400101",
    "roleCode": "PATWARI",
    "roleName": "Patwari",
    "officerId": 3,
    "officerName": "Patwari Officer Name",
    "mobileNo": "9876543212",
    "postingUserid": "PATWARI@400101",
    "postingType": "UNIT_BASED",
    "fromDate": "2026-01-09",
    "toDate": null,
    "isCurrent": true
  }
}
```

**Error Responses:**

**400 Bad Request - Both courtId and unitId provided:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot provide both courtId and unitId. Provide either courtId (court-based) or unitId (unit-based)",
  "path": "/api/admin/postings"
}
```

**400 Bad Request - Neither provided:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Either courtId (court-based posting) or unitId (unit-based posting) must be provided",
  "path": "/api/admin/postings"
}
```

---

### 2. Get Field Officers for Court

**Endpoint:** `GET /api/admin/postings/field-officers/court/{courtId}?roleCode={roleCode}`

**Description:** Find field officers (unit-based postings) available to a court. Searches unit hierarchy to find officers in units under the court's jurisdiction.

**Path Parameters:**
- `courtId` (required) - Court ID

**Query Parameters:**
- `roleCode` (required) - Role code to search (e.g., "PATWARI", "KANUNGO")

**Example:**
```
GET /api/admin/postings/field-officers/court/1?roleCode=PATWARI
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Field officers retrieved successfully",
  "data": [
    {
      "id": 2,
      "courtId": null,
      "unitId": 3,
      "unitName": "Chandigarh Circle 1",
      "unitCode": "CHD_CIRCLE_01",
      "unitLgdCode": "400101",
      "roleCode": "PATWARI",
      "roleName": "Patwari",
      "officerId": 3,
      "officerName": "Patwari Officer Name",
      "mobileNo": "9876543212",
      "postingUserid": "PATWARI@400101",
      "postingType": "UNIT_BASED",
      "fromDate": "2026-01-09",
      "toDate": null,
      "isCurrent": true
    }
  ]
}
```

**Business Logic:**
- Searches for unit-based postings (where `courtId IS NULL`)
- Filters by `roleCode`
- Finds officers in units that are:
  - Directly under the court's unit (child units)
  - Or the court's unit itself
  - Or parent units (if court is at lower level)

---

### 3. Get Field Officers by Role

**Endpoint:** `GET /api/admin/postings/field-officers/role/{roleCode}`

**Description:** Get all unit-based postings (field officers) for a specific role across all units.

**Path Parameters:**
- `roleCode` (required) - Role code (e.g., "PATWARI", "KANUNGO")

**Example:**
```
GET /api/admin/postings/field-officers/role/PATWARI
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Field officers retrieved successfully",
  "data": [
    {
      "id": 2,
      "courtId": null,
      "unitId": 3,
      "unitName": "Chandigarh Circle 1",
      "unitCode": "CHD_CIRCLE_01",
      "unitLgdCode": "400101",
      "roleCode": "PATWARI",
      "roleName": "Patwari",
      "officerId": 3,
      "officerName": "Patwari Officer Name",
      "mobileNo": "9876543212",
      "postingUserid": "PATWARI@400101",
      "postingType": "UNIT_BASED",
      "fromDate": "2026-01-09",
      "toDate": null,
      "isCurrent": true
    }
  ]
}
```

---

### 4. Get Field Officers by Unit and Role

**Endpoint:** `GET /api/admin/postings/field-officers/unit/{unitId}/role/{roleCode}`

**Description:** Get unit-based postings for a specific unit and role.

**Path Parameters:**
- `unitId` (required) - Administrative unit ID
- `roleCode` (required) - Role code

**Example:**
```
GET /api/admin/postings/field-officers/unit/3/role/PATWARI
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Field officers retrieved successfully",
  "data": [
    {
      "id": 2,
      "courtId": null,
      "unitId": 3,
      "unitName": "Chandigarh Circle 1",
      "unitCode": "CHD_CIRCLE_01",
      "unitLgdCode": "400101",
      "roleCode": "PATWARI",
      "roleName": "Patwari",
      "officerId": 3,
      "officerName": "Patwari Officer Name",
      "mobileNo": "9876543212",
      "postingUserid": "PATWARI@400101",
      "postingType": "UNIT_BASED",
      "fromDate": "2026-01-09",
      "toDate": null,
      "isCurrent": true
    }
  ]
}
```

---

### 5. Get Active Postings by Unit

**Endpoint:** `GET /api/admin/postings/unit/{unitId}/active`

**Description:** Get all active postings for a unit. Returns both:
- Court-based postings (courts in this unit)
- Unit-based postings (directly assigned to this unit)

**Path Parameters:**
- `unitId` (required) - Administrative unit ID

**Example:**
```
GET /api/admin/postings/unit/2/active
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Active postings retrieved successfully",
  "data": [
    {
      "id": 1,
      "courtId": 1,
      "courtName": "Court of Tehsildar...",
      "postingType": "COURT_BASED",
      "roleCode": "TEHSILDAR",
      "officerName": "Tehsildar Officer Name",
      "postingUserid": "TEHSILDAR@CHD_TEHSILDAR_COURT",
      "isCurrent": true
    },
    {
      "id": 2,
      "courtId": null,
      "postingType": "UNIT_BASED",
      "roleCode": "PATWARI",
      "officerName": "Patwari Officer Name",
      "postingUserid": "PATWARI@400101",
      "isCurrent": true
    }
  ]
}
```

---

### 6. Get All Active Postings

**Endpoint:** `GET /api/admin/postings/active`

**Description:** Get all active postings (both court-based and unit-based).

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Active postings retrieved successfully",
  "data": [
    {
      "id": 1,
      "postingType": "COURT_BASED",
      "postingUserid": "TEHSILDAR@CHD_TEHSILDAR_COURT",
      ...
    },
    {
      "id": 2,
      "postingType": "UNIT_BASED",
      "postingUserid": "PATWARI@400101",
      ...
    }
  ]
}
```

---

### 7. Get Posting by UserID

**Endpoint:** `GET /api/admin/postings/userid/{userid}`

**Description:** Get active posting by UserID. Works for both court-based and unit-based UserIDs.

**Path Parameters:**
- `userid` (required) - UserID (e.g., `TEHSILDAR@CHD_TEHSILDAR_COURT` or `PATWARI@400101`)

**Example:**
```
GET /api/admin/postings/userid/PATWARI@400101
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Posting retrieved successfully",
  "data": {
    "id": 2,
    "courtId": null,
    "unitId": 3,
    "postingType": "UNIT_BASED",
    "postingUserid": "PATWARI@400101",
    ...
  }
}
```

---

## Frontend Implementation Guide

### 1. Posting Assignment Form

#### Step 1: Determine Posting Type

```typescript
// In your Angular component
postingType: 'COURT' | 'UNIT' = 'COURT';

onPostingTypeChange(type: 'COURT' | 'UNIT') {
  this.postingType = type;
  // Clear the other field
  if (type === 'COURT') {
    this.postingForm.patchValue({ unitId: null });
  } else {
    this.postingForm.patchValue({ courtId: null });
  }
}
```

#### Step 2: Form Structure

```typescript
postingForm = this.fb.group({
  postingType: ['COURT', Validators.required], // Radio button: COURT or UNIT
  courtId: [null], // Required if postingType === 'COURT'
  unitId: [null], // Required if postingType === 'UNIT'
  roleCode: ['', Validators.required],
  officerId: ['', Validators.required]
});

// Conditional validation
get courtIdControl() {
  return this.postingForm.get('courtId');
}

get unitIdControl() {
  return this.postingForm.get('unitId');
}

ngOnInit() {
  // Watch posting type changes
  this.postingForm.get('postingType')?.valueChanges.subscribe(type => {
    if (type === 'COURT') {
      this.courtIdControl?.setValidators([Validators.required]);
      this.unitIdControl?.clearValidators();
      this.unitIdControl?.setValue(null);
    } else {
      this.unitIdControl?.setValidators([Validators.required]);
      this.courtIdControl?.clearValidators();
      this.courtIdControl?.setValue(null);
    }
    this.courtIdControl?.updateValueAndValidity();
    this.unitIdControl?.updateValueAndValidity();
  });
}
```

#### Step 3: Get Available Roles

**For Court-Based Posting:**
```typescript
onCourtSelected(courtId: number) {
  // Get all roles (no filtering by level)
  this.roleService.getAllRoles().subscribe(roles => {
    this.availableRoles = roles;
  });
}
```

**For Unit-Based Posting:**
```typescript
onUnitSelected(unitId: number) {
  // Get all roles (no filtering by level)
  this.roleService.getAllRoles().subscribe(roles => {
    this.availableRoles = roles;
  });
}
```

#### Step 4: Submit Posting

```typescript
submitPosting() {
  if (this.postingForm.valid) {
    const formValue = this.postingForm.value;
    
    const payload: PostingAssignmentDTO = {
      courtId: formValue.postingType === 'COURT' ? formValue.courtId : null,
      unitId: formValue.postingType === 'UNIT' ? formValue.unitId : null,
      roleCode: formValue.roleCode,
      officerId: formValue.officerId
    };

    this.postingService.assignPersonToPost(payload).subscribe({
      next: (response) => {
        if (response.success) {
          alert(`Posting created! UserID: ${response.data.postingUserid}`);
          // Show password info if needed
          this.loadPostings();
        }
      },
      error: (error) => {
        console.error('Error creating posting:', error);
        alert('Error: ' + (error.error?.message || 'Failed to create posting'));
      }
    });
  }
}
```

---

### 2. Search Field Officers (For Court Officers)

#### Use Case: Court officer needs to find Patwari for field work

```typescript
// In your case detail component
searchFieldOfficers(courtId: number, roleCode: string) {
  this.postingService.getFieldOfficersForCourt(courtId, roleCode)
    .subscribe({
      next: (response) => {
        if (response.success) {
          this.availableFieldOfficers = response.data;
          // Show in modal/dropdown for selection
        }
      }
    });
}

// Example: When Tehsildar needs Patwari
onNeedFieldOfficer() {
  const currentCourtId = this.currentUser.posting.courtId;
  this.searchFieldOfficers(currentCourtId, 'PATWARI');
}
```

#### Display Field Officers

```html
<div *ngIf="availableFieldOfficers.length > 0">
  <h3>Available Field Officers</h3>
  <table>
    <thead>
      <tr>
        <th>Officer Name</th>
        <th>Unit</th>
        <th>Mobile</th>
        <th>Action</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let officer of availableFieldOfficers">
        <td>{{ officer.officerName }}</td>
        <td>{{ officer.unitName }}</td>
        <td>{{ officer.mobileNo }}</td>
        <td>
          <button (click)="assignFieldOfficerToCase(officer)">
            Assign to Case
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

---

### 3. Login Implementation

#### Login Form

```typescript
loginForm = this.fb.group({
  userid: ['', [Validators.required, Validators.pattern(/^[A-Z_]+@.+$/)]],
  password: ['', Validators.required]
});

login() {
  if (this.loginForm.valid) {
    const credentials = this.loginForm.value;
    
    this.authService.postBasedLogin(credentials).subscribe({
      next: (response) => {
        if (response.success) {
          // Store token
          localStorage.setItem('token', response.data.token);
          
          // Check posting type
          const postingType = response.data.posting?.courtId ? 'COURT_BASED' : 'UNIT_BASED';
          
          // Redirect based on posting type
          if (postingType === 'COURT_BASED') {
            this.router.navigate(['/court-dashboard']);
          } else {
            this.router.navigate(['/field-officer-dashboard']);
          }
        }
      },
      error: (error) => {
        console.error('Login failed:', error);
        alert('Invalid UserID or password');
      }
    });
  }
}
```

#### UserID Format Detection

```typescript
detectPostingType(userid: string): 'COURT' | 'UNIT' {
  // UserID format: ROLE_CODE@<CODE>
  // If CODE is numeric (LGD code) -> UNIT-based
  // If CODE is alphanumeric (court code) -> COURT-based
  
  const parts = userid.split('@');
  if (parts.length !== 2) {
    throw new Error('Invalid UserID format');
  }
  
  const code = parts[1];
  // Check if code is numeric (LGD code)
  if (/^\d+$/.test(code)) {
    return 'UNIT';
  } else {
    return 'COURT';
  }
}
```

---

### 4. Dashboard Based on Posting Type

#### Court-Based Officer Dashboard

```typescript
// Show cases assigned to court
loadCourtCases() {
  this.caseService.getCasesByCourt(this.currentUser.posting.courtId)
    .subscribe(cases => {
      this.cases = cases;
    });
}
```

#### Unit-Based Officer Dashboard

```typescript
// Show cases assigned to unit or assigned to this officer
loadFieldOfficerCases() {
  // Option 1: Cases assigned to this officer
  this.caseService.getCasesAssignedToOfficer(this.currentUser.officerId)
    .subscribe(cases => {
      this.assignedCases = cases;
    });
  
  // Option 2: Cases in this unit (if needed)
  this.caseService.getCasesByUnit(this.currentUser.posting.unitId)
    .subscribe(cases => {
      this.unitCases = cases;
    });
}
```

---

## Use Cases

### Use Case 1: Assign Patwari to Circle Unit

**Scenario:** Admin wants to assign a Patwari to a Circle unit for field work.

**Steps:**
1. Admin navigates to "Posting Management"
2. Selects posting type: "UNIT"
3. Selects Circle unit from dropdown
4. Selects role: "PATWARI"
5. Selects officer
6. Submits form

**API Call:**
```json
POST /api/admin/postings
{
  "courtId": null,
  "unitId": 3,
  "roleCode": "PATWARI",
  "officerId": 3
}
```

**Result:**
- Patwari assigned to Circle unit
- UserID generated: `PATWARI@400101`
- Can login with this UserID
- Can upload field data for cases in this unit

---

### Use Case 2: Court Officer Needs Field Officer

**Scenario:** Tehsildar at District court needs Patwari for field verification of a case.

**Steps:**
1. Tehsildar opens case detail
2. Clicks "Assign Field Officer"
3. Selects role: "PATWARI"
4. System searches and shows available Patwaris
5. Tehsildar selects a Patwari
6. System assigns Patwari to case

**API Call:**
```
GET /api/admin/postings/field-officers/court/1?roleCode=PATWARI
```

**Response:** List of Patwaris in Circle units under District

**Then:** Manual case assignment API (to be implemented separately)

---

### Use Case 3: Field Officer Login

**Scenario:** Patwari logs in to upload field data.

**Steps:**
1. Patwari enters UserID: `PATWARI@400101`
2. Enters password
3. System authenticates
4. Redirects to field officer dashboard
5. Shows cases assigned to this officer or unit

**API Call:**
```json
POST /api/admin/auth/officer-login
{
  "userid": "PATWARI@400101",
  "password": "Rccms@2101"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "...",
    "posting": {
      "postingType": "UNIT_BASED",
      "unitId": 3,
      "unitName": "Chandigarh Circle 1",
      "roleCode": "PATWARI",
      ...
    }
  }
}
```

---

## Database Migration Notes

### Schema Changes Required

1. **Modify `officer_da_history` table:**
   ```sql
   -- Make court_id nullable
   ALTER TABLE officer_da_history ALTER COLUMN court_id DROP NOT NULL;
   
   -- Add unit_id column
   ALTER TABLE officer_da_history ADD COLUMN unit_id BIGINT;
   ALTER TABLE officer_da_history ADD CONSTRAINT fk_posting_unit 
     FOREIGN KEY (unit_id) REFERENCES admin_unit(unit_id);
   
   -- Add unique constraint for unit-based postings
   ALTER TABLE officer_da_history 
     ADD CONSTRAINT uk_posting_unit_role_current 
     UNIQUE (unit_id, role_code, is_current) 
     WHERE unit_id IS NOT NULL AND is_current = true;
   
   -- Add index for unit-based queries
   CREATE INDEX idx_unit_role ON officer_da_history(unit_id, role_code);
   CREATE INDEX idx_posting_type ON officer_da_history(court_id, unit_id);
   ```

2. **Data Migration (if needed):**
   - Existing court-based postings will continue to work
   - No data migration needed for existing records

---

## Summary

### Key Points for Frontend Developers

1. **Posting Type Selection:**
   - Provide radio buttons or toggle: "Court-Based" vs "Unit-Based"
   - Show/hide fields based on selection

2. **Role Dropdown:**
   - Show ALL roles (don't filter by level matching)
   - Let backend handle validation

3. **UserID Display:**
   - Show generated UserID after posting creation
   - Format: `ROLE@CODE` (CODE can be court code or LGD code)

4. **Field Officer Search:**
   - Use `/field-officers/court/{courtId}?roleCode={roleCode}` endpoint
   - Display in modal/dropdown for selection

5. **Login:**
   - Same login endpoint for both types
   - Check `postingType` in response to determine dashboard

6. **Dashboard:**
   - Court-based: Show court cases
   - Unit-based: Show assigned cases or unit cases

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-09  
**For:** Chandigarh RCCMS Implementation
