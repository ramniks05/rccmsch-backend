# Module Forms & Templates Guide

This document explains how to configure hearing forms and document templates
(notice, ordersheet, judgement) per case nature, and how to connect them to workflow
checklists.

## Overview

There are two types of modules:

1) **Hearing**: data form (date, purpose, next date, etc.).
2) **Notice / Ordersheet / Judgement**: template-based documents (HTML + editable fields).

All configurations are **per case nature** by default, with an optional
**case type override**. If a case type override exists, it is used first.
If not, the system falls back to the case nature default.

Checklist conditions are applied using `workflow_permission.conditions`.

---

## Module Types

`HEARING`, `NOTICE`, `ORDERSHEET`, `JUDGEMENT`

## Override rule

Lookup order when rendering forms/templates for a case:

1) Case type override (if exists)
2) Case nature default

---

## Admin: Configure Module Forms (Hearing/Other forms)

### List fields
`GET /api/admin/module-forms/case-natures/{caseNatureId}/modules/{moduleType}/fields?caseTypeId=`

### Create field
`POST /api/admin/module-forms/fields`
```
{
  "caseNatureId": 33,
  "caseTypeId": 5,
  "moduleType": "HEARING",
  "fieldName": "hearingDate",
  "fieldLabel": "Hearing Date",
  "fieldType": "DATE",
  "isRequired": true,
  "displayOrder": 1
}
```

### Update field
`PUT /api/admin/module-forms/fields/{fieldId}`

### Delete field
`DELETE /api/admin/module-forms/fields/{fieldId}`

---

## Admin: Configure Document Templates

### List templates
`GET /api/admin/document-templates/case-natures/{caseNatureId}/modules/{moduleType}?caseTypeId=&activeOnly=true`

### Create template
`POST /api/admin/document-templates`
```
{
  "caseNatureId": 33,
  "caseTypeId": 5,
  "moduleType": "NOTICE",
  "templateName": "Default Notice Template",
  "templateHtml": "<html>...</html>",
  "templateData": "{ \"placeholders\": [\"{{caseNumber}}\", \"{{applicantName}}\"] }",
  "version": 1,
  "allowEditAfterSign": false,
  "isActive": true
}
```

### Update template
`PUT /api/admin/document-templates/{templateId}`

### Delete template
`DELETE /api/admin/document-templates/{templateId}`

---

## Officer: Hearing Form (Submit)

### Get form schema for a case
`GET /api/cases/{caseId}/module-forms/HEARING`

### Submit form data
`POST /api/cases/{caseId}/module-forms/HEARING/submit`
```
{
  "formData": "{\"hearingDate\":\"2026-02-01\",\"purpose\":\"First hearing\"}",
  "remarks": "Scheduled"
}
```

This sets workflow flag: `HEARING_SUBMITTED = true` in `case_workflow_instance.workflow_data`.

---

## Officer: Documents (Notice / Ordersheet / Judgement)

### Get active template for a case
`GET /api/cases/{caseId}/documents/NOTICE/template`

### Save document
`POST /api/cases/{caseId}/documents/NOTICE`
```
{
  "templateId": 12,
  "contentHtml": "<html>filled content</html>",
  "contentData": "{\"applicantName\":\"Ramesh\"}",
  "status": "FINAL"
}
```

When status is `FINAL` or `SIGNED`, workflow flag is set:
`NOTICE_READY = true` (similarly for `ORDERSHEET_READY`, `JUDGEMENT_READY`).

---

## Workflow Checklist (Admin)

Checklist is applied by setting `conditions` in workflow permissions.

Example: Approve transition requires notice + hearing + ordersheet + judgement.

```
{
  "workflowDataFieldsRequired": [
    "NOTICE_READY",
    "HEARING_SUBMITTED",
    "ORDERSHEET_READY",
    "JUDGEMENT_READY"
  ]
}
```

Use the existing permission APIs:
- Create permission: `POST /api/admin/workflow/transitions/{transitionId}/permissions`
- Update permission: `PUT /api/admin/workflow/permissions/{permissionId}`

Put the JSON above in `conditions`.

---

## Notes

1) Templates are stored as HTML; frontend can render and also export to PDF.
2) If `allowEditAfterSign = false`, signed documents cannot be edited.
3) Checklist keys are simple flags inside `workflow_data`.

