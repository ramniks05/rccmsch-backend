# RCCMS Admin API Documentation

## Base URL
```
http://localhost:8080
```

## Authentication
All admin endpoints (except login) require JWT token in Authorization header:
```
Authorization: Bearer <token>
```

---

## 1. Admin Authentication

### 1.1 Admin Login
**Endpoint:** `POST /api/admin/auth/login`

**Description:** Login with default admin credentials

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin@123"
}
```

**Default Credentials:**
- Username: `admin`
- Password: `admin@123`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 0,
    "citizenType": null,
    "email": null,
    "mobileNumber": null,
    "expiresIn": 3600
  },
  "timestamp": "2026-01-09T15:30:00"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/admin/auth/login"
}
```

---

## 2. Officer Management (Government Employees)

### 2.1 Create Officer
**Endpoint:** `POST /api/admin/officers`

**Description:** Create a new government employee/officer. Temporary password will be auto-generated.

**Request Body:**
```json
{
  "fullName": "John Doe",
  "mobileNo": "9876543210",
  "email": "john.doe@example.com"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Officer created successfully. Temporary password generated.",
  "data": {
    "id": 1,
    "fullName": "John Doe",
    "mobileNo": "9876543210",
    "email": "john.doe@example.com",
    "authType": "TEMP_PASSWORD",
    "isActive": true,
    "isPasswordResetRequired": true,
    "isMobileVerified": false
  }
}
```

**Note:** Temporary password format: `Rccms@<last4MobileDigits>` (e.g., `Rccms@3210`). Check console logs for the generated password.

### 2.2 Get All Officers
**Endpoint:** `GET /api/admin/officers`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Officers retrieved successfully",
  "data": [
    {
      "id": 1,
      "fullName": "John Doe",
      "mobileNo": "9876543210",
      "email": "john.doe@example.com",
      "authType": "TEMP_PASSWORD",
      "isActive": true,
      "isPasswordResetRequired": true,
      "isMobileVerified": false
    }
  ]
}
```

### 2.3 Get Officer by ID
**Endpoint:** `GET /api/admin/officers/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Officer retrieved successfully",
  "data": {
    "id": 1,
    "fullName": "John Doe",
    "mobileNo": "9876543210",
    "email": "john.doe@example.com",
    "authType": "TEMP_PASSWORD",
    "isActive": true,
    "isPasswordResetRequired": true,
    "isMobileVerified": false
  }
}
```

---

## 3. Posting Management (Assign Officers/DAs to Posts)

### 3.1 Assign Officer to Post
**Endpoint:** `POST /api/admin/postings`

**Description:** Assign an officer to a post (UNIT + ROLE). Existing active posting for same unit+role will be closed automatically.

**Request Body:**
```json
{
  "unitId": 1,
  "roleCode": "DISTRICT_OFFICER",
  "officerId": 1
}
```

**Role Codes:**
- `SUPER_ADMIN`
- `STATE_ADMIN`
- `DISTRICT_OFFICER`
- `SUB_DIVISION_OFFICER`
- `CIRCLE_OFFICER`
- `DEALING_ASSISTANT`

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Person assigned to post successfully. UserID and temporary password generated.",
  "data": {
    "id": 1,
    "unitId": 1,
    "unitName": "Imphal West",
    "unitLgdCode": "140001",
    "roleCode": "DISTRICT_OFFICER",
    "roleName": "District Officer",
    "officerId": 1,
    "officerName": "John Doe",
    "mobileNo": "9876543210",
    "postingUserid": "DISTRICT_OFFICER@140001",
    "fromDate": "2026-01-09",
    "toDate": null,
    "isCurrent": true
  }
}
```

**Note:** UserID format: `ROLE_CODE@UNIT_LGD_CODE` (e.g., `DISTRICT_OFFICER@140001`)

### 3.2 Transfer Officer to New Post
**Endpoint:** `PUT /api/admin/postings/transfer`

**Description:** Transfer an officer from current post to a new post. All active postings for the officer will be closed.

**Request Body:**
```json
{
  "unitId": 2,
  "roleCode": "SUB_DIVISION_OFFICER",
  "officerId": 1
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Person transferred successfully",
  "data": {
    "id": 2,
    "unitId": 2,
    "unitName": "Patsoi",
    "unitLgdCode": "140002",
    "roleCode": "SUB_DIVISION_OFFICER",
    "roleName": "Sub-Division Officer",
    "officerId": 1,
    "officerName": "John Doe",
    "mobileNo": "9876543210",
    "postingUserid": "SUB_DIVISION_OFFICER@140002",
    "fromDate": "2026-01-09",
    "toDate": null,
    "isCurrent": true
  }
}
```

### 3.3 Get Posting by UserID
**Endpoint:** `GET /api/admin/postings/userid/{userid}`

**Example:** `GET /api/admin/postings/userid/DISTRICT_OFFICER@140001`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Posting retrieved successfully",
  "data": {
    "id": 1,
    "unitId": 1,
    "unitName": "Imphal West",
    "unitLgdCode": "140001",
    "roleCode": "DISTRICT_OFFICER",
    "roleName": "District Officer",
    "officerId": 1,
    "officerName": "John Doe",
    "mobileNo": "9876543210",
    "postingUserid": "DISTRICT_OFFICER@140001",
    "fromDate": "2026-01-09",
    "toDate": null,
    "isCurrent": true
  }
}
```

### 3.4 Get Postings by Officer
**Endpoint:** `GET /api/admin/postings/officer/{officerId}`

**Description:** Get all postings (history) for an officer

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Postings retrieved successfully",
  "data": [
    {
      "id": 1,
      "unitId": 1,
      "unitName": "Imphal West",
      "unitLgdCode": "140001",
      "roleCode": "DISTRICT_OFFICER",
      "roleName": "District Officer",
      "officerId": 1,
      "officerName": "John Doe",
      "mobileNo": "9876543210",
      "postingUserid": "DISTRICT_OFFICER@140001",
      "fromDate": "2026-01-09",
      "toDate": "2026-01-10",
      "isCurrent": false
    },
    {
      "id": 2,
      "unitId": 2,
      "unitName": "Patsoi",
      "unitLgdCode": "140002",
      "roleCode": "SUB_DIVISION_OFFICER",
      "roleName": "Sub-Division Officer",
      "officerId": 1,
      "officerName": "John Doe",
      "mobileNo": "9876543210",
      "postingUserid": "SUB_DIVISION_OFFICER@140002",
      "fromDate": "2026-01-10",
      "toDate": null,
      "isCurrent": true
    }
  ]
}
```

### 3.5 Get Postings by Unit
**Endpoint:** `GET /api/admin/postings/unit/{unitId}`

**Description:** Get all postings (history) for a unit

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Postings retrieved successfully",
  "data": [
    {
      "id": 1,
      "unitId": 1,
      "unitName": "Imphal West",
      "unitLgdCode": "140001",
      "roleCode": "DISTRICT_OFFICER",
      "roleName": "District Officer",
      "officerId": 1,
      "officerName": "John Doe",
      "mobileNo": "9876543210",
      "postingUserid": "DISTRICT_OFFICER@140001",
      "fromDate": "2026-01-09",
      "toDate": null,
      "isCurrent": true
    }
  ]
}
```

### 3.6 Get All Active Postings
**Endpoint:** `GET /api/admin/postings/active`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Active postings retrieved successfully",
  "data": [
    {
      "id": 1,
      "unitId": 1,
      "unitName": "Imphal West",
      "unitLgdCode": "140001",
      "roleCode": "DISTRICT_OFFICER",
      "roleName": "District Officer",
      "officerId": 1,
      "officerName": "John Doe",
      "mobileNo": "9876543210",
      "postingUserid": "DISTRICT_OFFICER@140001",
      "fromDate": "2026-01-09",
      "toDate": null,
      "isCurrent": true
    }
  ]
}
```

### 3.7 Close Posting
**Endpoint:** `PUT /api/admin/postings/{id}/close`

**Description:** Close an active posting (sets toDate and isCurrent=false)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Posting closed successfully",
  "data": {
    "message": "Posting closed successfully",
    "id": 1
  }
}
```

---

## 4. Administrative Units Management

### 4.1 Create Administrative Unit
**Endpoint:** `POST /api/admin-units`

**Request Body:**
```json
{
  "unitCode": "IMW001",
  "unitName": "Imphal West",
  "unitLevel": "DISTRICT",
  "lgdCode": 140001,
  "parentUnitId": 1,
  "isActive": true
}
```

**Unit Levels:**
- `STATE`
- `DISTRICT`
- `SUB_DIVISION`
- `CIRCLE`

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Admin unit created successfully",
  "data": {
    "unitId": 1,
    "unitCode": "IMW001",
    "unitName": "Imphal West",
    "unitLevel": "DISTRICT",
    "lgdCode": 140001,
    "parentUnitId": 1,
    "parentUnitName": "Manipur",
    "isActive": true
  }
}
```

### 4.2 Get All Administrative Units
**Endpoint:** `GET /api/admin-units`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin units retrieved successfully",
  "data": [
    {
      "unitId": 1,
      "unitCode": "MNSTATE01",
      "unitName": "Manipur",
      "unitLevel": "STATE",
      "lgdCode": 140000,
      "parentUnitId": null,
      "parentUnitName": null,
      "isActive": true
    }
  ]
}
```

### 4.3 Get Active Administrative Units
**Endpoint:** `GET /api/admin-units/active`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Active admin units retrieved successfully",
  "data": [
    {
      "unitId": 1,
      "unitCode": "MNSTATE01",
      "unitName": "Manipur",
      "unitLevel": "STATE",
      "lgdCode": 140000,
      "parentUnitId": null,
      "parentUnitName": null,
      "isActive": true
    }
  ]
}
```

### 4.4 Get Administrative Units by Level
**Endpoint:** `GET /api/admin-units/level/{level}`

**Example:** `GET /api/admin-units/level/DISTRICT`

**Level Values:** `STATE`, `DISTRICT`, `SUB_DIVISION`, `CIRCLE`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin units retrieved successfully",
  "data": [
    {
      "unitId": 1,
      "unitCode": "IMW001",
      "unitName": "Imphal West",
      "unitLevel": "DISTRICT",
      "lgdCode": 140001,
      "parentUnitId": 1,
      "parentUnitName": "Manipur",
      "isActive": true
    }
  ]
}
```

### 4.5 Get Administrative Units by Parent
**Endpoint:** `GET /api/admin-units/parent/{parentId}`

**Description:** Get child units for a parent unit

**Example:** `GET /api/admin-units/parent/1`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin units retrieved successfully",
  "data": [
    {
      "unitId": 2,
      "unitCode": "IMW001",
      "unitName": "Imphal West",
      "unitLevel": "DISTRICT",
      "lgdCode": 140001,
      "parentUnitId": 1,
      "parentUnitName": "Manipur",
      "isActive": true
    }
  ]
}
```

### 4.6 Get Root Units (State Level)
**Endpoint:** `GET /api/admin-units/root`

**Description:** Get all state-level units (no parent)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Root admin units retrieved successfully",
  "data": [
    {
      "unitId": 1,
      "unitCode": "MNSTATE01",
      "unitName": "Manipur",
      "unitLevel": "STATE",
      "lgdCode": 140000,
      "parentUnitId": null,
      "parentUnitName": null,
      "isActive": true
    }
  ]
}
```

### 4.7 Get Administrative Unit by ID
**Endpoint:** `GET /api/admin-units/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin unit retrieved successfully",
  "data": {
    "unitId": 1,
    "unitCode": "MNSTATE01",
    "unitName": "Manipur",
    "unitLevel": "STATE",
    "lgdCode": 140000,
    "parentUnitId": null,
    "parentUnitName": null,
    "isActive": true
  }
}
```

### 4.8 Get Administrative Unit by Code
**Endpoint:** `GET /api/admin-units/code/{code}`

**Example:** `GET /api/admin-units/code/IMW001`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin unit retrieved successfully",
  "data": {
    "unitId": 1,
    "unitCode": "IMW001",
    "unitName": "Imphal West",
    "unitLevel": "DISTRICT",
    "lgdCode": 140001,
    "parentUnitId": 1,
    "parentUnitName": "Manipur",
    "isActive": true
  }
}
```

### 4.9 Update Administrative Unit
**Endpoint:** `PUT /api/admin-units/{id}`

**Request Body:**
```json
{
  "unitCode": "IMW001",
  "unitName": "Imphal West Updated",
  "unitLevel": "DISTRICT",
  "lgdCode": 140001,
  "parentUnitId": 1,
  "isActive": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin unit updated successfully",
  "data": {
    "unitId": 1,
    "unitCode": "IMW001",
    "unitName": "Imphal West Updated",
    "unitLevel": "DISTRICT",
    "lgdCode": 140001,
    "parentUnitId": 1,
    "parentUnitName": "Manipur",
    "isActive": true
  }
}
```

### 4.10 Delete Administrative Unit (Soft Delete)
**Endpoint:** `DELETE /api/admin-units/{id}`

**Description:** Sets `isActive = false`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Admin unit deleted successfully",
  "data": {
    "message": "Admin unit deleted successfully",
    "id": 1
  }
}
```

---

## 5. Case Types Management

### 5.1 Create Case Type
**Endpoint:** `POST /api/case-types`

**Request Body:**
```json
{
  "name": "Mutation (after Gift/Sale Deeds)",
  "code": "MUTATION_GIFT_SALE",
  "description": "Mutation after gift or sale deeds",
  "isActive": true
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Case type created successfully",
  "data": {
    "id": 1,
    "name": "Mutation (after Gift/Sale Deeds)",
    "code": "MUTATION_GIFT_SALE",
    "description": "Mutation after gift or sale deeds",
    "isActive": true
  }
}
```

### 5.2 Get All Case Types
**Endpoint:** `GET /api/case-types`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Case types retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Mutation (after Gift/Sale Deeds)",
      "code": "MUTATION_GIFT_SALE",
      "description": "Mutation after gift or sale deeds",
      "isActive": true
    }
  ]
}
```

### 5.3 Get Active Case Types
**Endpoint:** `GET /api/case-types/active`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Active case types retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Mutation (after Gift/Sale Deeds)",
      "code": "MUTATION_GIFT_SALE",
      "description": "Mutation after gift or sale deeds",
      "isActive": true
    }
  ]
}
```

### 5.4 Get Case Type by ID
**Endpoint:** `GET /api/case-types/{id}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Case type retrieved successfully",
  "data": {
    "id": 1,
    "name": "Mutation (after Gift/Sale Deeds)",
    "code": "MUTATION_GIFT_SALE",
    "description": "Mutation after gift or sale deeds",
    "isActive": true
  }
}
```

### 5.5 Get Case Type by Code
**Endpoint:** `GET /api/case-types/code/{code}`

**Example:** `GET /api/case-types/code/MUTATION_GIFT_SALE`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Case type retrieved successfully",
  "data": {
    "id": 1,
    "name": "Mutation (after Gift/Sale Deeds)",
    "code": "MUTATION_GIFT_SALE",
    "description": "Mutation after gift or sale deeds",
    "isActive": true
  }
}
```

### 5.6 Update Case Type
**Endpoint:** `PUT /api/case-types/{id}`

**Request Body:**
```json
{
  "name": "Mutation (after Gift/Sale Deeds) - Updated",
  "code": "MUTATION_GIFT_SALE",
  "description": "Updated description",
  "isActive": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Case type updated successfully",
  "data": {
    "id": 1,
    "name": "Mutation (after Gift/Sale Deeds) - Updated",
    "code": "MUTATION_GIFT_SALE",
    "description": "Updated description",
    "isActive": true
  }
}
```

### 5.7 Delete Case Type (Soft Delete)
**Endpoint:** `DELETE /api/case-types/{id}`

**Description:** Sets `isActive = false`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Case type deleted successfully",
  "data": {
    "message": "Case type deleted successfully",
    "id": 1
  }
}
```

### 5.8 Hard Delete Case Type
**Endpoint:** `DELETE /api/case-types/{id}/hard`

**Description:** Permanent deletion from database

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Case type permanently deleted",
  "data": {
    "message": "Case type permanently deleted",
    "id": 1
  }
}
```

---

## 6. Officer/DA Authentication (Admin can test)

### 6.1 Officer/DA Login
**Endpoint:** `POST /api/admin/auth/officer-login`

**Request Body:**
```json
{
  "userid": "DISTRICT_OFFICER@140001",
  "password": "Rccms@3210"
}
```

**UserID Format:** `ROLE_CODE@UNIT_LGD_CODE`

**Examples:**
- `SUPER_ADMIN@MNSTATE01`
- `STATE_ADMIN@MNSTATE01`
- `DISTRICT_OFFICER@IMW001`
- `SUB_DIVISION_OFFICER@LAM001`
- `CIRCLE_OFFICER@SEK001`
- `DEALING_ASSISTANT@SEK001`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Officer login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "citizenType": null,
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

**First Login Response (401 - Password Reset Required):**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Password reset required. Please reset your password.",
  "path": "/api/admin/auth/officer-login"
}
```

### 6.2 Reset Password (First Login)
**Endpoint:** `POST /api/admin/auth/reset-password`

**Request Body:**
```json
{
  "userid": "DISTRICT_OFFICER@140001",
  "newPassword": "NewSecurePass@123",
  "confirmPassword": "NewSecurePass@123"
}
```

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character (@$!%*?&)
- `newPassword` and `confirmPassword` must match

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": {
    "message": "Password reset successfully",
    "userid": "DISTRICT_OFFICER@140001"
  }
}
```

### 6.3 Verify Mobile with OTP
**Endpoint:** `POST /api/admin/auth/verify-mobile`

**Request Body:**
```json
{
  "userid": "DISTRICT_OFFICER@140001",
  "otp": "123456"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Mobile number verified successfully",
  "data": {
    "message": "Mobile number verified successfully",
    "userid": "DISTRICT_OFFICER@140001"
  }
}
```

---

## Frontend Implementation Guide (Angular/TypeScript)

### Service Example

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl = 'http://localhost:8080/api';
  private token: string | null = null;

  constructor(private http: HttpClient) {
    this.token = localStorage.getItem('adminToken');
  }

  // Admin Login
  adminLogin(username: string, password: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin/auth/login`, {
      username,
      password
    });
  }

  // Get Auth Headers
  private getAuthHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${this.token}`
    });
  }

  // Officer Management
  createOfficer(officer: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin/officers`, officer, {
      headers: this.getAuthHeaders()
    });
  }

  getAllOfficers(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/officers`, {
      headers: this.getAuthHeaders()
    });
  }

  getOfficerById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/officers/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  // Posting Management
  assignOfficerToPost(posting: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin/postings`, posting, {
      headers: this.getAuthHeaders()
    });
  }

  transferOfficer(posting: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/admin/postings/transfer`, posting, {
      headers: this.getAuthHeaders()
    });
  }

  getPostingByUserid(userid: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/postings/userid/${userid}`, {
      headers: this.getAuthHeaders()
    });
  }

  getPostingsByOfficer(officerId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/postings/officer/${officerId}`, {
      headers: this.getAuthHeaders()
    });
  }

  getPostingsByUnit(unitId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/postings/unit/${unitId}`, {
      headers: this.getAuthHeaders()
    });
  }

  getAllActivePostings(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/postings/active`, {
      headers: this.getAuthHeaders()
    });
  }

  closePosting(postingId: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/admin/postings/${postingId}/close`, {}, {
      headers: this.getAuthHeaders()
    });
  }

  // Administrative Units Management
  createAdminUnit(unit: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin-units`, unit, {
      headers: this.getAuthHeaders()
    });
  }

  getAllAdminUnits(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin-units`, {
      headers: this.getAuthHeaders()
    });
  }

  getAdminUnitsByLevel(level: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin-units/level/${level}`, {
      headers: this.getAuthHeaders()
    });
  }

  getAdminUnitsByParent(parentId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin-units/parent/${parentId}`, {
      headers: this.getAuthHeaders()
    });
  }

  getRootUnits(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin-units/root`, {
      headers: this.getAuthHeaders()
    });
  }

  getAdminUnitById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin-units/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  updateAdminUnit(id: number, unit: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/admin-units/${id}`, unit, {
      headers: this.getAuthHeaders()
    });
  }

  deleteAdminUnit(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/admin-units/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  // Case Types Management
  createCaseType(caseType: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/case-types`, caseType, {
      headers: this.getAuthHeaders()
    });
  }

  getAllCaseTypes(): Observable<any> {
    return this.http.get(`${this.baseUrl}/case-types`, {
      headers: this.getAuthHeaders()
    });
  }

  getActiveCaseTypes(): Observable<any> {
    return this.http.get(`${this.baseUrl}/case-types/active`, {
      headers: this.getAuthHeaders()
    });
  }

  getCaseTypeById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/case-types/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  updateCaseType(id: number, caseType: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/case-types/${id}`, caseType, {
      headers: this.getAuthHeaders()
    });
  }

  deleteCaseType(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/case-types/${id}`, {
      headers: this.getAuthHeaders()
    });
  }
}
```

### Component Example

```typescript
import { Component, OnInit } from '@angular/core';
import { AdminService } from './admin.service';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {
  officers: any[] = [];
  postings: any[] = [];
  adminUnits: any[] = [];
  caseTypes: any[] = [];

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.loadData();
  }

  // Admin Login
  login(username: string, password: string) {
    this.adminService.adminLogin(username, password).subscribe({
      next: (response) => {
        if (response.success) {
          localStorage.setItem('adminToken', response.data.token);
          localStorage.setItem('refreshToken', response.data.refreshToken);
          // Redirect to admin dashboard
        }
      },
      error: (error) => {
        console.error('Login failed:', error);
      }
    });
  }

  // Load all data
  loadData() {
    this.loadOfficers();
    this.loadPostings();
    this.loadAdminUnits();
    this.loadCaseTypes();
  }

  // Officer Management
  loadOfficers() {
    this.adminService.getAllOfficers().subscribe({
      next: (response) => {
        if (response.success) {
          this.officers = response.data;
        }
      }
    });
  }

  createOfficer(officer: any) {
    this.adminService.createOfficer(officer).subscribe({
      next: (response) => {
        if (response.success) {
          const last4Digits = officer.mobileNo.slice(-4);
          alert('Officer created! Temporary password: Rccms@' + last4Digits);
          this.loadOfficers();
        }
      }
    });
  }

  // Posting Management
  loadPostings() {
    this.adminService.getAllActivePostings().subscribe({
      next: (response) => {
        if (response.success) {
          this.postings = response.data;
        }
      }
    });
  }

  assignOfficerToPost(posting: any) {
    this.adminService.assignOfficerToPost(posting).subscribe({
      next: (response) => {
        if (response.success) {
          alert('Officer assigned! UserID: ' + response.data.postingUserid);
          this.loadPostings();
        }
      }
    });
  }

  // Admin Units Management
  loadAdminUnits() {
    this.adminService.getAllAdminUnits().subscribe({
      next: (response) => {
        if (response.success) {
          this.adminUnits = response.data;
        }
      }
    });
  }

  // Case Types Management
  loadCaseTypes() {
    this.adminService.getAllCaseTypes().subscribe({
      next: (response) => {
        if (response.success) {
          this.caseTypes = response.data;
        }
      }
    });
  }
}
```

---

## Complete API Endpoint Summary

| Category | Endpoint | Method | Description |
|----------|----------|--------|-------------|
| **Auth** | `/api/admin/auth/login` | POST | Admin login |
| **Auth** | `/api/admin/auth/officer-login` | POST | Officer/DA login |
| **Auth** | `/api/admin/auth/reset-password` | POST | Reset password |
| **Auth** | `/api/admin/auth/verify-mobile` | POST | Verify mobile |
| **Officers** | `/api/admin/officers` | POST | Create officer |
| **Officers** | `/api/admin/officers` | GET | Get all officers |
| **Officers** | `/api/admin/officers/{id}` | GET | Get officer by ID |
| **Postings** | `/api/admin/postings` | POST | Assign officer to post |
| **Postings** | `/api/admin/postings/transfer` | PUT | Transfer officer |
| **Postings** | `/api/admin/postings/userid/{userid}` | GET | Get posting by UserID |
| **Postings** | `/api/admin/postings/officer/{officerId}` | GET | Get postings by officer |
| **Postings** | `/api/admin/postings/unit/{unitId}` | GET | Get postings by unit |
| **Postings** | `/api/admin/postings/active` | GET | Get all active postings |
| **Postings** | `/api/admin/postings/{id}/close` | PUT | Close posting |
| **Admin Units** | `/api/admin-units` | POST | Create admin unit |
| **Admin Units** | `/api/admin-units` | GET | Get all admin units |
| **Admin Units** | `/api/admin-units/{id}` | GET | Get admin unit by ID |
| **Admin Units** | `/api/admin-units/code/{code}` | GET | Get admin unit by code |
| **Admin Units** | `/api/admin-units/level/{level}` | GET | Get by level |
| **Admin Units** | `/api/admin-units/parent/{parentId}` | GET | Get by parent |
| **Admin Units** | `/api/admin-units/root` | GET | Get root units |
| **Admin Units** | `/api/admin-units/{id}` | PUT | Update admin unit |
| **Admin Units** | `/api/admin-units/{id}` | DELETE | Delete admin unit |
| **Case Types** | `/api/case-types` | POST | Create case type |
| **Case Types** | `/api/case-types` | GET | Get all case types |
| **Case Types** | `/api/case-types/active` | GET | Get active case types |
| **Case Types** | `/api/case-types/{id}` | GET | Get case type by ID |
| **Case Types** | `/api/case-types/code/{code}` | GET | Get case type by code |
| **Case Types** | `/api/case-types/{id}` | PUT | Update case type |
| **Case Types** | `/api/case-types/{id}` | DELETE | Delete case type |
| **Case Types** | `/api/case-types/{id}/hard` | DELETE | Hard delete case type |

---

## Important Notes

1. **Token Storage:** Store JWT token after login and include in all subsequent requests
2. **Error Handling:** Handle 401 (Unauthorized) by redirecting to login page
3. **UserID Format:** `ROLE_CODE@UNIT_LGD_CODE` (e.g., `DISTRICT_OFFICER@140001`)
4. **Temporary Password:** `Rccms@<last4MobileDigits>` (auto-generated, check console logs)
5. **Password Reset:** Mandatory on first login for officers
6. **Posting Rules:** Only one active posting per unit+role; old postings are closed automatically
7. **Hierarchy:** STATE → DISTRICT → SUB_DIVISION → CIRCLE (strict hierarchy)
8. **Base URL:** `http://localhost:8080` (change for production)

---

## cURL Examples

### Admin Login
```bash
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin@123"
  }'
```

### Create Officer
```bash
curl -X POST http://localhost:8080/api/admin/officers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "fullName": "John Doe",
    "mobileNo": "9876543210",
    "email": "john.doe@example.com"
  }'
```

### Assign Officer to Post
```bash
curl -X POST http://localhost:8080/api/admin/postings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "unitId": 1,
    "roleCode": "DISTRICT_OFFICER",
    "officerId": 1
  }'
```

### Get All Officers
```bash
curl -X GET http://localhost:8080/api/admin/officers \
  -H "Authorization: Bearer <token>"
```

### Get All Active Postings
```bash
curl -X GET http://localhost:8080/api/admin/postings/active \
  -H "Authorization: Bearer <token>"
```

---

## Response Format

All API responses follow this standard format:

```json
{
  "success": true/false,
  "message": "Operation message",
  "data": { ... },
  "timestamp": "2026-01-09T15:30:00"
}
```

**Error Response Format:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 400/401/404/409/500,
  "error": "Error Type",
  "message": "Error message",
  "errors": [
    {
      "field": "fieldName",
      "message": "Validation error message"
    }
  ],
  "path": "/api/admin/officers"
}
```

---

## Swagger UI

Access interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

All endpoints are documented with request/response examples in Swagger UI.

