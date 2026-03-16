# Case Status and Workflow (Dynamic vs Fixed)

## How case status is managed

**Case status is driven by the workflow you configure.** It is not a fixed list in code.

- **`case.status`** stores the **workflow state code** (`WorkflowState.state_code`) of the case’s **current** workflow state.
- **On case creation:** The case is linked to a workflow via Case Type → `workflow_code`. The **initial state** of that workflow (the one with `is_initial_state = true`) is used:  
  `case.status = initialState.getStateCode()`  
  So values like "CASE_INITIATED", "DRAFT", "PENDING" etc. come from the workflow state you define.
- **On every transition:** When a transition is executed, the case moves to the target state and:  
  `case.status = toState.getStateCode()`  
  So "PENDING_HEARING", "HEARING_SCHEDULED", "PENDING_JUDGEMENT", "DISPOSED", etc. are whatever state codes you configure in that workflow.

When you **create a new workflow** and assign it to a case type:

1. Define **workflow states** (e.g. CASE_INITIATED, NOTICE_GENERATED, HEARING_SCHEDULED, PENDING_JUDGEMENT, DISPOSED) with their **state_code** and **state_name**.
2. Set one state as **initial state** (`is_initial_state = true`).
3. Mark end states as **final** (`is_final_state = true`) if they mean “case closed/disposed”.
4. Case status will then automatically be those state codes as the case moves through the workflow. No code change is required for status values.

**Display name for status:** The API returns both `status` (code) and, from the workflow instance, `currentStateCode` and `currentStateName`. Use `currentStateName` (or the new `statusName` when set) for display; use `status` for filtering (e.g. GET /api/cases/status/{status}).

---

## What was fixed (no longer hardcoded)

These were previously hardcoded and are now workflow-driven so that new workflows work without code changes:

1. **Dashboard – “Hearing scheduled” count**  
   Previously: counted cases where `status = 'CASE_NUMBER_HEARING_DATE_GENERATED'`.  
   Now: uses a **configurable list** of state codes in `application.yml` (`app.dashboard.hearing-scheduled-statuses`). You can add any state code from your workflow(s) that should count as “hearing scheduled”. If the list is empty, the count is 0.

2. **Officer case statistics – Pending vs Disposed**  
   Previously: `pending = status != 'DISPOSED'`, `disposed = status = 'DISPOSED'`.  
   Now: uses **workflow state’s `is_final_state`**: pending = current state is not final, disposed = current state is final. So any workflow can define its own “end” states (DISPOSED, CLOSED, APPROVED, etc.) and they are treated as disposed.

3. **Case DTO – statusName**  
   When returning a case, `statusName` is set from the current workflow state’s `state_name`, so the frontend can show a single human-readable status label from the workflow configuration.

---

## Telling the user which state_code to use (workflow create/edit UI)

When the admin is **creating or editing workflow states**, the frontend should call:

**GET /api/admin/workflow/status-hints**

Response includes:

- **`reportingStatesWithLabels`** – **All possible reporting/dashboard state codes with display labels** (e.g. INITIATE_CASE → "Case Filled", PENDING → "Pending", CASE_ADMITTED → "Case Accepted", HEARING_SCHEDULED → "Hearing Scheduled", DISPOSED → "Disposed"). Use as the main dropdown/autocomplete so user can **choose or type**.
- **`reportingStatesList`** – Same as above as list of `{ stateCode, stateName }` for UI.
- **`stateCodesForChoice`** – State codes already in use in your workflows (from DB).
- **`stateCodesWithLabels`** – Same with labels (from DB).
- **`hearingScheduledStateCodes`** – Codes that count as "Hearing scheduled" on dashboard (from config).
- **`hints`** – Short texts for the state code field and final-state checkbox.

Use this API on the "Add/Edit workflow state" screen so the user can choose from the full reporting list or type a new code.

**Example response (for frontend – choose from list or type):**

```json
{
  "success": true,
  "message": "State code lists: choose from existing or type new",
  "data": {
    "stateCodesForChoice": ["CASE_INITIATED", "HEARING_SCHEDULED", "NOTICE_GENERATED", "DISPOSED"],
    "stateCodesWithLabels": [
      { "stateCode": "CASE_INITIATED", "stateName": "Case Initiated" },
      { "stateCode": "HEARING_SCHEDULED", "stateName": "Hearing Scheduled" }
    ],
    "reportingStatesWithLabels": {
      "INITIATE_CASE": "Case Filled",
      "CASE_INITIATED": "Case Initiated",
      "PENDING": "Pending",
      "CASE_ADMITTED": "Case Accepted",
      "HEARING_SCHEDULED": "Hearing Scheduled",
      "NOTICE_GENERATED": "Notice Generated",
      "PENDING_JUDGEMENT": "Pending Judgement",
      "DISPOSED": "Disposed",
      "COMPLETED": "Completed",
      "REJECTED": "Rejected"
    },
    "reportingStatesList": [
      { "stateCode": "INITIATE_CASE", "stateName": "Case Filled" },
      { "stateCode": "PENDING", "stateName": "Pending" },
      { "stateCode": "CASE_ADMITTED", "stateName": "Case Accepted" },
      { "stateCode": "HEARING_SCHEDULED", "stateName": "Hearing Scheduled" }
    ],
    "hearingScheduledStateCodes": ["CASE_NUMBER_HEARING_DATE_GENERATED", "HEARING_SCHEDULED"],
    "hints": { "stateCode": "...", "hearingScheduled": "...", "finalState": "..." }
  }
}
```

- **reportingStatesWithLabels** / **reportingStatesList** – **Full list of all possible reporting states** (Case Filled, Pending, Case Accepted, Hearing Scheduled, Pending Judgement, Disposed, etc.). Use for the main state code dropdown: user can **choose from this list or type a new code**.
- **stateCodesForChoice** / **stateCodesWithLabels** – State codes already in your DB (for “reuse existing”).
- **hearingScheduledStateCodes** – Codes that count as "Hearing scheduled" on dashboard.
- **hints** – Copy for the state code field and final-state checkbox.

---

## Frontend / API usage

- **List/filter by status:** Use `GET /api/cases/status/{status}` where `{status}` is a **state_code** from the workflow (e.g. HEARING_SCHEDULED, PENDING_JUDGEMENT). To get the list of possible statuses for a case type, use the workflow states API (e.g. states for the workflow linked to that case type).
- **Display status:** Use `currentStateName` or `statusName` from the case response for the label; use `status` (or `currentStateCode`) for the value.
- **Dashboard:** “Hearing scheduled” count is driven by config (see above). Other counts (pending/disposed) use workflow final state.

---

## Summary

| Aspect | Source | When you add a new workflow |
|--------|--------|-----------------------------|
| Case status value | `WorkflowState.state_code` (current state) | New state codes become new status values automatically. |
| Case status label | `WorkflowState.state_name` | Use state names you configure. |
| Pending vs disposed | `WorkflowState.is_final_state` | Mark end states as final; they count as disposed. |
| “Hearing scheduled” count | Config list of state codes | Add your new state code(s) to `app.dashboard.hearing-scheduled-statuses` in config. |
| Get cases by status | `GET /api/cases/status/{state_code}` | Use any state_code from your workflow. |

So case status is **not** fixed: it is whatever you configure in your workflow states. Only the “hearing scheduled” dashboard count needs a one-time config update when you introduce new state codes that should be counted there.
