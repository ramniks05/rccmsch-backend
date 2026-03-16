# Workflow Data Keys – Single Source of Truth

Workflow permission conditions use `workflowDataFieldsRequired` to require that certain keys exist in `case_workflow_instance.workflow_data`. Those keys must match exactly what the application writes when forms are submitted or documents are created/signed.

## API: GET /api/admin/workflow/data-keys

**Response (for frontend):**

```json
{
  "success": true,
  "message": "Valid workflow data keys (use these in workflowDataFieldsRequired)",
  "data": {
    "keys": [
      "HEARING_SUBMITTED",
      "NOTICE_SUBMITTED",
      "ORDERSHEET_SUBMITTED",
      "JUDGEMENT_SUBMITTED",
      "ATTENDANCE_SUBMITTED",
      "FIELD_REPORT_SUBMITTED",
      "NOTICE_DRAFT_CREATED",
      "NOTICE_READY",
      "NOTICE_SIGNED",
      "ORDERSHEET_DRAFT_CREATED",
      "ORDERSHEET_READY",
      "ORDERSHEET_SIGNED",
      "JUDGEMENT_DRAFT_CREATED",
      "JUDGEMENT_READY",
      "JUDGEMENT_SIGNED",
      "NOTICE_ACCEPTED_BY_APPLICANT"
    ],
    "keysWithLabels": {
      "HEARING_SUBMITTED": "Hearing form submitted",
      "NOTICE_SUBMITTED": "Notice form submitted",
      "NOTICE_DRAFT_CREATED": "Draft notice created",
      "NOTICE_READY": "Notice document ready",
      "NOTICE_SIGNED": "Notice document signed",
      "NOTICE_ACCEPTED_BY_APPLICANT": "Notice accepted by applicant"
    },
    "keysWithBinding": [
      { "key": "HEARING_SUBMITTED", "label": "Hearing form submitted", "moduleType": "HEARING", "kind": "FORM" },
      { "key": "NOTICE_SUBMITTED", "label": "Notice form submitted", "moduleType": "NOTICE", "kind": "FORM" },
      { "key": "ORDERSHEET_SUBMITTED", "label": "Ordersheet form submitted", "moduleType": "ORDERSHEET", "kind": "FORM" },
      { "key": "JUDGEMENT_SUBMITTED", "label": "Judgement form submitted", "moduleType": "JUDGEMENT", "kind": "FORM" },
      { "key": "ATTENDANCE_SUBMITTED", "label": "Attendance form submitted", "moduleType": "ATTENDANCE", "kind": "FORM" },
      { "key": "FIELD_REPORT_SUBMITTED", "label": "Field report form submitted", "moduleType": "FIELD_REPORT", "kind": "FORM" },
      { "key": "NOTICE_DRAFT_CREATED", "label": "Draft notice created", "moduleType": "NOTICE", "kind": "DOCUMENT" },
      { "key": "NOTICE_READY", "label": "Notice document ready", "moduleType": "NOTICE", "kind": "DOCUMENT" },
      { "key": "NOTICE_SIGNED", "label": "Notice document signed", "moduleType": "NOTICE", "kind": "DOCUMENT" },
      { "key": "ORDERSHEET_DRAFT_CREATED", "label": "Draft ordersheet created", "moduleType": "ORDERSHEET", "kind": "DOCUMENT" },
      { "key": "ORDERSHEET_READY", "label": "Ordersheet document ready", "moduleType": "ORDERSHEET", "kind": "DOCUMENT" },
      { "key": "ORDERSHEET_SIGNED", "label": "Ordersheet document signed", "moduleType": "ORDERSHEET", "kind": "DOCUMENT" },
      { "key": "JUDGEMENT_DRAFT_CREATED", "label": "Draft judgement created", "moduleType": "JUDGEMENT", "kind": "DOCUMENT" },
      { "key": "JUDGEMENT_READY", "label": "Judgement document ready", "moduleType": "JUDGEMENT", "kind": "DOCUMENT" },
      { "key": "JUDGEMENT_SIGNED", "label": "Judgement document signed", "moduleType": "JUDGEMENT", "kind": "DOCUMENT" },
      { "key": "NOTICE_ACCEPTED_BY_APPLICANT", "label": "Notice accepted by applicant", "moduleType": "NOTICE", "kind": "SPECIAL" }
    ]
  },
  "timestamp": "2026-03-14T12:00:00"
}
```

**How keys bind to forms and documents (dynamic form schema):**

- The **form schema** (fields, layout) is dynamic: you define it in admin per case nature / case type / **module type** (e.g. HEARING). The backend still identifies each form by **moduleType** (HEARING, NOTICE, ORDERSHEET, etc.).
- When a form is submitted, the API is called with that module type in the path: `POST /api/cases/{caseId}/module-forms/{moduleType}/submit` (e.g. `moduleType=HEARING`). The backend then sets the workflow key `{moduleType}_SUBMITTED` (e.g. `HEARING_SUBMITTED`).
- So the binding is: **workflow key ↔ moduleType**. Use `data.keysWithBinding` to know for each key:
  - **kind = FORM** → key is set when the **module form** is submitted. Bind to: get schema `GET /api/cases/{caseId}/module-forms/{moduleType}`, submit `POST .../module-forms/{moduleType}/submit`. The “hearing form” is the form whose schema is returned for `moduleType=HEARING`; its fields are dynamic, but the key is always `HEARING_SUBMITTED`.
  - **kind = DOCUMENT** → key is set when the **document** (notice/ordersheet/judgement) is created/updated. Bind to: `GET/POST /api/cases/{caseId}/documents/{moduleType}`.
  - **kind = SPECIAL** → key is set by other actions (e.g. applicant acceptance); no form/document API.

**Frontend usage:**

- Use `data.keys` for a simple list of valid keys (e.g. for multi-select or validation).
- Use `data.keysWithLabels` for a key → label map for dropdowns.
- Use **`data.keysWithBinding`** to bind each option to the correct form or document: for each entry, `moduleType` tells you which form/document (e.g. HEARING), and `kind` tells you whether it’s a form (module-forms) or document (documents) or special.
- When saving a permission, set `conditions.workflowDataFieldsRequired` to an array of keys from `data.keys` only. The backend rejects any key not in this list.

---

## Frontend implementation guide

### 1. Fetch workflow data keys (once, e.g. on admin load or app init)

```ts
// GET /api/admin/workflow/data-keys (with auth)
const res = await fetch('/api/admin/workflow/data-keys', { headers: { Authorization: `Bearer ${token}` } });
const json = await res.json();
if (!json.success) throw new Error(json.message);
const { keys, keysWithLabels, keysWithBinding } = json.data;
```

Keep `keysWithBinding` in state or context so permission screens and case screens can use it.

---

### 2. Admin: Permission create/edit – “Workflow data required” dropdown

When the admin configures a transition permission and sets **workflowDataFieldsRequired** (conditions):

- **Options:** Use `keysWithBinding` to build the list. Show `label`, store `key`.
- **Multi-select:** User can select one or more keys (e.g. “Hearing form submitted” + “Draft notice created”). Send them as an array in the conditions JSON.

```ts
// Example: building options for a multi-select
const options = keysWithBinding.map((item) => ({
  value: item.key,
  label: item.label,
  kind: item.kind,      // optional: show badge "Form" / "Document" / "Special"
  moduleType: item.moduleType,
}));

// When saving permission (conditions JSON)
const conditions = {
  workflowDataFieldsRequired: selectedKeys,  // e.g. ["HEARING_SUBMITTED", "NOTICE_DRAFT_CREATED"]
};
// POST/PUT permission with conditions: JSON.stringify(conditions)
```

Validate before submit: only allow keys that exist in `keys` (or in `keysWithBinding[].key`). The backend will reject invalid keys with a clear error.

---

### 3. Case screen: Transition checklist – “Open form” or “Open document”

When you show the transition checklist and a condition is “HEARING_SUBMITTED” or “NOTICE_DRAFT_CREATED”, you need to open the right form or document.

- **Find binding:** From `keysWithBinding` find the item where `item.key === conditionKey` (e.g. `HEARING_SUBMITTED`).
- **If kind === "FORM":**  
  - Fetch schema: `GET /api/cases/{caseId}/module-forms/{item.moduleType}`  
  - Submit form: `POST /api/cases/{caseId}/module-forms/{item.moduleType}/submit`  
  - So for “Hearing form submitted” you use `moduleType: "HEARING"` → the **hearing form** (dynamic schema for HEARING).
- **If kind === "DOCUMENT":**  
  - Open document flow: `GET /api/cases/{caseId}/documents/{item.moduleType}` and create/update via `POST /api/cases/{caseId}/documents/{item.moduleType}`.  
  - So for “Draft notice created” you use `moduleType: "NOTICE"` → the **notice document**.
- **If kind === "SPECIAL":** No form/document to open; show message or link per business (e.g. “Notice accepted by applicant” might be a citizen action).

```ts
// Example: get route and label for a checklist condition
function getActionForConditionKey(conditionKey: string, caseId: number) {
  const binding = keysWithBinding.find((b) => b.key === conditionKey);
  if (!binding) return { type: 'unknown', label: conditionKey };

  if (binding.kind === 'FORM') {
    return {
      type: 'form',
      label: binding.label,
      moduleType: binding.moduleType,
      schemaUrl: `/api/cases/${caseId}/module-forms/${binding.moduleType}`,
      submitUrl: `/api/cases/${caseId}/module-forms/${binding.moduleType}/submit`,
    };
  }
  if (binding.kind === 'DOCUMENT') {
    return {
      type: 'document',
      label: binding.label,
      moduleType: binding.moduleType,
      documentUrl: `/api/cases/${caseId}/documents/${binding.moduleType}`,
    };
  }
  return { type: 'special', label: binding.label };
}
```

Then in the UI: for `type === 'form'` open the module form (using schema from `schemaUrl`); for `type === 'document'` open the document screen for that `moduleType`.

---

### 4. Summary

| Screen / flow           | Use from API              | What to do |
|-------------------------|---------------------------|------------|
| Admin – permission conditions | `keysWithBinding`        | Dropdown/multi-select: show `label`, store `key` in `workflowDataFieldsRequired`. |
| Case – transition checklist   | `keysWithBinding`        | For each required key, use `kind` + `moduleType` to open the correct form (module-forms) or document (documents). |
| Validation                 | `keys` or `keysWithBinding[].key` | Only allow these keys when building/sending conditions. |

The “hearing form” is the form whose schema is returned for `moduleType=HEARING`; its fields are configured dynamically in admin, but the key is always `HEARING_SUBMITTED` and the frontend always uses the same endpoints with `moduleType=HEARING`.

---

## Single reference

- **Definition:** `in.gov.manipur.rccms.constant.WorkflowDataKey`
- **Valid keys:** Built from `ModuleType` (e.g. `HEARING_SUBMITTED`, `NOTICE_DRAFT_CREATED`, `NOTICE_READY`, `NOTICE_SIGNED`, …) plus `NOTICE_ACCEPTED_BY_APPLICANT`.
- **When saving a permission:** Use only keys returned by `WorkflowDataKey.validKeys()` in `workflowDataFieldsRequired`. The service validates this and rejects invalid keys (e.g. `"DRAFT"`).
- **API:** `GET /api/admin/workflow/data-keys` returns the list of valid keys and their display labels for the admin UI.

## Fixing existing permissions that use invalid keys

If a permission fails with “Condition FAIL: workflowDataFieldsRequired - required=[DRAFT]” it is because the condition uses a key that is never written. Update the permission to use a valid key that the app actually sets, for example:

- For “draft notice” requirement use: `NOTICE_DRAFT_CREATED`
- For “hearing submitted” use: `HEARING_SUBMITTED`

Example (fix permission id 106 that required `DRAFT`):

```sql
UPDATE workflow_permission
SET conditions = '{"workflowDataFieldsRequired": ["HEARING_SUBMITTED"]}'
WHERE id = 106 AND conditions LIKE '%"DRAFT"%';
```

Or to require draft notice created:

```sql
UPDATE workflow_permission
SET conditions = '{"workflowDataFieldsRequired": ["NOTICE_DRAFT_CREATED"]}'
WHERE id = 106 AND conditions LIKE '%"DRAFT"%';
```

Choose the key that matches your business rule (e.g. `HEARING_SUBMITTED` or `NOTICE_DRAFT_CREATED`).
