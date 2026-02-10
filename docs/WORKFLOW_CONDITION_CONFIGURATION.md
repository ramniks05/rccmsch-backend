# Workflow Condition Configuration Guide

## Overview

This document provides a comprehensive guide for frontend developers to implement an admin-friendly UI for configuring workflow transition conditions. The system allows admins to set up prerequisites (checklist items) that must be met before a workflow transition can be executed.

## Table of Contents

1. [Understanding Workflow Conditions](#understanding-workflow-conditions)
2. [API Endpoints](#api-endpoints)
3. [Condition Types](#condition-types)
4. [UI Components Design](#ui-components-design)
5. [Implementation Examples](#implementation-examples)
6. [Best Practices](#best-practices)

---

## Understanding Workflow Conditions

### What are Workflow Conditions?

Workflow conditions are prerequisites that must be satisfied before a transition can be executed. For example:
- **"Schedule Hearing"** transition requires: Hearing date must be assigned
- **"Send Notice"** transition requires: Notice draft must be finalized
- **"Complete Hearing"** transition requires: Hearing form submitted AND hearing date filled

### How Conditions Work

1. **Form Submission Sets Flags**: When a form is submitted (e.g., hearing form), the backend automatically sets workflow flags:
   - `HEARING_SUBMITTED` = true (when hearing form is submitted)
   - `NOTICE_READY` = true (when notice document is FINAL/SIGNED)
   - `ORDERSHEET_READY` = true (when ordersheet document is FINAL/SIGNED)

2. **Conditions Check Flags**: When a user tries to execute a transition, the system checks if all required conditions are met.

3. **Blocking**: If any condition fails, the transition is blocked and the user sees which conditions are missing.

---

## API Endpoints

### 1. Get Transition Checklist Status

**Endpoint**: `GET /api/workflow/checklist/{caseId}/{transitionCode}`

**Description**: Get the checklist status for a specific transition on a case. Shows which conditions are met and which are blocking.

**Response**:
```json
{
  "success": true,
  "message": "Checklist retrieved successfully",
  "data": {
    "transitionCode": "SCHEDULE_HEARING",
    "transitionName": "Schedule Hearing",
    "canExecute": false,
    "conditions": [
      {
        "label": "Hearing form submitted",
        "type": "WORKFLOW_FLAG",
        "flagName": "HEARING_SUBMITTED",
        "required": true,
        "passed": false,
        "message": "Hearing form must be submitted before scheduling hearing"
      },
      {
        "label": "Hearing date assigned",
        "type": "FORM_FIELD",
        "fieldName": "hearingDate",
        "moduleType": "HEARING",
        "required": true,
        "passed": false,
        "message": "Hearing date must be assigned"
      }
    ],
    "blockingReasons": [
      "Hearing form must be submitted before scheduling hearing",
      "Hearing date must be assigned"
    ]
  }
}
```

### 2. Get Available Transitions with Checklist

**Endpoint**: `GET /api/workflow/cases/{caseId}/transitions`

**Description**: Get all available transitions for a case, including checklist status.

**Response**:
```json
{
  "success": true,
  "message": "Transitions retrieved successfully",
  "data": [
    {
      "id": 1,
      "transitionCode": "SCHEDULE_HEARING",
      "transitionName": "Schedule Hearing",
      "fromStateCode": "NOTICE_GENERATED",
      "toStateCode": "HEARING_SCHEDULED",
      "requiresComment": false,
      "description": "Schedule a hearing for the case",
      "canExecute": false,
      "blockingConditions": [
        {
          "label": "Hearing form submitted",
          "passed": false
        }
      ]
    }
  ]
}
```

### 3. Get Transition Conditions (Admin)

**Endpoint**: `GET /api/admin/workflow/transitions/{transitionId}/conditions`

**Description**: Get all configured conditions for a transition (for admin configuration UI).

**Response**:
```json
{
  "success": true,
  "message": "Conditions retrieved successfully",
  "data": [
    {
      "id": 1,
      "permissionId": 5,
      "roleCode": "CIRCLE_OFFICER",
      "conditionType": "WORKFLOW_FLAG",
      "flagName": "HEARING_SUBMITTED",
      "displayLabel": "Hearing form submitted",
      "isActive": true
    },
    {
      "id": 2,
      "permissionId": 5,
      "roleCode": "CIRCLE_OFFICER",
      "conditionType": "FORM_FIELD",
      "moduleType": "HEARING",
      "fieldName": "hearingDate",
      "displayLabel": "Hearing date assigned",
      "isActive": true
    }
  ]
}
```

### 4. Create/Update Permission with Conditions (Admin)

**Endpoint**: `POST /api/admin/workflow/transitions/{transitionId}/permissions`

**Request Body**:
```json
{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": false,
  "hierarchyRule": "SAME_UNIT",
  "conditions": {
    "workflowDataFieldsRequired": ["HEARING_SUBMITTED"],
    "moduleFormFieldsRequired": [
      {
        "moduleType": "HEARING",
        "fieldName": "hearingDate"
      }
    ]
  },
  "isActive": true
}
```

**Note**: The `conditions` field should be sent as a JSON string in the actual API call:
```json
{
  "roleCode": "CIRCLE_OFFICER",
  "conditions": "{\"workflowDataFieldsRequired\":[\"HEARING_SUBMITTED\"],\"moduleFormFieldsRequired\":[{\"moduleType\":\"HEARING\",\"fieldName\":\"hearingDate\"}]}"
}
```

---

## Condition Types

### 1. Workflow Flag Conditions

**Type**: `WORKFLOW_FLAG`

**Description**: Checks if a workflow flag is set to `true` in `CaseWorkflowInstance.workflowData`.

**Common Flags**:
- `HEARING_SUBMITTED` - Hearing form has been submitted
- `NOTICE_SUBMITTED` - Notice form has been submitted
- `NOTICE_READY` - Notice document is FINAL or SIGNED
- `ORDERSHEET_READY` - Ordersheet document is FINAL or SIGNED
- `JUDGEMENT_READY` - Judgement document is FINAL or SIGNED

**UI Display**: 
- Checkbox: "Require [Module] form submitted"
- Checkbox: "Require [Module] document ready"

**JSON Format**:
```json
{
  "workflowDataFieldsRequired": ["HEARING_SUBMITTED", "NOTICE_READY"]
}
```

### 2. Module Form Field Conditions

**Type**: `FORM_FIELD`

**Description**: Checks if a specific field in a module form submission has a value.

**UI Display**:
- Dropdown: Select Module Type (HEARING, NOTICE, ORDERSHEET, JUDGEMENT)
- Dropdown: Select Field Name (from module form schema)
- Toggle: "Required" or "Optional"

**JSON Format**:
```json
{
  "moduleFormFieldsRequired": [
    {
      "moduleType": "HEARING",
      "fieldName": "hearingDate"
    },
    {
      "moduleType": "NOTICE",
      "fieldName": "noticeNumber"
    }
  ]
}
```

### 3. Case Data Field Conditions

**Type**: `CASE_DATA_FIELD`

**Description**: Checks if a field in `Case.caseData` JSON has a value or equals a specific value.

**UI Display**:
- Dropdown: Select Case Data Field
- Input: Expected Value (optional, for equality check)

**JSON Format**:
```json
{
  "caseDataFieldsRequired": ["applicantName", "caseNumber"],
  "caseDataFieldEquals": {
    "priority": "HIGH",
    "status": "ACTIVE"
  }
}
```

### 4. Case Type/Priority Filters

**Type**: `CASE_FILTER`

**Description**: Restricts transition to specific case types or priorities.

**UI Display**:
- Multi-select: Case Types
- Multi-select: Priorities

**JSON Format**:
```json
{
  "caseTypeCodesAllowed": ["NEW_FILE", "APPEAL"],
  "casePriorityIn": ["HIGH", "URGENT"]
}
```

---

## UI Components Design

### 1. Workflow Configuration Page

**Location**: Admin Panel → Workflow Management → Configure Transitions

**Layout**:
```
┌─────────────────────────────────────────────────────────┐
│ Workflow Configuration                                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Workflow: [Dropdown: Select Workflow]                │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │ Transitions List                                │  │
│  ├─────────────────────────────────────────────────┤  │
│  │ [Transition Card 1]                             │  │
│  │   From: DA_ENTRY → To: MANDOL_RECEIVED         │  │
│  │   Code: ENTER_IN_REGISTER                        │  │
│  │   [View Conditions] [Edit]                      │  │
│  │                                                  │  │
│  │ [Transition Card 2]                             │  │
│  │   From: NOTICE_GENERATED → To: HEARING_SCHEDULED│  │
│  │   Code: SCHEDULE_HEARING                        │  │
│  │   [View Conditions] [Edit]                      │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 2. Condition Editor Modal

**Trigger**: Click "Edit" or "View Conditions" on a transition card

**Layout**:
```
┌─────────────────────────────────────────────────────────┐
│ Configure Conditions: SCHEDULE_HEARING                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Transition: Schedule Hearing                          │
│  From: NOTICE_GENERATED → To: HEARING_SCHEDULED        │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │ Permissions & Conditions                        │  │
│  ├─────────────────────────────────────────────────┤  │
│  │                                                 │  │
│  │ Role: CIRCLE_OFFICER                            │  │
│  │ Unit Level: CIRCLE                              │  │
│  │                                                 │  │
│  │ ┌─ Module Forms ───────────────────────────┐   │  │
│  │ │ ☑ Require Hearing form submitted         │   │  │
│  │ │ ☐ Require Notice form submitted          │   │  │
│  │ │ ☐ Require Ordersheet form submitted     │   │  │
│  │ └──────────────────────────────────────────┘   │  │
│  │                                                 │  │
│  │ ┌─ Documents ──────────────────────────────┐   │  │
│  │ │ ☑ Require Notice document ready           │   │  │
│  │ │ ☐ Require Ordersheet document ready       │   │  │
│  │ │ ☐ Require Judgement document ready        │   │  │
│  │ └──────────────────────────────────────────┘   │  │
│  │                                                 │  │
│  │ ┌─ Form Fields ─────────────────────────────┐  │  │
│  │ │ Module: [HEARING ▼]                       │  │  │
│  │ │ Field:  [hearingDate ▼]                   │  │  │
│  │ │ [Add Field]                                │  │  │
│  │ │                                             │  │  │
│  │ │ • Hearing → hearingDate                    │  │  │
│  │ └────────────────────────────────────────────┘  │  │
│  │                                                 │  │
│  │ ┌─ Case Filters ────────────────────────────┐  │  │
│  │ │ Case Types: [Multi-select]                 │  │  │
│  │ │ Priorities: [Multi-select]                 │  │  │
│  │ └───────────────────────────────────────────┘  │  │
│  │                                                 │  │
│  │ [Cancel] [Save Conditions]                     │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 3. Checklist Display (User View)

**Location**: Case Detail Page → Available Actions

**Layout**:
```
┌─────────────────────────────────────────────────────────┐
│ Available Actions                                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [✓] Submit Application          [Execute]              │
│                                                         │
│  [✗] Schedule Hearing            [Execute] (Disabled)  │
│      ⚠ Missing Requirements:                           │
│      • Hearing form must be submitted                   │
│      • Hearing date must be assigned                    │
│                                                         │
│  [✓] Generate Notice            [Execute]              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Tooltip on Hover**:
```
┌─────────────────────────────────────┐
│ Schedule Hearing - Checklist        │
├─────────────────────────────────────┤
│ ✓ Notice document ready             │
│ ✗ Hearing form submitted            │
│ ✗ Hearing date assigned             │
│                                     │
│ 2 of 3 conditions met               │
└─────────────────────────────────────┘
```

---

## Implementation Examples

### Example 1: Configure "Schedule Hearing" Transition

**Scenario**: Admin wants to require that hearing form is submitted and hearing date is assigned before allowing "Schedule Hearing" transition.

**Steps**:

1. **Open Condition Editor**:
   - Navigate to Workflow Config
   - Find transition "SCHEDULE_HEARING"
   - Click "Edit Conditions"

2. **Select Permission**:
   - Choose role: "CIRCLE_OFFICER"
   - Choose unit level: "CIRCLE"

3. **Configure Conditions**:
   - Check: "Require Hearing form submitted"
   - Add Form Field:
     - Module: "HEARING"
     - Field: "hearingDate"
   - Click "Save"

4. **Backend JSON Generated**:
```json
{
  "workflowDataFieldsRequired": ["HEARING_SUBMITTED"],
  "moduleFormFieldsRequired": [
    {
      "moduleType": "HEARING",
      "fieldName": "hearingDate"
    }
  ]
}
```

### Example 2: Configure "Send Notice" Transition

**Scenario**: Admin wants to require that notice document is finalized before sending to user.

**Steps**:

1. **Open Condition Editor** for "SEND_NOTICE" transition

2. **Configure Conditions**:
   - Check: "Require Notice document ready"
   - Click "Save"

3. **Backend JSON Generated**:
```json
{
  "workflowDataFieldsRequired": ["NOTICE_READY"]
}
```

### Example 3: Frontend Code Example (React/TypeScript)

```typescript
// types.ts
interface WorkflowCondition {
  id?: number;
  permissionId: number;
  roleCode: string;
  conditionType: 'WORKFLOW_FLAG' | 'FORM_FIELD' | 'CASE_DATA_FIELD' | 'CASE_FILTER';
  flagName?: string;
  moduleType?: 'HEARING' | 'NOTICE' | 'ORDERSHEET' | 'JUDGEMENT';
  fieldName?: string;
  displayLabel: string;
  isActive: boolean;
}

interface ConditionChecklistItem {
  label: string;
  type: string;
  required: boolean;
  passed: boolean;
  message: string;
}

interface TransitionChecklist {
  transitionCode: string;
  transitionName: string;
  canExecute: boolean;
  conditions: ConditionChecklistItem[];
  blockingReasons: string[];
}

// ConditionEditor.tsx
import React, { useState, useEffect } from 'react';

const ConditionEditor: React.FC<{ transitionId: number }> = ({ transitionId }) => {
  const [conditions, setConditions] = useState<WorkflowCondition[]>([]);
  const [selectedFlags, setSelectedFlags] = useState<string[]>([]);
  const [formFields, setFormFields] = useState<Array<{moduleType: string, fieldName: string}>>([]);

  useEffect(() => {
    // Load existing conditions
    fetch(`/api/admin/workflow/transitions/${transitionId}/conditions`)
      .then(res => res.json())
      .then(data => {
        setConditions(data.data);
        // Parse and populate UI state
        // ... parsing logic
      });
  }, [transitionId]);

  const handleSave = () => {
    const conditionsJson = {
      workflowDataFieldsRequired: selectedFlags,
      moduleFormFieldsRequired: formFields
    };

    fetch(`/api/admin/workflow/transitions/${transitionId}/permissions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        roleCode: 'CIRCLE_OFFICER',
        conditions: JSON.stringify(conditionsJson),
        // ... other fields
      })
    });
  };

  return (
    <div className="condition-editor">
      <h3>Module Forms</h3>
      <label>
        <input
          type="checkbox"
          checked={selectedFlags.includes('HEARING_SUBMITTED')}
          onChange={(e) => {
            if (e.target.checked) {
              setSelectedFlags([...selectedFlags, 'HEARING_SUBMITTED']);
            } else {
              setSelectedFlags(selectedFlags.filter(f => f !== 'HEARING_SUBMITTED'));
            }
          }}
        />
        Require Hearing form submitted
      </label>
      
      {/* More checkboxes... */}

      <h3>Form Fields</h3>
      {/* Form field selector... */}

      <button onClick={handleSave}>Save Conditions</button>
    </div>
  );
};
```

---

## Best Practices

### 1. User Experience

- **Show Clear Messages**: When a transition is blocked, show exactly what's missing
- **Visual Indicators**: Use icons (✓, ✗) and colors (green, red) to show condition status
- **Tooltips**: Provide detailed information on hover
- **Progressive Disclosure**: Show summary first, details on demand

### 2. Admin Configuration

- **Predefined Options**: Provide dropdowns/checkboxes instead of free text
- **Validation**: Validate condition configuration before saving
- **Preview**: Show how conditions will appear to end users
- **Examples**: Provide example configurations for common scenarios

### 3. Performance

- **Lazy Loading**: Load checklist only when needed
- **Caching**: Cache condition configurations
- **Batch Operations**: Load multiple transitions' checklists in one request if possible

### 4. Error Handling

- **Graceful Degradation**: If checklist API fails, still show transition (maybe with warning)
- **Clear Error Messages**: Show user-friendly error messages
- **Retry Logic**: Allow retry for failed API calls

---

## Common Workflow Flag Names

| Flag Name | Description | Set When |
|-----------|-------------|----------|
| `HEARING_SUBMITTED` | Hearing form submitted | Hearing form submission saved |
| `NOTICE_SUBMITTED` | Notice form submitted | Notice form submission saved |
| `NOTICE_READY` | Notice document ready | Notice document status = FINAL or SIGNED |
| `ORDERSHEET_READY` | Ordersheet document ready | Ordersheet document status = FINAL or SIGNED |
| `JUDGEMENT_READY` | Judgement document ready | Judgement document status = FINAL or SIGNED |

---

## Support

For questions or issues:
- Backend API Documentation: See Swagger UI at `/swagger-ui.html`
- Contact: Backend Development Team

---

**Last Updated**: January 2026
**Version**: 1.0
