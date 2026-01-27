# Citizen Resubmit Guide

This guide explains how to enable and use the citizen resubmit flow after an officer returns a case for correction.

## When resubmit is allowed

Resubmit is allowed only when the current workflow state code is exactly:

`RETURNED_FOR_CORRECTION`

If the case is in any other state (for example `RETURNED_TO_CITIZEN`), the backend will reject resubmission.

## Backend endpoints

### 1) List citizen cases

Use this to show the citizen their submitted cases and decide when to show the resubmit button.

`GET /api/cases/applicant/{applicantId}`

Use the `status` field in each case. Show the resubmit button only when:

`status === "RETURNED_FOR_CORRECTION"`

### 2) Resubmit case

`PUT /api/citizen/cases/{caseId}/resubmit`

**Headers**
- `X-User-Id: <applicantId>` (or provide JWT, if your frontend uses tokens)

**Body**
```
{
  "caseData": "{... JSON string ...}",
  "remarks": "optional remarks"
}
```

`caseData` must be a JSON string and must pass the form schema validation for the case type.

## Frontend flow

1. Call `GET /api/cases/applicant/{applicantId}`.
2. For each case, check `status`.
3. If `status === "RETURNED_FOR_CORRECTION"`, show a **Resubmit** action.
4. On resubmit:
   - Collect updated form data.
   - Convert it to a JSON string.
   - Call `PUT /api/citizen/cases/{caseId}/resubmit` with `X-User-Id` header.

## Common issues

- **Resubmit button not showing**: The case status is not `RETURNED_FOR_CORRECTION`.
- **Resubmit fails with “Case is not in RETURNED_FOR_CORRECTION state”**: The workflow state code is different. Fix the workflow state code for the “return for correction” step.
- **Validation errors**: Your `caseData` JSON must match the form schema for the case type.

