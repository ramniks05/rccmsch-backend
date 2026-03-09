# Backend Changes Summary: Field Report Workflow

## Overview

This document summarizes the backend changes made to support the dynamic field report workflow where Tehsildar can request field reports from field officers (Patwari, Kanungo, etc.) who are posted to units (not courts).

---

## Changes Made

### 1. Repository Layer (`OfficerDaHistoryRepository.java`)

**Added Method:**
```java
/**
 * Find all field officers (unit-based postings) below a unit in hierarchy
 * Returns all unit-based officers in units under the given unit (child units)
 */
@Query("SELECT p FROM OfficerDaHistory p " +
       "WHERE p.isCurrent = true " +
       "AND p.courtId IS NULL " +
       "AND (p.unit.unitId = :unitId " +
       "     OR p.unit.parentUnitId = :unitId " +
       "     OR EXISTS (SELECT 1 FROM AdminUnit au WHERE au.unitId = :unitId AND au.parentUnitId = p.unit.unitId)) " +
       "ORDER BY p.roleCode, p.unit.unitName, p.officer.fullName")
List<OfficerDaHistory> findAllFieldOfficersBelowUnit(@Param("unitId") Long unitId);
```

**Purpose:** Get all field officers (all roles) in units below a given unit (e.g., all Patwaris and Kanungos in Circles under a District).

---

### 2. Service Layer (`PostingService.java`)

**Added Method:**
```java
/**
 * Get all field officers (unit-based postings) below a unit in hierarchy
 * Returns all unit-based officers in units under the given unit (all roles)
 * Useful for showing all field officers available to a Tehsildar
 */
@Transactional(readOnly = true)
public List<PostingDTO> getAllFieldOfficersBelowUnit(Long unitId) {
    List<OfficerDaHistory> fieldOfficers = postingRepository.findAllFieldOfficersBelowUnit(unitId);
    return fieldOfficers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
}
```

**Purpose:** Service method to get all field officers below a unit, converting entities to DTOs.

---

### 3. Controller Layer (`PostingController.java`)

**New Controller Created:** `src/main/java/in/gov/manipur/rccms/controller/PostingController.java`

**Endpoints Exposed:**

1. **POST `/api/admin/postings`**
   - Assign person to post (court-based or unit-based)
   - Already existed in service, now exposed via REST

2. **GET `/api/admin/postings/field-officers/court/{courtId}?roleCode={roleCode}`**
   - Get field officers for a court
   - If `roleCode` provided: returns officers of that role
   - If `roleCode` omitted: returns **all field officers** (all roles) below the court's unit

3. **GET `/api/admin/postings/field-officers/unit/{unitId}`** ⭐ **NEW**
   - Get **all field officers** (all roles) below a unit
   - Returns Patwari, Kanungo, and any other field officers in child units

4. **GET `/api/admin/postings/field-officers/role/{roleCode}`**
   - Get all field officers by role across all units

5. **GET `/api/admin/postings/field-officers/unit/{unitId}/role/{roleCode}`**
   - Get field officers by unit and role

---

### 4. Case Assignment Endpoint (`CaseController.java`)

**Added Endpoint:**
```java
/**
 * Assign case to a specific officer (manual assignment)
 * PUT /api/admin/cases/{caseId}/assign-officer
 */
@PutMapping("/{caseId}/assign-officer")
public ResponseEntity<ApiResponse<CaseDTO>> assignCaseToOfficer(
        @PathVariable Long caseId,
        @Valid @RequestBody AssignCaseOfficerDTO dto)
```

**Request Body:**
```json
{
  "officerId": 25,
  "roleCode": "PATWARI"
}
```

**Purpose:** Manually assign a case to a specific field officer after requesting field report.

---

### 5. DTO Created (`AssignCaseOfficerDTO.java`)

**New File:** `src/main/java/in/gov/manipur/rccms/dto/AssignCaseOfficerDTO.java`

**Fields:**
- `officerId` (required) - Officer ID to assign
- `roleCode` (required) - Role code (e.g., "PATWARI")

---

### 6. DTO Fix (`ExecuteTransitionDTO.java`)

**Fixed:** Removed `caseId` field from DTO (it's passed as path parameter, not in body)

---

## API Endpoints Summary

### Field Officer Search APIs

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/admin/postings/field-officers/unit/{unitId}` | GET | Get **all field officers** (all roles) below unit |
| `/api/admin/postings/field-officers/court/{courtId}` | GET | Get field officers for court (all roles if roleCode omitted) |
| `/api/admin/postings/field-officers/court/{courtId}?roleCode=PATWARI` | GET | Get field officers for court by role |
| `/api/admin/postings/field-officers/role/{roleCode}` | GET | Get all field officers by role |
| `/api/admin/postings/field-officers/unit/{unitId}/role/{roleCode}` | GET | Get field officers by unit and role |

### Case Assignment API

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/admin/cases/{caseId}/assign-officer` | PUT | Assign case to specific officer |

---

## How It Works

### Dynamic Field Officer Discovery

1. **Tehsildar's Context:**
   - Tehsildar is posted to a **court** (court-based posting)
   - Court has an associated **unit** (district)
   - Tehsildar's `unitId` = district ID

2. **Field Officer Discovery:**
   - Field officers (Patwari, Kanungo) are posted to **units** (unit-based postings)
   - Their `courtId = NULL`, `unitId = Circle/Sub-Division ID`
   - They are in units **below** the district (child units)

3. **API Call:**
   ```
   GET /api/admin/postings/field-officers/unit/{districtUnitId}
   ```
   - Returns all field officers in Circles/Sub-Divisions under that district
   - Includes all roles: PATWARI, KANUNGO, etc.

4. **Result:**
   - Tehsildar sees **all field officers below them** in hierarchy
   - No hardcoding needed
   - Automatically includes new officers when they're posted

---

## Workflow Integration

### Request Field Report Flow

1. **Check Transition Availability:**
   ```
   GET /api/cases/{caseId}/transitions
   ```
   - Check if `REQUEST_FIELD_REPORT` transition exists and `canExecute = true`

2. **Load Field Officers:**
   ```
   GET /api/admin/postings/field-officers/unit/{unitId}
   ```
   - Get all field officers below Tehsildar's unit

3. **Execute Transition:**
   ```
   POST /api/cases/{caseId}/transitions/execute
   {
     "transitionCode": "REQUEST_FIELD_REPORT",
     "comments": "..."
   }
   ```
   - Move case to `FIELD_REPORT_REQUESTED` state

4. **Assign Case:**
   ```
   PUT /api/admin/cases/{caseId}/assign-officer
   {
     "officerId": 25,
     "roleCode": "PATWARI"
   }
   ```
   - Assign case to selected field officer

---

## Database Impact

**No schema changes required.** All functionality uses existing tables:
- `officer_da_history` - Already supports unit-based postings (`court_id IS NULL`)
- `case_workflow_instance` - Already supports `assigned_to_officer_id`
- `workflow_transition` - Already configured for transitions
- `workflow_permission` - Already configured for role permissions

---

## Testing

### Test Scenarios

1. **Get Field Officers Below Unit:**
   ```bash
   GET /api/admin/postings/field-officers/unit/2
   ```
   - Should return all Patwari, Kanungo, etc. in Circles under District 2

2. **Get Field Officers for Court (All Roles):**
   ```bash
   GET /api/admin/postings/field-officers/court/1
   ```
   - Should return all field officers below court's district

3. **Get Field Officers for Court (Specific Role):**
   ```bash
   GET /api/admin/postings/field-officers/court/1?roleCode=PATWARI
   ```
   - Should return only Patwaris

4. **Assign Case to Field Officer:**
   ```bash
   PUT /api/admin/cases/123/assign-officer
   {
     "officerId": 25,
     "roleCode": "PATWARI"
   }
   ```
   - Should assign case to Patwari officer 25

---

## Benefits

1. ✅ **Fully Dynamic:** No hardcoded officer lists
2. ✅ **Hierarchy-Based:** Automatically finds officers in child units
3. ✅ **Role-Agnostic:** Works with any field officer role (PATWARI, KANUNGO, etc.)
4. ✅ **Workflow-Aligned:** Uses workflow transitions and permissions
5. ✅ **Scalable:** Adding new units or officers requires no code changes

---

## Files Modified/Created

### Created:
- `src/main/java/in/gov/manipur/rccms/controller/PostingController.java`
- `src/main/java/in/gov/manipur/rccms/dto/AssignCaseOfficerDTO.java`
- `docs/FRONTEND_FIELD_REPORT_WORKFLOW_IMPLEMENTATION.md`
- `docs/BACKEND_CHANGES_FIELD_REPORT_WORKFLOW.md`

### Modified:
- `src/main/java/in/gov/manipur/rccms/repository/OfficerDaHistoryRepository.java` - Added `findAllFieldOfficersBelowUnit` method
- `src/main/java/in/gov/manipur/rccms/service/PostingService.java` - Added `getAllFieldOfficersBelowUnit` method
- `src/main/java/in/gov/manipur/rccms/controller/CaseController.java` - Added `assignCaseToOfficer` endpoint
- `src/main/java/in/gov/manipur/rccms/dto/ExecuteTransitionDTO.java` - Removed `caseId` field (it's in path)

---

## Next Steps for Frontend

See `docs/FRONTEND_FIELD_REPORT_WORKFLOW_IMPLEMENTATION.md` for complete frontend implementation guide.

Key points:
1. Use `GET /api/admin/postings/field-officers/unit/{unitId}` to load all field officers
2. Group officers by role for better UI display
3. Execute transition first, then assign case
4. Handle errors gracefully

---

**Document Version:** 1.0  
**Last Updated:** 2026-03-09  
**For:** Chandigarh RCCMS Implementation
