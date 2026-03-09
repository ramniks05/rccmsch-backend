# Frontend Implementation Guide: Attendance Marking

## Overview

This guide explains how to implement the attendance marking feature in the frontend application. The system allows proceeding officers to mark attendance of parties (petitioner and respondent) during case proceedings, with support for proxy attendance.

---

## Implementation Flow

### Step 1: When to Show Attendance Form

**Trigger Points:**
- When case is in `PROCEEDINGS_IN_PROGRESS` state
- When officer clicks "Mark Attendance" action/button
- When workflow transition requires attendance (checklist shows `ATTENDANCE_SUBMITTED` is required)
- On hearing date when recording daily proceedings

**Where to Place:**
- As a modal/dialog that opens when officer clicks "Mark Attendance"
- As a dedicated page/component accessible from case details page
- As part of the proceedings recording workflow

---

### Step 2: Load Parties Data

**API Call:** `GET /api/cases/{caseId}/parties`

**Purpose:** Get list of parties (petitioner, respondent) for the case

**What to Do:**
1. Call this API when attendance form opens
2. Store the response in component state
3. Display each party as a row/item in the form

**Response Structure:**
- `caseId`: Case ID
- `caseNumber`: Case number for display
- `parties`: Array of party objects with:
  - `partyId`: Unique identifier ("petitioner", "respondent")
  - `partyName`: Name of the party
  - `partyType`: "PETITIONER" or "RESPONDENT"
  - `partyLabel`: Display label ("Petitioner", "Respondent")

**Error Handling:**
- If API fails, show error message
- If no parties found, show message: "No parties found for this case"
- Fallback: Use applicant name as petitioner if parties array is empty

---

### Step 3: Load Attendance Form Schema (Optional)

**API Call:** `GET /api/cases/{caseId}/module-forms/ATTENDANCE`

**Purpose:** Get form field definitions and validation rules

**What to Do:**
1. Call this API to understand required fields
2. Use field definitions to build dynamic form
3. Apply validation rules from schema

**When to Use:**
- If you want to build form dynamically based on schema
- If validation rules need to be enforced
- If field order/display needs to follow schema

**Note:** If form structure is fixed, you can skip this step and hardcode the form fields.

---

### Step 4: Load Existing Attendance (Optional)

**API Call:** `GET /api/cases/{caseId}/module-forms/ATTENDANCE/latest`

**Purpose:** Get previously submitted attendance data (if exists)

**What to Do:**
1. Call this API when form opens
2. If data exists, populate form with existing values
3. Allow officer to edit and resubmit
4. Show "Last marked on: [date]" if data exists

**Use Cases:**
- Editing attendance for same hearing date
- Viewing attendance history
- Pre-filling form for convenience

---

### Step 5: Build Attendance Form UI

**Form Structure:**

1. **Attendance Date Field**
   - Input type: Date picker
   - Default value: Today's date
   - Required: Yes
   - Purpose: Record which date attendance is being marked

2. **Parties Section**
   - Display each party from Step 2 as a card/row
   - For each party, show:
     - **Party Name** (bold/large text)
     - **Party Type** (Petitioner/Respondent badge)
     - **Present Checkbox**: Main checkbox to mark party as present
     - **Proxy Section** (shown only if "Present" is checked):
       - Checkbox: "Represented by Proxy"
       - Text input: "Proxy Name" (shown if proxy checkbox is checked)
     - **Remarks Textarea**: Optional remarks for this party

3. **General Remarks Field**
   - Large textarea
   - Optional
   - Purpose: Overall remarks about attendance

**UI Layout Suggestions:**
- Use card-based layout for each party
- Group related fields (proxy checkbox + proxy name input)
- Use collapsible sections if many parties
- Show visual indicators (green checkmark for present, red X for absent)

---

### Step 6: Handle Form Interactions

**Checkbox Logic:**

1. **"Present" Checkbox:**
   - When checked: Show proxy section and remarks field
   - When unchecked: Hide proxy section, clear proxy data

2. **"Proxy" Checkbox:**
   - When checked: Show proxy name input field
   - When unchecked: Hide proxy name input, clear proxy name

3. **State Management:**
   - Maintain state object with:
     - `attendanceDate`: Date value
     - `parties`: Array of party attendance objects
     - `remarks`: General remarks
   - Each party object should have:
     - `partyId`: From loaded parties
     - `partyName`: From loaded parties
     - `partyType`: From loaded parties
     - `isPresent`: Boolean (from checkbox)
     - `isProxy`: Boolean (from proxy checkbox)
     - `proxyName`: String (from input, if proxy)
     - `remarks`: String (from textarea)

---

### Step 7: Validate Form Data

**Validation Rules:**

1. **Required Fields:**
   - Attendance date must be selected
   - At least one party must be marked as present (optional - depends on business rules)

2. **Conditional Validation:**
   - If "Proxy" is checked, "Proxy Name" must be filled
   - If party is marked present, proxy name should be required if proxy is checked

3. **Date Validation:**
   - Attendance date should not be in future (optional)
   - Attendance date should be on or after case application date (optional)

**Error Display:**
- Show inline errors below fields
- Highlight fields with errors (red border)
- Show summary of errors at top of form (optional)

---

### Step 8: Submit Attendance Form

**API Call:** `POST /api/cases/{caseId}/module-forms/ATTENDANCE/submit`

**Request Body Structure:**
```json
{
  "formData": "JSON string containing attendance data",
  "remarks": "General remarks (optional)"
}
```

**formData JSON Structure:**
```json
{
  "attendanceDate": "2026-03-09",
  "parties": [
    {
      "partyId": "petitioner",
      "partyName": "Ram Kumar",
      "partyType": "PETITIONER",
      "isPresent": true,
      "isProxy": false,
      "proxyName": null,
      "remarks": null
    },
    {
      "partyId": "respondent",
      "partyName": "Shyam Singh",
      "partyType": "RESPONDENT",
      "isPresent": true,
      "isProxy": true,
      "proxyName": "Mohan Das",
      "remarks": "Authorized representative"
    }
  ],
  "remarks": "Both parties present. Respondent represented by proxy."
}
```

**What to Do:**

1. **Prepare Data:**
   - Convert form state to JSON string
   - Include all parties (both present and absent)
   - Include attendance date
   - Include general remarks

2. **Make API Call:**
   - Use POST method
   - Include authentication token in headers
   - Send JSON body with `formData` and `remarks`

3. **Handle Response:**
   - On success: Show success message, close modal/form, refresh case data
   - On error: Show error message, keep form open for correction

4. **Update UI:**
   - After successful submission:
     - Show success notification
     - Close attendance form modal
     - Refresh case details/workflow status
     - Update workflow checklist (if attendance was required)

---

### Step 9: Integration with Workflow

**Check Workflow Checklist:**

1. **Before Showing Form:**
   - Check if current transition requires `ATTENDANCE_SUBMITTED`
   - Show "Mark Attendance" button only if required or if officer wants to mark

2. **After Submission:**
   - Refresh workflow transitions list
   - Check if `ATTENDANCE_SUBMITTED` flag is now set
   - Enable next transition if checklist passes

**Workflow Integration Points:**

- **Case Details Page:** Show "Mark Attendance" button when in proceedings state
- **Transition Checklist:** Show attendance requirement as condition
- **After Attendance:** Automatically refresh transition checklist

---

### Step 10: Display Attendance History (Optional)

**API Call:** `GET /api/cases/{caseId}/module-forms/ATTENDANCE/latest`

**Purpose:** Show previously marked attendance

**What to Display:**

1. **Attendance History Section:**
   - List of all attendance submissions (if multiple submissions API exists)
   - Or show latest attendance with date
   - Display: Date, Parties present, Proxy info, Remarks

2. **Visual Indicators:**
   - Green badge for present parties
   - Red badge for absent parties
   - Blue badge for proxy representation

**Where to Show:**
- In case details page
- In proceedings history
- As a separate "Attendance History" tab

---

## UI/UX Best Practices

### 1. Form Layout

- **Group Related Fields:** Keep party info, proxy info, and remarks together
- **Clear Visual Hierarchy:** Use headings, spacing, and borders
- **Responsive Design:** Ensure form works on mobile devices
- **Accessibility:** Use proper labels, ARIA attributes, keyboard navigation

### 2. User Experience

- **Progressive Disclosure:** Show proxy fields only when needed
- **Clear Actions:** Use descriptive button labels ("Mark Attendance", "Save Attendance")
- **Confirmation:** Show confirmation dialog before submitting (optional)
- **Loading States:** Show loading spinner during API calls
- **Success Feedback:** Show clear success message after submission

### 3. Error Handling

- **Inline Errors:** Show errors next to fields
- **Validation:** Validate before submission
- **API Errors:** Show user-friendly error messages
- **Retry:** Allow retry on network errors

### 4. Data Display

- **Party Cards:** Use card layout for each party
- **Checkbox States:** Clear visual indication of checked/unchecked
- **Proxy Info:** Highlight proxy representation differently
- **Date Display:** Use readable date format

---

## Component Structure Suggestion

### Recommended Component Hierarchy:

```
AttendanceForm (Main Component)
├── AttendanceDatePicker
├── PartiesList
│   ├── PartyCard (for each party)
│   │   ├── PartyInfo (name, type)
│   │   ├── PresentCheckbox
│   │   ├── ProxySection (conditional)
│   │   │   ├── ProxyCheckbox
│   │   │   └── ProxyNameInput
│   │   └── PartyRemarksTextarea
├── GeneralRemarksTextarea
└── SubmitButton
```

### State Management:

- **Local State:** Use component state for form data
- **Form State:** Track each party's attendance status
- **Validation State:** Track validation errors
- **Loading State:** Track API call status

---

## Integration Points

### 1. Case Details Page

- Add "Mark Attendance" button
- Show attendance status badge
- Link to attendance history

### 2. Workflow Transitions

- Check if attendance is required
- Show attendance requirement in checklist
- Enable transition after attendance submission

### 3. Proceedings Recording

- Integrate attendance marking with proceedings
- Link attendance date with hearing date
- Show attendance summary in proceedings view

---

## Testing Checklist

### Functional Testing:

- [ ] Load parties successfully
- [ ] Display all parties correctly
- [ ] Mark party as present
- [ ] Mark party as absent
- [ ] Enable proxy checkbox when present
- [ ] Enter proxy name
- [ ] Add remarks for party
- [ ] Add general remarks
- [ ] Validate required fields
- [ ] Submit form successfully
- [ ] Handle API errors gracefully
- [ ] Load existing attendance data
- [ ] Edit and resubmit attendance

### UI/UX Testing:

- [ ] Form is responsive on mobile
- [ ] Checkboxes are clearly visible
- [ ] Proxy section shows/hides correctly
- [ ] Error messages are clear
- [ ] Success message appears after submission
- [ ] Loading states are shown during API calls

### Integration Testing:

- [ ] Attendance form opens from case details
- [ ] Workflow checklist updates after submission
- [ ] Next transition enables after attendance
- [ ] Attendance history displays correctly

---

## Common Scenarios

### Scenario 1: Both Parties Present

**Flow:**
1. Officer opens attendance form
2. Sees petitioner and respondent listed
3. Checks "Present" for both parties
4. Leaves proxy unchecked for both
5. Adds general remarks: "Both parties present"
6. Submits form

**Result:** Attendance marked, workflow flag set, next transition enabled

---

### Scenario 2: Respondent Represented by Proxy

**Flow:**
1. Officer opens attendance form
2. Checks "Present" for petitioner (normal)
3. Checks "Present" for respondent
4. Checks "Proxy" for respondent
5. Enters proxy name: "Mohan Das"
6. Adds remarks: "Respondent represented by authorized agent"
7. Submits form

**Result:** Attendance marked with proxy info, workflow flag set

---

### Scenario 3: One Party Absent

**Flow:**
1. Officer opens attendance form
2. Checks "Present" for petitioner
3. Leaves respondent unchecked (absent)
4. Adds remarks: "Respondent absent, notice served"
5. Submits form

**Result:** Attendance marked with absent party, workflow continues

---

### Scenario 4: Editing Existing Attendance

**Flow:**
1. Officer opens attendance form
2. System loads existing attendance data
3. Officer sees previously marked attendance
4. Officer updates proxy information
5. Resubmits form

**Result:** Updated attendance saved, previous data overwritten

---

## API Summary

### Required APIs:

1. **GET `/api/cases/{caseId}/parties`**
   - Purpose: Get parties list
   - When: On form open
   - Response: CasePartiesDTO

2. **GET `/api/cases/{caseId}/module-forms/ATTENDANCE`** (Optional)
   - Purpose: Get form schema
   - When: On form open (if dynamic form)
   - Response: ModuleFormSchemaDTO

3. **GET `/api/cases/{caseId}/module-forms/ATTENDANCE/latest`** (Optional)
   - Purpose: Get existing attendance
   - When: On form open (to pre-fill)
   - Response: ModuleFormSubmissionDTO

4. **POST `/api/cases/{caseId}/module-forms/ATTENDANCE/submit`**
   - Purpose: Submit attendance
   - When: On form submit
   - Request: CreateModuleFormSubmissionDTO
   - Response: ModuleFormSubmissionDTO

---

## Key Points to Remember

1. **Party Extraction:** Parties come from case data, system handles extraction automatically
2. **Form Data Format:** Attendance data must be JSON string in `formData` field
3. **Workflow Integration:** Submission sets `ATTENDANCE_SUBMITTED` flag automatically
4. **Proxy Handling:** Proxy checkbox and name input are conditional fields
5. **State Management:** Track each party's attendance status separately
6. **Validation:** Validate proxy name if proxy checkbox is checked
7. **Error Handling:** Show user-friendly errors for API failures
8. **Success Feedback:** Clear indication when attendance is saved

---

## Next Steps

1. **Design UI Mockups:** Create wireframes for attendance form
2. **Implement Components:** Build form components based on this guide
3. **Integrate APIs:** Connect frontend to backend APIs
4. **Add Validation:** Implement form validation logic
5. **Test Flow:** Test complete attendance marking flow
6. **Polish UI:** Improve styling and user experience
7. **Add History View:** Implement attendance history display (optional)

---

**Document Version:** 1.0  
**Last Updated:** 2026-03-09  
**For:** Chandigarh RCCMS Frontend Implementation
