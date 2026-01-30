# Frontend Implementation Guide

This guide provides complete API documentation and implementation patterns for building the frontend application that integrates with the RCCMSMP workflow system.

---

## Table of Contents

1. [Authentication](#authentication)
2. [Workflow Integration](#workflow-integration)
3. [Module Forms (Hearing, Notice, etc.)](#module-forms)
4. [Documents (Notice, Ordersheet, etc.)](#documents)
5. [Applicant/Citizen Features](#applicant-features)
6. [Error Handling](#error-handling)
7. [State Management Patterns](#state-management-patterns)

---

## Authentication

### Officer Authentication
Officers use JWT tokens for authentication. Include token in Authorization header:

```http
Authorization: Bearer {jwt_token}
```

### Applicant/Citizen Authentication
Applicants can use either:
- JWT token (if available)
- X-User-Id header (for citizen login)

```http
X-User-Id: {applicantId}
```

---

## Workflow Integration

### 1. Get Available Transitions

**Endpoint:** `GET /api/cases/{caseId}/transitions`

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Available transitions retrieved successfully",
  "data": [
    {
      "id": 136,
      "transitionCode": "SET_HEARING_DATE",
      "transitionName": "Set Hearing Date",
      "fromStateCode": "APPROVED_APPLICATION",
      "toStateCode": "SET_HEARING_DATE",
      "requiresComment": true,
      "description": "Set Hearing Date",
      "checklist": {
        "transitionCode": "SET_HEARING_DATE",
        "transitionName": "Set Hearing Date",
        "canExecute": false,
        "conditions": [
          {
            "type": "FORM_FIELD",
            "moduleType": "HEARING",
            "label": "Hearing form submitted",
            "required": true,
            "passed": false,
            "message": "Hearing form submitted must be completed"
          }
        ],
        "blockingReasons": [
          "Hearing form submitted must be completed"
        ]
      },
      "formSchema": {
        "caseNatureId": 1,
        "caseNatureCode": "CIVIL",
        "moduleType": "HEARING",
        "fields": [
          {
            "fieldName": "hearingDate",
            "fieldLabel": "Hearing Date",
            "fieldType": "DATE",
            "isRequired": true,
            "validationRules": "{...}",
            ...
          }
        ],
        "totalFields": 5
      }
    }
  ],
  "timestamp": "2026-01-28T00:28:05.7405704"
}
```

**Frontend Implementation:**
```typescript
// Fetch available transitions
const getAvailableTransitions = async (caseId: number) => {
  const response = await fetch(`/api/cases/${caseId}/transitions`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  return data.data; // Array of WorkflowTransitionDTO
};

// Display transitions with checklist and form schema
transitions.forEach(transition => {
  if (transition.checklist) {
    // Show checklist conditions
    transition.checklist.conditions.forEach(condition => {
      if (!condition.passed) {
        // Show as blocking requirement
      }
    });
  }
  
  if (transition.formSchema) {
    // Show form to fill
    // Use formSchema.fields to build form UI
  }
});
```

### 2. Get Transition Checklist

**Endpoint:** `GET /api/cases/{caseId}/transitions/{transitionCode}/checklist`

**Response:**
```json
{
  "success": true,
  "message": "Checklist retrieved successfully",
  "data": {
    "transitionCode": "SET_HEARING_DATE",
    "transitionName": "Set Hearing Date",
    "canExecute": false,
    "conditions": [
      {
        "type": "FORM_FIELD",
        "moduleType": "HEARING",
        "fieldName": "hearingDate",
        "label": "Hearing - hearingDate",
        "required": true,
        "passed": false,
        "message": "Hearing - hearingDate must be filled"
      },
      {
        "type": "WORKFLOW_FLAG",
        "flagName": "HEARING_SUBMITTED",
        "label": "Hearing form submitted",
        "required": true,
        "passed": false,
        "message": "Hearing form submitted must be completed"
      }
    ],
    "blockingReasons": [
      "Hearing form submitted must be completed"
    ]
  }
}
```

**Frontend Implementation:**
```typescript
const getChecklist = async (caseId: number, transitionCode: string) => {
  const response = await fetch(
    `/api/cases/${caseId}/transitions/${transitionCode}/checklist`,
    {
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );
  const data = await response.json();
  return data.data;
};

// Display checklist
const ChecklistComponent = ({ checklist }) => {
  return (
    <div>
      <h3>Requirements</h3>
      {checklist.conditions.map((condition, idx) => (
        <div key={idx} className={condition.passed ? 'passed' : 'failed'}>
          {condition.passed ? '✓' : '✗'} {condition.message}
          {condition.moduleType && (
            <button onClick={() => showForm(condition.moduleType)}>
              Fill {condition.moduleType} Form
            </button>
          )}
        </div>
      ))}
      {!checklist.canExecute && (
        <div className="error">
          Cannot execute: {checklist.blockingReasons.join(', ')}
        </div>
      )}
    </div>
  );
};
```

### 3. Execute Transition

**Endpoint:** `POST /api/cases/{caseId}/transitions/{transitionCode}`

**Request Body:**
```json
{
  "comments": "Hearing date set for 2026-02-15"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transition executed successfully",
  "data": {
    "id": 123,
    "caseId": 18,
    "currentStateCode": "SET_HEARING_DATE",
    "currentStateName": "Set Hearing Date",
    ...
  }
}
```

**Frontend Implementation:**
```typescript
const executeTransition = async (
  caseId: number, 
  transitionCode: string, 
  comments?: string
) => {
  const response = await fetch(
    `/api/cases/${caseId}/transitions/${transitionCode}`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ comments })
    }
  );
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to execute transition');
  }
  
  return await response.json();
};
```

---

## Module Forms

### 1. Get Form Schema and Data

**Endpoint:** `GET /api/cases/{caseId}/module-forms/{moduleType}/data`

**Response:**
```json
{
  "success": true,
  "message": "Module form schema and data retrieved",
  "data": {
    "schema": {
      "caseNatureId": 1,
      "caseNatureCode": "CIVIL",
      "moduleType": "HEARING",
      "fields": [
        {
          "fieldName": "hearingDate",
          "fieldLabel": "Hearing Date",
          "fieldType": "DATE",
          "isRequired": true,
          "validationRules": "{\"min\":\"today\",\"max\":\"2026-12-31\"}",
          "defaultValue": null,
          "placeholder": "Select hearing date",
          "helpText": "Select the date for the hearing"
        },
        {
          "fieldName": "hearingTime",
          "fieldLabel": "Hearing Time",
          "fieldType": "TIME",
          "isRequired": true
        },
        {
          "fieldName": "venue",
          "fieldLabel": "Venue",
          "fieldType": "TEXT",
          "isRequired": true
        }
      ],
      "totalFields": 3
    },
    "formData": {
      "hearingDate": "2026-02-15",
      "hearingTime": "10:00",
      "venue": "Court Room 1"
    },
    "hasExistingData": true
  }
}
```

**Frontend Implementation:**
```typescript
const getFormData = async (caseId: number, moduleType: string) => {
  const response = await fetch(
    `/api/cases/${caseId}/module-forms/${moduleType}/data`,
    {
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );
  const data = await response.json();
  return data.data; // { schema, formData, hasExistingData }
};

// Build form dynamically
const FormBuilder = ({ schema, formData, onSubmit }) => {
  const [formValues, setFormValues] = useState(formData || {});
  
  return (
    <form onSubmit={(e) => {
      e.preventDefault();
      onSubmit(formValues);
    }}>
      {schema.fields.map(field => (
        <FormField
          key={field.fieldName}
          field={field}
          value={formValues[field.fieldName]}
          onChange={(value) => 
            setFormValues({...formValues, [field.fieldName]: value})
          }
        />
      ))}
      <button type="submit">Submit</button>
    </form>
  );
};

// Form field component
const FormField = ({ field, value, onChange }) => {
  switch (field.fieldType) {
    case 'DATE':
      return (
        <input
          type="date"
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          required={field.isRequired}
          placeholder={field.placeholder}
        />
      );
    case 'TIME':
      return (
        <input
          type="time"
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          required={field.isRequired}
        />
      );
    case 'TEXT':
      return (
        <input
          type="text"
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          required={field.isRequired}
          placeholder={field.placeholder}
        />
      );
    // Add more field types as needed
    default:
      return <input type="text" value={value || ''} onChange={(e) => onChange(e.target.value)} />;
  }
};
```

### 2. Submit Form

**Endpoint:** `POST /api/cases/{caseId}/module-forms/{moduleType}/submit`

**Request Body:**
```json
{
  "formData": {
    "hearingDate": "2026-02-15",
    "hearingTime": "10:00",
    "venue": "Court Room 1"
  },
  "remarks": "Hearing scheduled as per request"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Module form submitted",
  "data": {
    "id": 45,
    "caseId": 18,
    "moduleType": "HEARING",
    "formData": "{\"hearingDate\":\"2026-02-15\",\"hearingTime\":\"10:00\",\"venue\":\"Court Room 1\"}",
    "submittedByOfficerId": 5,
    "submittedAt": "2026-01-28T10:30:00",
    "remarks": "Hearing scheduled as per request"
  }
}
```

**Frontend Implementation:**
```typescript
const submitForm = async (
  caseId: number,
  moduleType: string,
  formData: object,
  remarks?: string
) => {
  const response = await fetch(
    `/api/cases/${caseId}/module-forms/${moduleType}/submit`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        formData,
        remarks
      })
    }
  );
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to submit form');
  }
  
  return await response.json();
};

// Usage
const handleFormSubmit = async (formValues) => {
  try {
    await submitForm(caseId, 'HEARING', formValues, 'Hearing scheduled');
    // Refresh transitions to update checklist
    await refreshTransitions();
    // Show success message
  } catch (error) {
    // Show error message
  }
};
```

---

## Documents

### 1. Get Document Template

**Endpoint:** `GET /api/cases/{caseId}/documents/{moduleType}/template`

**Response:**
```json
{
  "success": true,
  "message": "Template retrieved",
  "data": {
    "id": 10,
    "templateName": "Notice Template",
    "templateHtml": "<html>...</html>",
    "templateData": "{...}",
    "isActive": true
  }
}
```

### 2. Get Latest Document

**Endpoint:** `GET /api/cases/{caseId}/documents/{moduleType}`

**Response:**
```json
{
  "success": true,
  "message": "Document retrieved",
  "data": {
    "id": 1,
    "caseId": 18,
    "moduleType": "NOTICE",
    "templateId": 10,
    "templateName": "Notice Template",
    "contentHtml": "<html>Notice content...</html>",
    "contentData": "{\"noticeNumber\":\"N001\",\"date\":\"2026-01-28\"}",
    "status": "DRAFT",
    "signedByOfficerId": null,
    "signedAt": null,
    "createdAt": "2026-01-28T10:00:00",
    "updatedAt": "2026-01-28T10:15:00"
  }
}
```

**Frontend Implementation:**
```typescript
const getDocument = async (caseId: number, moduleType: string) => {
  const response = await fetch(
    `/api/cases/${caseId}/documents/${moduleType}`,
    {
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );
  const data = await response.json();
  return data.data;
};

// Display document
const DocumentViewer = ({ document }) => {
  return (
    <div>
      <div>Status: {document.status}</div>
      <div dangerouslySetInnerHTML={{ __html: document.contentHtml }} />
      {document.status === 'DRAFT' && (
        <button onClick={() => openEditor(document)}>Edit Document</button>
      )}
      {document.status === 'DRAFT' && (
        <button onClick={() => finalizeDocument(document.id)}>
          Finalize
        </button>
      )}
    </div>
  );
};
```

### 3. Create/Update Document

**Endpoint:** `POST /api/cases/{caseId}/documents/{moduleType}`

**Request Body:**
```json
{
  "templateId": 10,
  "contentHtml": "<html>Notice content...</html>",
  "contentData": "{\"noticeNumber\":\"N001\",\"date\":\"2026-01-28\"}",
  "status": "DRAFT"
}
```

**Frontend Implementation:**
```typescript
const saveDocument = async (
  caseId: number,
  moduleType: string,
  document: {
    templateId?: number;
    contentHtml: string;
    contentData: string;
    status: 'DRAFT' | 'FINAL' | 'SIGNED';
  }
) => {
  const response = await fetch(
    `/api/cases/${caseId}/documents/${moduleType}`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(document)
    }
  );
  
  return await response.json();
};
```

### 4. Update Document by ID

**Endpoint:** `PUT /api/cases/{caseId}/documents/{moduleType}/{documentId}`

**Request Body:**
```json
{
  "templateId": 10,
  "contentHtml": "<html>Updated notice...</html>",
  "contentData": "{\"noticeNumber\":\"N001\",\"date\":\"2026-01-28\"}",
  "status": "FINAL"
}
```

**Frontend Implementation:**
```typescript
const updateDocument = async (
  caseId: number,
  moduleType: string,
  documentId: number,
  updates: {
    contentHtml?: string;
    contentData?: string;
    status?: 'DRAFT' | 'FINAL' | 'SIGNED';
  }
) => {
  const response = await fetch(
    `/api/cases/${caseId}/documents/${moduleType}/${documentId}`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updates)
    }
  );
  
  return await response.json();
};

// Finalize document
const finalizeDocument = async (caseId: number, documentId: number) => {
  const doc = await getDocument(caseId, 'NOTICE');
  await updateDocument(caseId, 'NOTICE', documentId, {
    ...doc,
    status: 'FINAL'
  });
  // Refresh transitions - NOTICE_READY flag should now be true
};
```

---

## Applicant Features

### 1. View Notice (Applicant)

**Endpoint:** `GET /api/citizen/cases/{caseId}/documents/NOTICE`

**Headers:**
```
X-User-Id: {applicantId}
```

**Response:**
```json
{
  "success": true,
  "message": "Notice retrieved",
  "data": {
    "id": 1,
    "caseId": 18,
    "moduleType": "NOTICE",
    "status": "FINAL",
    "contentHtml": "<html>Notice content...</html>",
    "contentData": "{...}",
    ...
  }
}
```

**Frontend Implementation:**
```typescript
const getNoticeForApplicant = async (caseId: number, applicantId: number) => {
  const response = await fetch(
    `/api/citizen/cases/${caseId}/documents/NOTICE`,
    {
      headers: {
        'X-User-Id': applicantId.toString()
      }
    }
  );
  
  if (response.status === 404) {
    // Notice not sent yet or not visible
    return null;
  }
  
  const data = await response.json();
  return data.data;
};

// Display notice to applicant
const ApplicantNoticeView = ({ caseId, applicantId }) => {
  const [notice, setNotice] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getNoticeForApplicant(caseId, applicantId)
      .then(setNotice)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [caseId, applicantId]);
  
  if (loading) return <div>Loading...</div>;
  if (!notice) return <div>Notice has not been sent to you yet.</div>;
  
  return (
    <div>
      <h2>Notice</h2>
      <div dangerouslySetInnerHTML={{ __html: notice.contentHtml }} />
      <button onClick={() => acceptNotice(caseId, applicantId)}>
        Accept/Receive Notice
      </button>
    </div>
  );
};
```

### 2. Accept/Receive Notice (Applicant)

**Endpoint:** `POST /api/citizen/cases/{caseId}/documents/NOTICE/accept?comments=Notice received`

**Headers:**
```
X-User-Id: {applicantId}
```

**Response:**
```json
{
  "success": true,
  "message": "Notice accepted successfully",
  "data": "Notice acceptance has been recorded in case history"
}
```

**Frontend Implementation:**
```typescript
const acceptNotice = async (
  caseId: number,
  applicantId: number,
  comments?: string
) => {
  const url = new URL(
    `/api/citizen/cases/${caseId}/documents/NOTICE/accept`,
    window.location.origin
  );
  if (comments) {
    url.searchParams.set('comments', comments);
  }
  
  const response = await fetch(url.toString(), {
    method: 'POST',
    headers: {
      'X-User-Id': applicantId.toString()
    }
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to accept notice');
  }
  
  return await response.json();
};

// Usage
const handleAcceptNotice = async () => {
  try {
    await acceptNotice(caseId, applicantId, 'Notice received and acknowledged');
    // Show success message
    // Optionally refresh case history
  } catch (error) {
    // Show error message
  }
};
```

### 3. Get Case History (Applicant)

**Endpoint:** `GET /api/cases/{caseId}/history`

**Response:**
```json
{
  "success": true,
  "message": "Workflow history retrieved",
  "data": [
    {
      "id": 100,
      "caseId": 18,
      "transitionCode": "SEND_NOTICE_TO_PARTY",
      "transitionName": "Send Notice to Party",
      "fromStateCode": "NOTICE_FINALIZED",
      "toStateCode": "NOTICE_SENT_TO_PARTY",
      "performedByRole": "CIRCLE_OFFICER",
      "performedAt": "2026-01-28T10:00:00",
      "comments": "Notice sent to applicant"
    },
    {
      "id": 101,
      "caseId": 18,
      "transitionCode": null,
      "transitionName": null,
      "fromStateCode": "NOTICE_SENT_TO_PARTY",
      "toStateCode": "NOTICE_SENT_TO_PARTY",
      "performedByRole": "CITIZEN",
      "performedAt": "2026-01-28T10:30:00",
      "comments": "Notice received and accepted by applicant",
      "metadata": "{\"action\":\"NOTICE_ACCEPTED\",\"type\":\"APPLICANT_ACKNOWLEDGMENT\"}"
    }
  ]
}
```

**Frontend Implementation:**
```typescript
const getCaseHistory = async (caseId: number) => {
  const response = await fetch(`/api/cases/${caseId}/history`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const data = await response.json();
  return data.data;
};

// Display history
const CaseHistory = ({ caseId }) => {
  const [history, setHistory] = useState([]);
  
  useEffect(() => {
    getCaseHistory(caseId).then(setHistory);
  }, [caseId]);
  
  return (
    <div>
      <h3>Case History</h3>
      {history.map(entry => (
        <div key={entry.id}>
          <div>{entry.performedAt}</div>
          <div>
            {entry.transitionName || 
             (entry.metadata && JSON.parse(entry.metadata).action === 'NOTICE_ACCEPTED' 
              ? 'Notice Accepted' 
              : 'Action')}
          </div>
          <div>By: {entry.performedByRole}</div>
          {entry.comments && <div>{entry.comments}</div>}
        </div>
      ))}
    </div>
  );
};
```

---

## Complete Workflow Flow Example

### Scenario: Set Hearing Date

```typescript
// 1. Get available transitions
const transitions = await getAvailableTransitions(caseId);

// 2. Find SET_HEARING_DATE transition
const hearingTransition = transitions.find(
  t => t.transitionCode === 'SET_HEARING_DATE'
);

// 3. Check if form is needed
if (hearingTransition.formSchema) {
  // 4. Get form schema and existing data
  const formData = await getFormData(caseId, 'HEARING');
  
  // 5. Show form to user
  // Use formData.schema.fields to build form
  // Pre-fill with formData.formData if hasExistingData is true
  
  // 6. User fills and submits form
  await submitForm(caseId, 'HEARING', {
    hearingDate: '2026-02-15',
    hearingTime: '10:00',
    venue: 'Court Room 1'
  });
  
  // 7. Refresh transitions - checklist should now show HEARING_SUBMITTED = true
  const updatedTransitions = await getAvailableTransitions(caseId);
  const updatedHearingTransition = updatedTransitions.find(
    t => t.transitionCode === 'SET_HEARING_DATE'
  );
  
  // 8. Check if transition can be executed
  if (updatedHearingTransition.checklist.canExecute) {
    // 9. Execute transition
    await executeTransition(caseId, 'SET_HEARING_DATE', 
      'Hearing date set for 2026-02-15');
  }
}
```

---

## Notice Workflow Example

### Scenario: DA → Circle Officer → Applicant

```typescript
// Step 1: DA prepares draft notice
// 1.1 Get template
const template = await fetch(
  `/api/cases/${caseId}/documents/NOTICE/template`
).then(r => r.json());

// 1.2 Create draft document
await saveDocument(caseId, 'NOTICE', {
  templateId: template.data.id,
  contentHtml: generateNoticeHtml(template.data, caseData),
  contentData: JSON.stringify(noticeData),
  status: 'DRAFT'
});

// 1.3 Submit notice form (if required)
await submitForm(caseId, 'NOTICE', {
  noticeType: 'HEARING_NOTICE',
  noticeDate: '2026-01-28'
});

// Step 2: DA forwards to Circle Officer
// 2.1 Check checklist
const checklist = await getChecklist(caseId, 'FORWARD_DRAFT_NOTICE_TO_CO');

// 2.2 Execute transition
if (checklist.canExecute) {
  await executeTransition(caseId, 'FORWARD_DRAFT_NOTICE_TO_CO',
    'Forwarding draft notice for review');
}

// Step 3: Circle Officer reviews
// 3.1 Get document
const document = await getDocument(caseId, 'NOTICE');

// 3.2 Update document status to FINAL
await updateDocument(caseId, 'NOTICE', document.id, {
  ...document,
  status: 'FINAL'
});

// 3.3 Finalize notice
await executeTransition(caseId, 'FINALIZE_NOTICE',
  'Notice finalized and ready');

// 3.4 Send to party
await executeTransition(caseId, 'SEND_NOTICE_TO_PARTY',
  'Notice sent to applicant');

// Step 4: Applicant views and accepts
// 4.1 Applicant views notice
const notice = await getNoticeForApplicant(caseId, applicantId);

// 4.2 Applicant accepts notice
await acceptNotice(caseId, applicantId, 'Notice received');
```

---

## Error Handling

### Common Error Responses

**400 Bad Request:**
```json
{
  "success": false,
  "message": "Case ID cannot be null",
  "errors": null
}
```

**401 Unauthorized:**
```json
{
  "success": false,
  "message": "User ID not found. Please provide X-User-Id header or valid JWT token."
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "message": "You do not have access to this case"
}
```

**404 Not Found:**
```json
{
  "success": false,
  "message": "Notice has not been sent to you yet. Current state: NOTICE_GENERATED"
}
```

**500 Internal Server Error:**
```json
{
  "timestamp": "2026-01-28T10:14:56.5401216",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please contact support."
}
```

### Frontend Error Handling

```typescript
const handleApiCall = async (apiCall: () => Promise<Response>) => {
  try {
    const response = await apiCall();
    
    if (!response.ok) {
      const error = await response.json();
      
      switch (response.status) {
        case 400:
          // Bad request - show validation errors
          showError(error.message);
          break;
        case 401:
          // Unauthorized - redirect to login
          redirectToLogin();
          break;
        case 403:
          // Forbidden - show access denied
          showError('You do not have permission to perform this action');
          break;
        case 404:
          // Not found - show not found message
          showError(error.message || 'Resource not found');
          break;
        case 500:
          // Server error - show generic error
          showError('An error occurred. Please try again later.');
          break;
        default:
          showError(error.message || 'An unexpected error occurred');
      }
      
      throw new Error(error.message);
    }
    
    return await response.json();
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
};

// Usage
try {
  const transitions = await handleApiCall(() =>
    fetch(`/api/cases/${caseId}/transitions`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
  );
} catch (error) {
  // Error already handled in handleApiCall
}
```

---

## State Management Patterns

### React Example with Context

```typescript
// WorkflowContext.tsx
import React, { createContext, useContext, useState, useEffect } from 'react';

interface WorkflowState {
  transitions: WorkflowTransitionDTO[];
  currentState: string;
  loading: boolean;
  error: string | null;
}

const WorkflowContext = createContext<WorkflowState | null>(null);

export const WorkflowProvider = ({ caseId, children }) => {
  const [state, setState] = useState<WorkflowState>({
    transitions: [],
    currentState: '',
    loading: true,
    error: null
  });
  
  useEffect(() => {
    loadTransitions();
  }, [caseId]);
  
  const loadTransitions = async () => {
    try {
      setState(prev => ({ ...prev, loading: true, error: null }));
      const transitions = await getAvailableTransitions(caseId);
      const currentState = transitions[0]?.fromStateCode || '';
      setState({
        transitions,
        currentState,
        loading: false,
        error: null
      });
    } catch (error) {
      setState(prev => ({
        ...prev,
        loading: false,
        error: error.message
      }));
    }
  };
  
  return (
    <WorkflowContext.Provider value={{ ...state, refresh: loadTransitions }}>
      {children}
    </WorkflowContext.Provider>
  );
};

export const useWorkflow = () => {
  const context = useContext(WorkflowContext);
  if (!context) {
    throw new Error('useWorkflow must be used within WorkflowProvider');
  }
  return context;
};

// Usage in component
const CaseActions = () => {
  const { transitions, loading, refresh } = useWorkflow();
  
  const handleExecuteTransition = async (transitionCode: string) => {
    await executeTransition(caseId, transitionCode);
    await refresh(); // Reload transitions after execution
  };
  
  return (
    <div>
      {transitions.map(transition => (
        <TransitionCard
          key={transition.id}
          transition={transition}
          onExecute={handleExecuteTransition}
        />
      ))}
    </div>
  );
};
```

### Vue.js Example

```typescript
// useWorkflow.ts
import { ref, computed } from 'vue';

export const useWorkflow = (caseId: number) => {
  const transitions = ref<WorkflowTransitionDTO[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  
  const loadTransitions = async () => {
    loading.value = true;
    error.value = null;
    try {
      transitions.value = await getAvailableTransitions(caseId);
    } catch (err) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  };
  
  const executeTransition = async (transitionCode: string, comments?: string) => {
    try {
      await executeTransition(caseId, transitionCode, comments);
      await loadTransitions(); // Refresh
    } catch (err) {
      error.value = err.message;
    }
  };
  
  return {
    transitions,
    loading,
    error,
    loadTransitions,
    executeTransition
  };
};

// Usage in component
<script setup lang="ts">
import { useWorkflow } from '@/composables/useWorkflow';

const props = defineProps<{ caseId: number }>();
const { transitions, loading, executeTransition } = useWorkflow(props.caseId);

onMounted(() => {
  loadTransitions();
});
</script>
```

---

## UI Component Examples

### Transition Card Component

```typescript
interface TransitionCardProps {
  transition: WorkflowTransitionDTO;
  onExecute: (transitionCode: string) => void;
}

const TransitionCard = ({ transition, onExecute }: TransitionCardProps) => {
  const [showChecklist, setShowChecklist] = useState(false);
  const [showForm, setShowForm] = useState(false);
  
  return (
    <div className="transition-card">
      <h3>{transition.transitionName}</h3>
      <p>{transition.description}</p>
      
      {/* Checklist */}
      {transition.checklist && (
        <div>
          <button onClick={() => setShowChecklist(!showChecklist)}>
            {showChecklist ? 'Hide' : 'Show'} Requirements
          </button>
          {showChecklist && (
            <ChecklistComponent checklist={transition.checklist} />
          )}
        </div>
      )}
      
      {/* Form Schema */}
      {transition.formSchema && (
        <div>
          <button onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Hide' : 'Show'} Form
          </button>
          {showForm && (
            <FormBuilder
              schema={transition.formSchema}
              formData={null}
              onSubmit={async (formData) => {
                await submitForm(caseId, transition.formSchema.moduleType, formData);
                setShowForm(false);
                // Refresh transitions
              }}
            />
          )}
        </div>
      )}
      
      {/* Execute Button */}
      <button
        onClick={() => onExecute(transition.transitionCode)}
        disabled={!transition.checklist?.canExecute}
        title={transition.checklist?.blockingReasons?.join(', ')}
      >
        {transition.transitionName}
      </button>
    </div>
  );
};
```

### Checklist Component

```typescript
const ChecklistComponent = ({ checklist }) => {
  return (
    <div className="checklist">
      <h4>Requirements</h4>
      <ul>
        {checklist.conditions.map((condition, idx) => (
          <li
            key={idx}
            className={condition.passed ? 'passed' : 'failed'}
          >
            <span>{condition.passed ? '✓' : '✗'}</span>
            <span>{condition.message}</span>
            {condition.moduleType && !condition.passed && (
              <button onClick={() => openForm(condition.moduleType)}>
                Fill {condition.moduleType} Form
              </button>
            )}
          </li>
        ))}
      </ul>
      
      {!checklist.canExecute && (
        <div className="blocking-reasons">
          <strong>Cannot execute:</strong>
          <ul>
            {checklist.blockingReasons.map((reason, idx) => (
              <li key={idx}>{reason}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
```

---

## API Endpoint Summary

### Officer Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cases/{caseId}/transitions` | Get available transitions with checklist and form schema |
| GET | `/api/cases/{caseId}/transitions/{transitionCode}/checklist` | Get transition checklist |
| POST | `/api/cases/{caseId}/transitions/{transitionCode}` | Execute transition |
| GET | `/api/cases/{caseId}/module-forms/{moduleType}/data` | Get form schema and data |
| POST | `/api/cases/{caseId}/module-forms/{moduleType}/submit` | Submit form |
| GET | `/api/cases/{caseId}/documents/{moduleType}` | Get latest document |
| GET | `/api/cases/{caseId}/documents/{moduleType}/template` | Get document template |
| POST | `/api/cases/{caseId}/documents/{moduleType}` | Create/update document |
| PUT | `/api/cases/{caseId}/documents/{moduleType}/{documentId}` | Update document by ID |
| GET | `/api/cases/{caseId}/history` | Get case history |

### Applicant/Citizen Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/citizen/cases/{caseId}/documents/NOTICE` | View notice (if sent) |
| POST | `/api/citizen/cases/{caseId}/documents/NOTICE/accept` | Accept/receive notice |
| GET | `/api/cases/{caseId}/history` | Get case history (includes applicant actions) |

---

## TypeScript Type Definitions

```typescript
// Workflow Types
interface WorkflowTransitionDTO {
  id: number;
  transitionCode: string;
  transitionName: string;
  fromStateCode: string;
  toStateCode: string;
  requiresComment: boolean;
  description: string;
  checklist?: TransitionChecklistDTO;
  formSchema?: ModuleFormSchemaDTO;
}

interface TransitionChecklistDTO {
  transitionCode: string;
  transitionName: string;
  canExecute: boolean;
  conditions: ConditionStatusDTO[];
  blockingReasons: string[];
}

interface ConditionStatusDTO {
  label: string;
  type: 'WORKFLOW_FLAG' | 'FORM_FIELD' | 'CASE_DATA_FIELD';
  flagName?: string;
  moduleType?: string;
  fieldName?: string;
  required: boolean;
  passed: boolean;
  message: string;
}

// Form Types
interface ModuleFormSchemaDTO {
  caseNatureId: number;
  caseNatureCode: string;
  caseNatureName: string;
  caseTypeId?: number;
  caseTypeCode?: string;
  caseTypeName?: string;
  moduleType: 'HEARING' | 'NOTICE' | 'ORDERSHEET' | 'JUDGEMENT';
  fields: ModuleFormFieldDTO[];
  totalFields: number;
}

interface ModuleFormFieldDTO {
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  isRequired: boolean;
  validationRules?: string;
  defaultValue?: string;
  placeholder?: string;
  helpText?: string;
  displayOrder: number;
}

interface ModuleFormWithDataDTO {
  schema: ModuleFormSchemaDTO;
  formData: Record<string, any> | null;
  hasExistingData: boolean;
}

// Document Types
interface CaseDocumentDTO {
  id: number;
  caseId: number;
  caseNatureId: number;
  moduleType: 'HEARING' | 'NOTICE' | 'ORDERSHEET' | 'JUDGEMENT';
  templateId?: number;
  templateName?: string;
  contentHtml: string;
  contentData: string;
  status: 'DRAFT' | 'FINAL' | 'SIGNED';
  signedByOfficerId?: number;
  signedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// History Types
interface WorkflowHistoryDTO {
  id: number;
  caseId: number;
  transitionCode?: string;
  transitionName?: string;
  fromStateCode: string;
  toStateCode: string;
  performedByOfficerId?: number;
  performedByRole: string;
  performedAt: string;
  comments?: string;
  metadata?: string;
}

// API Response Types
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
  errors?: any;
}
```

---

## Best Practices

### 1. Caching Strategy

```typescript
// Cache transitions and refresh after actions
const useCachedTransitions = (caseId: number) => {
  const [cache, setCache] = useState<Map<number, WorkflowTransitionDTO[]>>(new Map());
  
  const getTransitions = async (refresh = false) => {
    if (!refresh && cache.has(caseId)) {
      return cache.get(caseId)!;
    }
    
    const transitions = await getAvailableTransitions(caseId);
    setCache(new Map(cache.set(caseId, transitions)));
    return transitions;
  };
  
  return { getTransitions, invalidate: () => cache.delete(caseId) };
};
```

### 2. Optimistic Updates

```typescript
const executeTransitionOptimistic = async (
  caseId: number,
  transitionCode: string,
  comments?: string
) => {
  // Update UI immediately
  updateUIOptimistically(transitionCode);
  
  try {
    await executeTransition(caseId, transitionCode, comments);
    // Refresh to get actual state
    await refreshTransitions();
  } catch (error) {
    // Revert optimistic update
    revertUIUpdate();
    throw error;
  }
};
```

### 3. Polling for Updates

```typescript
// Poll for transition updates when case is active
useEffect(() => {
  if (!isCaseActive) return;
  
  const interval = setInterval(async () => {
    const transitions = await getAvailableTransitions(caseId);
    setTransitions(transitions);
  }, 30000); // Poll every 30 seconds
  
  return () => clearInterval(interval);
}, [caseId, isCaseActive]);
```

---

## Testing Examples

### Unit Test Example

```typescript
describe('Workflow Integration', () => {
  it('should fetch and display transitions', async () => {
    const mockTransitions = [
      {
        id: 136,
        transitionCode: 'SET_HEARING_DATE',
        transitionName: 'Set Hearing Date',
        checklist: {
          canExecute: false,
          conditions: [
            {
              type: 'FORM_FIELD',
              moduleType: 'HEARING',
              passed: false,
              message: 'Hearing form submitted must be completed'
            }
          ]
        }
      }
    ];
    
    // Mock API call
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: mockTransitions })
    });
    
    const transitions = await getAvailableTransitions(18);
    expect(transitions).toEqual(mockTransitions);
  });
});
```

---

## Quick Reference

### Common Workflows

**1. Fill Form → Submit → Execute Transition:**
```
Get transitions → Show form → Submit form → Refresh transitions → Execute transition
```

**2. Create Document → Finalize → Send:**
```
Get template → Create document (DRAFT) → Update to FINAL → Execute SEND transition
```

**3. Applicant View Notice:**
```
Check workflow state → Get notice → Display → Accept notice
```

### Module Types
- `HEARING` - Hearing scheduling form
- `NOTICE` - Notice document
- `ORDERSHEET` - Ordersheet document
- `JUDGEMENT` - Judgement document

### Document Statuses
- `DRAFT` - Being edited
- `FINAL` - Finalized (sets `{MODULE}_READY` flag)
- `SIGNED` - Signed (also sets `{MODULE}_READY` flag)

### Workflow Flags
- `{MODULE}_SUBMITTED` - Form submitted (e.g., `HEARING_SUBMITTED`)
- `{MODULE}_DRAFT_CREATED` - Draft document created (e.g., `NOTICE_DRAFT_CREATED`)
- `{MODULE}_READY` - Document ready (e.g., `NOTICE_READY`)

---

This guide provides all the information needed to implement the frontend. For backend API details, refer to `WORKFLOW_EXPLANATION.md` and `NOTICE_WORKFLOW_GUIDE.md`.
