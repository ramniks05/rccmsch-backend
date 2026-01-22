# Posting System Migration: Unit → Court

## Summary

The posting system has been migrated from **Unit-based** to **Court-based** postings. Officers are now posted to **Courts** instead of **Administrative Units**, which better aligns with the case filing system where cases are filed in courts.

---

## Why This Change?

1. **Better Alignment**: Cases are filed in courts, so officers should be posted to courts
2. **More Precise**: A unit can have multiple courts (e.g., District has DC_COURT and REVENUE_TRIBUNAL)
3. **Direct Case Assignment**: Can directly assign cases to officers at the same court
4. **Unit Still Available**: Since Court → Unit, we can still derive unit for hierarchy checks

---

## Changes Made

### 1. Entity Changes

#### `OfficerDaHistory` Entity
- ❌ **Removed**: `unit_id` field and `unit` relationship
- ✅ **Added**: `court_id` field and `court` relationship
- ✅ **Updated**: Unique constraint from `(unit_id, role_code, is_current)` to `(court_id, role_code, is_current)`
- ✅ **Updated**: Index from `idx_unit_role` to `idx_court_role`
- ✅ **Updated**: UserID format comment from `ROLE_CODE@UNIT_LGD_CODE` to `ROLE_CODE@COURT_CODE`

### 2. DTO Changes

#### `PostingAssignmentDTO`
- ❌ **Removed**: `unitId` field
- ✅ **Added**: `courtId` field

#### `PostingDTO`
- ❌ **Removed**: Direct unit fields (unitId, unitName, unitLgdCode)
- ✅ **Added**: Court fields (courtId, courtName, courtCode, courtLevel, courtType)
- ✅ **Added**: Unit fields (derived from court) - unitId, unitName, unitCode, unitLgdCode

#### `PostingDetailsDTO`
- ✅ **Added**: Court fields (courtId, courtCode, courtName, courtLevel, courtType)
- ✅ **Kept**: Unit fields (derived from court for hierarchy)

### 3. Repository Changes

#### `OfficerDaHistoryRepository`
- ❌ **Removed**: `findByUnitIdAndRoleCodeAndIsCurrentTrue()`
- ❌ **Removed**: `findByUnitIdAndIsCurrentTrue()`
- ❌ **Removed**: `findByUnitIdOrderByFromDateDesc()`
- ❌ **Removed**: `existsByUnitIdAndRoleCodeAndIsCurrentTrue()`
- ❌ **Removed**: `findActivePostingsByUnitAndRole()`
- ✅ **Added**: `findByCourtIdAndRoleCodeAndIsCurrentTrue()`
- ✅ **Added**: `findByCourtIdAndIsCurrentTrue()`
- ✅ **Added**: `findByCourtIdOrderByFromDateDesc()`
- ✅ **Added**: `existsByCourtIdAndRoleCodeAndIsCurrentTrue()`
- ✅ **Added**: `findActivePostingsByCourtAndRole()`
- ✅ **Added**: `findActivePostingsByUnit()` - queries through court relationship
- ✅ **Updated**: `findByPostingUseridAndIsCurrentTrue()` - now eagerly fetches court and unit

### 4. Service Changes

#### `PostingService`
- ❌ **Removed**: `AdminUnitRepository` dependency
- ✅ **Added**: `CourtRepository` dependency
- ✅ **Updated**: `assignPersonToPost()` - now uses court instead of unit
- ✅ **Updated**: `closeExistingActivePosting()` - now uses courtId instead of unitId
- ✅ **Updated**: `generateUserid()` - now uses court code instead of unit LGD code
- ✅ **Updated**: `convertToDTO()` - now populates court fields and derives unit from court
- ❌ **Removed**: `getPostingsByUnit()` method
- ✅ **Added**: `getPostingsByCourt()` method
- ✅ **Added**: `getActivePostingsByUnit()` method - queries through court relationship

#### `PostBasedAuthService`
- ✅ **Updated**: `loginWithPostBasedCredentials()` - now gets court first, then derives unit
- ✅ **Updated**: JWT token generation - still uses unitId (derived from court)
- ✅ **Updated**: `PostingDetailsDTO` building - now includes court information

### 5. Controller Changes

#### `AdminController`
- ✅ **Updated**: `assignPersonToPost()` - documentation updated to mention COURT + ROLE
- ✅ **Updated**: `transferPerson()` - log messages updated
- ✅ **Updated**: `getPostingByUserid()` - documentation updated for new UserID format
- ❌ **Removed**: `getPostingsByUnit()` endpoint
- ✅ **Added**: `getPostingsByCourt()` endpoint - `GET /api/admin/postings/court/{courtId}`
- ✅ **Added**: `getActivePostingsByUnit()` endpoint - `GET /api/admin/postings/unit/{unitId}/active`

---

## UserID Format Change

### Before:
```
ROLE_CODE@UNIT_LGD_CODE
Example: SDC@123456
```

### After:
```
ROLE_CODE@COURT_CODE
Example: SDC@DC_COURT_IMPHAL_EAST
```

---

## API Changes

### Changed Endpoints:

1. **POST `/api/admin/postings`**
   - **Request Body**: Now requires `courtId` instead of `unitId`
   ```json
   {
     "courtId": 1,
     "roleCode": "SDC",
     "officerId": 1
   }
   ```

2. **GET `/api/admin/postings/court/{courtId}`** (NEW)
   - Replaces: `GET /api/admin/postings/unit/{unitId}`
   - Returns all postings (active and inactive) for a court

3. **GET `/api/admin/postings/unit/{unitId}/active`** (NEW)
   - Returns all active postings for a unit (through courts)

### Response Changes:

**PostingDTO** now includes:
```json
{
  "id": 1,
  "courtId": 1,
  "courtName": "DC Court Imphal East",
  "courtCode": "DC_COURT_IMPHAL_EAST",
  "courtLevel": "DISTRICT",
  "courtType": "DC_COURT",
  "unitId": 5,
  "unitName": "Imphal East District",
  "unitCode": "IE",
  "unitLgdCode": "123456",
  "roleCode": "SDC",
  "officerId": 1,
  "postingUserid": "SDC@DC_COURT_IMPHAL_EAST",
  ...
}
```

---

## Database Migration

### Required Changes:

1. **Add `court_id` column** to `officer_da_history` table
2. **Remove `unit_id` column** from `officer_da_history` table (or keep for migration period)
3. **Update unique constraint**: Change from `(unit_id, role_code, is_current)` to `(court_id, role_code, is_current)`
4. **Update index**: Change from `idx_unit_role` to `idx_court_role`
5. **Update foreign key**: Change from `fk_posting_unit` to `fk_posting_court`

### Migration SQL (Example):

```sql
-- Add court_id column
ALTER TABLE officer_da_history ADD COLUMN court_id BIGINT;

-- Migrate data (if unit_id exists, find court through unit)
-- Note: This requires mapping logic based on your data
UPDATE officer_da_history odh
SET court_id = (
    SELECT c.id 
    FROM courts c 
    WHERE c.unit_id = odh.unit_id 
    LIMIT 1
)
WHERE court_id IS NULL;

-- Add foreign key
ALTER TABLE officer_da_history 
ADD CONSTRAINT fk_posting_court 
FOREIGN KEY (court_id) REFERENCES courts(id);

-- Update unique constraint
ALTER TABLE officer_da_history 
DROP CONSTRAINT uk_posting_unit_role_current;

ALTER TABLE officer_da_history 
ADD CONSTRAINT uk_posting_court_role_current 
UNIQUE (court_id, role_code, is_current);

-- Update index
DROP INDEX idx_unit_role;
CREATE INDEX idx_court_role ON officer_da_history(court_id, role_code);

-- Remove unit_id (after verification)
-- ALTER TABLE officer_da_history DROP COLUMN unit_id;
-- ALTER TABLE officer_da_history DROP CONSTRAINT fk_posting_unit;
```

---

## Benefits

1. ✅ **Direct Court Assignment**: Officers are posted to courts where they actually work
2. ✅ **Better Case Assignment**: Cases can be directly assigned to officers at the same court
3. ✅ **More Precise**: Multiple courts per unit are properly handled
4. ✅ **Unit Still Available**: Unit information is derived from court for hierarchy checks
5. ✅ **Cleaner Architecture**: Aligns with case filing system

---

## Backward Compatibility

⚠️ **Breaking Changes:**
- UserID format changed (old UserIDs will not work)
- API endpoints changed (unit-based endpoints removed)
- Request/Response DTOs changed

**Migration Strategy:**
1. Run database migration
2. Update all existing postings to use court_id
3. Regenerate UserIDs for all active postings
4. Update frontend to use new API endpoints

---

## Summary

✅ **All code changes completed**
✅ **Entity, DTO, Repository, Service, Controller updated**
✅ **Unit information still available through court relationship**
✅ **Ready for database migration**

The system now properly aligns officers with courts, making case assignment more accurate and intuitive!
