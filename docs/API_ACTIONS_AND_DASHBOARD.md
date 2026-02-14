# API: Actions Required & Case Detail

This document specifies the endpoints for citizen/officer "Actions required" dashboards, officer "My Cases" filter by action, and on-demand case detail (history, documents).

**Authentication:** Citizen is identified by current auth (JWT or `X-User-Id` header as applicant). Officer is identified by current auth (JWT with officer id, role, unit).

---

## 1) Citizen dashboard – "Actions required"

When a citizen logs in, the frontend can show how many cases need their action and an optional short list.

### Endpoint

| Item | Value |
|------|--------|
| **URL** | `GET /api/citizen/cases/dashboard/actions-required` |
| **Method** | GET |
| **Auth** | Required (citizen: JWT or `X-User-Id` header) |

### Query parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | integer | No | Max number of items to return in the list (e.g. 10). If omitted, all action items are returned. |

### Sample response (200 OK)

```json
{
  "success": true,
  "message": "Actions required",
  "data": {
    "totalCount": 2,
    "items": [
      {
        "caseId": 101,
        "caseNumber": "MUT-UNIT-20250214-0001",
        "subject": "Mutation application for plot XYZ",
        "actionCode": "ACKNOWLEDGE_NOTICE",
        "actionLabel": "Acknowledge notice"
      },
      {
        "caseId": 102,
        "caseNumber": "MUT-UNIT-20250210-0002",
        "subject": "Correction of name in record",
        "actionCode": "RESUBMIT_AFTER_CORRECTION",
        "actionLabel": "Resubmit after correction"
      }
    ]
  },
  "timestamp": "2025-02-14T10:30:00"
}
```

**Action codes:** `ACKNOWLEDGE_NOTICE`, `RESUBMIT_AFTER_CORRECTION`. Use `actionCode` for routing; `actionLabel` for display.

---

## 2) Officer dashboard – "Actions required"

When an officer logs in, the frontend can show how many of their assigned cases need action (have at least one available workflow transition) and an optional short list.

### Endpoint

| Item | Value |
|------|--------|
| **URL** | `GET /api/cases/dashboard/actions-required` |
| **Method** | GET |
| **Auth** | Required (officer JWT with officerId, roleCode, unitId) |

### Query parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | integer | No | Max number of items in the list (e.g. 10). If omitted, all are returned. |

### Sample response (200 OK)

```json
{
  "success": true,
  "message": "Actions required",
  "data": {
    "totalCount": 3,
    "items": [
      {
        "caseId": 201,
        "caseNumber": "MUT-UNIT-20250214-0003",
        "subject": "Hearing scheduled for next week",
        "currentStateCode": "HEARING_SCHEDULED",
        "currentStateName": "Hearing Scheduled",
        "availableTransitions": [
          { "code": "RECORD_HEARING", "label": "Record hearing" },
          { "code": "ADJOURN", "label": "Adjourn" }
        ]
      },
      {
        "caseId": 202,
        "caseNumber": "MUT-UNIT-20250212-0001",
        "subject": "SDC decision pending",
        "currentStateCode": "SDC_DECISION_PENDING",
        "currentStateName": "SDC Decision Pending",
        "availableTransitions": [
          { "code": "APPROVE", "label": "Approve" },
          { "code": "SEND_NOTICE", "label": "Send notice" }
        ]
      }
    ]
  },
  "timestamp": "2025-02-14T10:30:00"
}
```

---

## 3) Officer "My Cases" – filter by action

Officers have a "My Cases" list (assigned cases). The list can be filtered by action type (transition code) so that only cases where that transition is currently available are returned.

### My Cases (with optional filter)

| Item | Value |
|------|--------|
| **URL** | `GET /api/cases/my-cases` |
| **Method** | GET |
| **Auth** | Required (officer) |

### Query parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `transitionCode` | string | No | When provided, only cases where this transition is currently available are returned (e.g. `RECORD_HEARING`, `APPROVE`, `SEND_NOTICE`). |

### Sample request

- All my cases: `GET /api/cases/my-cases`
- Only cases where "Record hearing" is available: `GET /api/cases/my-cases?transitionCode=RECORD_HEARING`

### Sample response (200 OK)

Same shape as existing my-cases: list of `CaseDTO`.

```json
{
  "success": true,
  "message": "Cases retrieved successfully",
  "data": [
    {
      "id": 201,
      "caseNumber": "MUT-UNIT-20250214-0003",
      "subject": "Hearing scheduled for next week",
      "status": "HEARING_SCHEDULED",
      "currentStateCode": "HEARING_SCHEDULED",
      "currentStateName": "Hearing Scheduled",
      "assignedToOfficerId": 5,
      "assignedToOfficerName": "John Doe",
      "applicationDate": "2025-02-01",
      "applicantName": "Jane Smith"
    }
  ],
  "timestamp": "2025-02-14T10:30:00"
}
```

### Action types for filter dropdown

To build the "filter by action type" dropdown, the frontend needs the list of action types (transition codes and labels) that currently exist in the officer's caseload.

| Item | Value |
|------|--------|
| **URL** | `GET /api/cases/my-cases/action-types` |
| **Method** | GET |
| **Auth** | Required (officer) |

No query or body parameters.

### Sample response (200 OK)

```json
{
  "success": true,
  "message": "Action types in caseload",
  "data": [
    { "code": "RECORD_HEARING", "label": "Record hearing" },
    { "code": "APPROVE", "label": "Approve" },
    { "code": "SEND_NOTICE", "label": "Send notice" },
    { "code": "ADJOURN", "label": "Adjourn" }
  ],
  "timestamp": "2025-02-14T10:30:00"
}
```

Use `code` as the value for `transitionCode` when calling `GET /api/cases/my-cases?transitionCode=...`. Optionally, the same list can be derived from the officer dashboard response (union of all `availableTransitions` from `items`).

---

## 4) Case detail on demand

When the user opens a case (e.g. from dashboard or My Cases), the frontend can load full case detail: case info, workflow history, and which documents exist. Document content is fetched separately when needed.

### Case detail (summary + history + documents list)

| Item | Value |
|------|--------|
| **URL** | `GET /api/cases/{id}/detail` |
| **Method** | GET |
| **Auth** | As per your policy (officer/citizen depending on context) |

Path parameter: `id` – case ID.

### Sample response (200 OK)

```json
{
  "success": true,
  "message": "Case detail",
  "data": {
    "caseInfo": {
      "id": 201,
      "caseNumber": "MUT-UNIT-20250214-0003",
      "subject": "Hearing scheduled for next week",
      "status": "HEARING_SCHEDULED",
      "currentStateCode": "HEARING_SCHEDULED",
      "currentStateName": "Hearing Scheduled",
      "applicantName": "Jane Smith",
      "applicationDate": "2025-02-01",
      "courtName": "Court of SDC",
      "unitName": "Unit A"
    },
    "history": [
      {
        "id": 501,
        "caseId": 201,
        "transitionCode": "SCHEDULE_HEARING",
        "transitionName": "Schedule hearing",
        "fromStateCode": "DA_ENTRY",
        "toStateCode": "HEARING_SCHEDULED",
        "performedByOfficerName": "John Doe",
        "performedByRole": "DA",
        "performedAt": "2025-02-10T14:00:00",
        "comments": "Hearing fixed for 20 Feb"
      }
    ],
    "documents": [
      {
        "documentId": 301,
        "moduleType": "NOTICE",
        "moduleTypeLabel": "NOTICE",
        "status": "SIGNED",
        "createdAt": "2025-02-08T10:00:00",
        "signedAt": "2025-02-08T12:00:00",
        "hasContent": true
      },
      {
        "documentId": 302,
        "moduleType": "HEARING",
        "moduleTypeLabel": "HEARING",
        "status": "DRAFT",
        "createdAt": "2025-02-10T14:05:00",
        "signedAt": null,
        "hasContent": true
      }
    ]
  },
  "timestamp": "2025-02-14T10:30:00"
}
```

### Fetching document content

To show notice/ordersheet/judgement content, use the existing document endpoint:

| Item | Value |
|------|--------|
| **URL** | `GET /api/cases/{caseId}/documents/{moduleType}` |
| **Method** | GET |
| **moduleType** | One of: `HEARING`, `NOTICE`, `ORDERSHEET`, `JUDGEMENT` |

Response includes full document (e.g. `CaseDocumentDTO` with `contentHtml`, `contentData`, `status`, etc.).

**Citizen:** For notice, use `GET /api/citizen/cases/{caseId}/documents/NOTICE` (same contract; access rules may differ).

### Other existing endpoints useful for case detail

- **Workflow history only:** `GET /api/cases/{caseId}/history` – same `history` array as in detail.
- **Case basic info:** `GET /api/cases/{id}` – same as `caseInfo` in detail.
- **Available transitions (officer):** `GET /api/cases/{caseId}/transitions` – list of transitions the current officer can perform.

---

## Summary table

| Purpose | Method | URL | Key params / body |
|--------|--------|-----|-------------------|
| Citizen actions required | GET | `/api/citizen/cases/dashboard/actions-required` | `limit` (optional) |
| Officer actions required | GET | `/api/cases/dashboard/actions-required` | `limit` (optional) |
| My Cases (all or filter) | GET | `/api/cases/my-cases` | `transitionCode` (optional) |
| My Cases action types | GET | `/api/cases/my-cases/action-types` | — |
| Case detail (info + history + documents) | GET | `/api/cases/{id}/detail` | path: `id` |
| Document content | GET | `/api/cases/{caseId}/documents/{moduleType}` | path: `caseId`, `moduleType` |
| Workflow history | GET | `/api/cases/{caseId}/history` | path: `caseId` |

All responses use the standard `ApiResponse<T>` wrapper: `success`, `message`, `data`, `timestamp`.
