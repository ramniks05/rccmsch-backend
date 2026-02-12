# Dashboard API Documentation

Dashboard APIs provide case summary counts and hearing reports for use on public, admin, and officer dashboards. **No authentication is required** — all dashboard endpoints are publicly readable.

**Base path:** `/api/dashboard`

---

## 1. Case Summary

Returns total, pending, disposed, and hearing-scheduled case counts for the dashboard.

### Endpoint

```
GET /api/dashboard/case-summary
```

### Request

No query parameters or request body.

### Response

**Success (200 OK)**

```json
{
  "success": true,
  "message": "Case summary",
  "data": {
    "totalCases": 150,
    "pendingCases": 98,
    "disposedCases": 52,
    "hearingScheduledCount": 12
  }
}
```

### Field descriptions

| Field | Type | Description |
|-------|------|-------------|
| `totalCases` | number | Total number of active cases (`isActive = true`) |
| `pendingCases` | number | Cases whose current workflow state is **not** final (in progress) |
| `disposedCases` | number | Cases whose current workflow state **is** final (e.g. Completed, Rejected) |
| `hearingScheduledCount` | number | Cases with status `HEARING_SCHEDULED` |

### Example

```bash
curl -X GET "http://localhost:8080/api/dashboard/case-summary"
```

---

## 2. Hearings by Date

For a **selected date**, returns **all courts** with their details and the list of case numbers scheduled for that date. Use when the user picks a date and wants to see court-wise hearings.

### Endpoint

```
GET /api/dashboard/hearings-by-date
```

### Request

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `date` | string (ISO date) | Yes | Date to query hearings for. Format: `yyyy-MM-dd` (e.g. `2026-02-15`) |

### Response

**Success (200 OK)**

```json
{
  "success": true,
  "message": "Hearings by date",
  "data": {
    "date": "2026-02-15",
    "courts": [
      {
        "courtId": 1,
        "courtCode": "DC_IMPHAL",
        "courtName": "DC Court Imphal",
        "caseCount": 3,
        "cases": [
          { "caseId": 10, "caseNumber": "RCC/2026/001" },
          { "caseId": 11, "caseNumber": "RCC/2026/002" },
          { "caseId": 12, "caseNumber": "RCC/2026/003" }
        ]
      },
      {
        "courtId": 2,
        "courtCode": "SDC_THOUBAL",
        "courtName": "SDC Court Thoubal",
        "caseCount": 2,
        "cases": [
          { "caseId": 20, "caseNumber": "RCC/2026/010" },
          { "caseId": 21, "caseNumber": "RCC/2026/011" }
        ]
      }
    ]
  }
}
```

### Field descriptions

| Field | Type | Description |
|-------|------|-------------|
| `date` | string | The requested date |
| `courts` | array | List of courts that have hearings on this date |
| `courts[].courtId` | number | Court ID |
| `courts[].courtCode` | string | Court code |
| `courts[].courtName` | string | Court display name (label) |
| `courts[].caseCount` | number | Number of cases for this court on this date |
| `courts[].cases` | array | List of `{ caseId, caseNumber }` for this court |

**Note:** Results are based on the `hearing_date` field on the case. If no cases have a hearing on the given date, `courts` will be an empty array.

### Example

```bash
curl -X GET "http://localhost:8080/api/dashboard/hearings-by-date?date=2026-02-15"
```

---

## 3. Hearings by Court (Next 10 Days)

For a **selected court**, returns the **next 10 days** from a start date with **date-wise** hearing count and case numbers. Use when the user picks a court and wants a date-wise list.

### Endpoint

```
GET /api/dashboard/hearings-by-court
```

### Request

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `courtId` | number | Yes | Court ID to get hearings for |
| `fromDate` | string (ISO date) | No | Start date for the 10-day window. Default: **today**. Format: `yyyy-MM-dd` |

### Response

**Success (200 OK)**

```json
{
  "success": true,
  "message": "Hearings by court",
  "data": {
    "courtId": 1,
    "courtCode": "DC_IMPHAL",
    "courtName": "DC Court Imphal",
    "fromDate": "2026-02-09",
    "days": [
      { "date": "2026-02-09", "caseCount": 2, "caseNumbers": ["RCC/2026/001", "RCC/2026/002"] },
      { "date": "2026-02-10", "caseCount": 0, "caseNumbers": [] },
      { "date": "2026-02-11", "caseCount": 1, "caseNumbers": ["RCC/2026/005"] },
      { "date": "2026-02-12", "caseCount": 0, "caseNumbers": [] },
      { "date": "2026-02-13", "caseCount": 0, "caseNumbers": [] },
      { "date": "2026-02-14", "caseCount": 0, "caseNumbers": [] },
      { "date": "2026-02-15", "caseCount": 0, "caseNumbers": [] },
      { "date": "2026-02-16", "caseCount": 0, "caseNumbers": [] },
      { "date": "2026-02-17", "caseCount": 0, "caseNumbers": [] },
      { "date": "2026-02-18", "caseCount": 0, "caseNumbers": [] }
    ]
  }
}
```

### Field descriptions

| Field | Type | Description |
|-------|------|-------------|
| `courtId` | number | Court ID |
| `courtCode` | string | Court code |
| `courtName` | string | Court display name |
| `fromDate` | string | First date of the 10-day window |
| `days` | array | Exactly 10 entries, one per day |
| `days[].date` | string | Date (yyyy-MM-dd) |
| `days[].caseCount` | number | Number of hearings on that date for this court |
| `days[].caseNumbers` | array | List of case numbers for that date |

### Example

```bash
# From today (default)
curl -X GET "http://localhost:8080/api/dashboard/hearings-by-court?courtId=1"

# From a specific date
curl -X GET "http://localhost:8080/api/dashboard/hearings-by-court?courtId=1&fromDate=2026-02-09"
```

---

## Common response wrapper

All responses use the standard API wrapper:

```json
{
  "success": true,
  "message": "string",
  "data": { ... }
}
```

On error, the backend may return an error payload with `success: false` and an appropriate HTTP status.

---

## Access and authentication

- **Access:** Public (no login required). Intended for use by public, admin, and officer dashboards.
- **Method:** All dashboard APIs are **GET** only.
- **CORS:** Allowed for configured frontend origins.

---

## Hearing date data

- **Hearings by date** and **Hearings by court** use the `hearing_date` field on the case.
- `hearing_date` should be set when a hearing is scheduled (e.g. when the HEARING form is submitted or when the “Schedule Hearing” transition is executed).
- Until `hearing_date` is populated for cases, these two endpoints will return empty or zero counts.

---

## Quick reference

| API | Method | Path | Purpose |
|-----|--------|------|---------|
| Case summary | GET | `/api/dashboard/case-summary` | Total, pending, disposed, hearing-scheduled counts |
| Hearings by date | GET | `/api/dashboard/hearings-by-date?date=yyyy-MM-dd` | All courts and case numbers for a given date |
| Hearings by court | GET | `/api/dashboard/hearings-by-court?courtId=&fromDate=` | Next 10 days date-wise for a court |
