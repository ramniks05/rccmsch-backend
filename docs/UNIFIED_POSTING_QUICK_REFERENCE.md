# Unified Posting System - Quick Reference Guide

## Overview

The system now supports **TWO types of postings**:
- **Court-Based**: Officers posted to courts (TEHSILDAR, READER)
- **Unit-Based**: Field officers assigned to units (PATWARI, KANUNGO)

---

## Quick API Reference

### 1. Create Posting

**Court-Based:**
```bash
POST /api/admin/postings
{
  "courtId": 1,
  "unitId": null,
  "roleCode": "TEHSILDAR",
  "officerId": 1
}
```

**Unit-Based:**
```bash
POST /api/admin/postings
{
  "courtId": null,
  "unitId": 3,
  "roleCode": "PATWARI",
  "officerId": 3
}
```

### 2. Search Field Officers for Court

```bash
GET /api/admin/postings/field-officers/court/{courtId}?roleCode=PATWARI
```

### 3. Get All Field Officers by Role

```bash
GET /api/admin/postings/field-officers/role/PATWARI
```

### 4. Get Field Officers by Unit and Role

```bash
GET /api/admin/postings/field-officers/unit/{unitId}/role/PATWARI
```

---

## UserID Formats

| Posting Type | Format | Example |
|-------------|--------|---------|
| Court-Based | `ROLE@COURT_CODE` | `TEHSILDAR@CHD_TEHSILDAR_COURT` |
| Unit-Based | `ROLE@UNIT_LGD_CODE` | `PATWARI@400101` |

---

## Frontend Form Structure

```typescript
{
  postingType: 'COURT' | 'UNIT',  // Radio button
  courtId: number | null,         // If postingType === 'COURT'
  unitId: number | null,          // If postingType === 'UNIT'
  roleCode: string,
  officerId: number
}
```

---

## Response Fields

All posting responses include:
- `postingType`: `"COURT_BASED"` or `"UNIT_BASED"`
- `postingUserid`: Generated UserID
- `courtId`: NULL for unit-based
- `unitId`: Always present (from court or direct)

---

## Login

Same endpoint for both types:
```bash
POST /api/admin/auth/officer-login
{
  "userid": "PATWARI@400101",  // or "TEHSILDAR@CHD_TEHSILDAR_COURT"
  "password": "Rccms@2101"
}
```

Check `posting.postingType` in response to determine dashboard.

---

## Database Changes

Run migration script: `docs/DATABASE_MIGRATION_UNIFIED_POSTING.sql`

Key changes:
- `court_id` is now nullable
- `unit_id` column added
- New unique constraint for unit-based postings
- New indexes for performance

---

## Common Use Cases

### Assign Patwari to Circle
```json
POST /api/admin/postings
{
  "courtId": null,
  "unitId": <CIRCLE_UNIT_ID>,
  "roleCode": "PATWARI",
  "officerId": <OFFICER_ID>
}
```

### Find Patwari for District Court
```bash
GET /api/admin/postings/field-officers/court/<DISTRICT_COURT_ID>?roleCode=PATWARI
```

### Login as Patwari
```json
POST /api/admin/auth/officer-login
{
  "userid": "PATWARI@400101",
  "password": "Rccms@2101"
}
```

---

**For detailed documentation, see:** `docs/UNIFIED_POSTING_SYSTEM_API_DOCUMENTATION.md`
