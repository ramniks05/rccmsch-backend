# Attendance Marking Workflow - Business Logic & Implementation Guide

## Overview

The attendance marking system allows proceeding officers (Tehsildar) to mark attendance of parties (petitioner and respondent) during case proceedings. The system supports:
- ✅ Marking parties as present/absent using checkboxes
- ✅ Proxy attendance (when a party is represented by someone else)
- ✅ Multiple parties (petitioner, respondent, and custom parties)
- ✅ Integration with workflow transitions
- ✅ Historical tracking of attendance records

---

## Business Logic Flow

### 1. **When to Mark Attendance**

Attendance marking is typically done:
- **During Proceedings**: When case is in `PROCEEDINGS_IN_PROGRESS` state
- **On Hearing Date**: When recording daily proceedings
- **Before Final Order**: To ensure all parties were present/notified

### 2. **Party Extraction**

Parties are extracted from `case.caseData` JSON field. The system looks for common field names:
- **Petitioner**: `petitionerName`, `petitioner_name`, `petitioner`, `applicantName`, `applicant_name`
- **Respondent**: `respondentName`, `respondent_name`, `respondent`, `oppositePartyName`, `opposite_party_name`

**Fallback**: If no parties found in `caseData`, the system uses the `applicant` name as the petitioner.

### 3. **Attendance Form Structure**

The attendance form is a **module form** of type `ATTENDANCE`. It stores data as JSON:

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
      "remarks": "Represented by authorized agent"
    }
  ],
  "remarks": "Both parties present. Respondent represented by proxy."
}
```

### 4. **Workflow Integration**

When attendance form is submitted:
1. Form data is saved to `case_module_form_submissions` table
2. Workflow flag `ATTENDANCE_SUBMITTED = true` is set in `case_workflow_instance.workflow_data`
3. This flag can be used in workflow checklist conditions

**Example Workflow Condition:**
```json
{
  "workflowDataFieldsRequired": ["ATTENDANCE_SUBMITTED"]
}
```

---

## API Endpoints

### 1. Get Parties for Case

**Endpoint:** `GET /api/cases/{caseId}/parties`

**Description:** Retrieves list of parties (petitioner, respondent) for a case to display in attendance form.

**Response:**
```json
{
  "success": true,
  "message": "Case parties retrieved successfully",
  "data": {
    "caseId": 123,
    "caseNumber": "PARTITION_CASES-CHD_DIST_01-20260224-0002",
    "parties": [
      {
        "partyId": "petitioner",
        "partyName": "Ram Kumar",
        "partyType": "PETITIONER",
        "partyLabel": "Petitioner"
      },
      {
        "partyId": "respondent",
        "partyName": "Shyam Singh",
        "partyType": "RESPONDENT",
        "partyLabel": "Respondent"
      }
    ]
  }
}
```

### 2. Get Attendance Form Schema

**Endpoint:** `GET /api/cases/{caseId}/module-forms/ATTENDANCE`

**Description:** Gets the form schema for attendance form (fields, validation rules, etc.)

**Response:**
```json
{
  "success": true,
  "message": "Module form schema retrieved",
  "data": {
    "caseNatureId": 1,
    "caseNatureCode": "PARTITION_CASES",
    "caseNatureName": "Partition Cases",
    "caseTypeId": 1,
    "caseTypeCode": "NEW_FILE",
    "caseTypeName": "New File",
    "moduleType": "ATTENDANCE",
    "fields": [
      {
        "fieldName": "attendanceDate",
        "fieldLabel": "Attendance Date",
        "fieldType": "DATE",
        "isRequired": true,
        "displayOrder": 1
      },
      {
        "fieldName": "remarks",
        "fieldLabel": "Remarks",
        "fieldType": "TEXTAREA",
        "isRequired": false,
        "displayOrder": 2
      }
    ],
    "totalFields": 2
  }
}
```

### 3. Get Latest Attendance Submission

**Endpoint:** `GET /api/cases/{caseId}/module-forms/ATTENDANCE/latest`

**Description:** Gets the latest attendance submission for a case (if exists)

**Response:**
```json
{
  "success": true,
  "message": "Latest submission retrieved",
  "data": {
    "id": 45,
    "caseId": 123,
    "caseNatureId": 1,
    "moduleType": "ATTENDANCE",
    "formData": "{\"attendanceDate\":\"2026-03-09\",\"parties\":[...]}",
    "submittedByOfficerId": 10,
    "submittedAt": "2026-03-09T10:30:00",
    "remarks": "Both parties present"
  }
}
```

### 4. Submit Attendance Form

**Endpoint:** `POST /api/cases/{caseId}/module-forms/ATTENDANCE/submit`

**Description:** Submit attendance marking form with party attendance data.

**Request Body:**
```json
{
  "formData": "{\"attendanceDate\":\"2026-03-09\",\"parties\":[{\"partyId\":\"petitioner\",\"partyName\":\"Ram Kumar\",\"partyType\":\"PETITIONER\",\"isPresent\":true,\"isProxy\":false,\"proxyName\":null,\"remarks\":null},{\"partyId\":\"respondent\",\"partyName\":\"Shyam Singh\",\"partyType\":\"RESPONDENT\",\"isPresent\":true,\"isProxy\":true,\"proxyName\":\"Mohan Das\",\"remarks\":\"Represented by authorized agent\"}],\"remarks\":\"Both parties present. Respondent represented by proxy.\"}",
  "remarks": "Attendance marked for hearing date 2026-03-09"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Module form submitted",
  "data": {
    "id": 45,
    "caseId": 123,
    "caseNatureId": 1,
    "moduleType": "ATTENDANCE",
    "formData": "...",
    "submittedByOfficerId": 10,
    "submittedAt": "2026-03-09T10:30:00",
    "remarks": "Attendance marked for hearing date 2026-03-09"
  }
}
```

---

## Frontend Implementation Guide

### Step 1: Load Parties

When the officer opens the attendance form, first load the parties:

```typescript
// Load parties for the case
const loadParties = async (caseId: number) => {
  try {
    const response = await fetch(`/api/cases/${caseId}/parties`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    const result = await response.json();
    if (result.success) {
      return result.data.parties; // Array of PartyInfoDTO
    }
  } catch (error) {
    console.error('Error loading parties:', error);
  }
  return [];
};
```

### Step 2: Load Attendance Form Schema

Load the form schema to understand required fields:

```typescript
const loadAttendanceSchema = async (caseId: number) => {
  try {
    const response = await fetch(`/api/cases/${caseId}/module-forms/ATTENDANCE`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    const result = await response.json();
    if (result.success) {
      return result.data; // ModuleFormSchemaDTO
    }
  } catch (error) {
    console.error('Error loading attendance schema:', error);
  }
  return null;
};
```

### Step 3: Check for Existing Attendance

Load existing attendance data if available:

```typescript
const loadExistingAttendance = async (caseId: number) => {
  try {
    const response = await fetch(`/api/cases/${caseId}/module-forms/ATTENDANCE/latest`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    const result = await response.json();
    if (result.success && result.data) {
      // Parse formData JSON
      return JSON.parse(result.data.formData);
    }
  } catch (error) {
    console.error('Error loading existing attendance:', error);
  }
  return null;
};
```

### Step 4: Build Attendance Form UI

Create a form with checkboxes for each party:

```typescript
interface PartyAttendance {
  partyId: string;
  partyName: string;
  partyType: string;
  isPresent: boolean;
  isProxy: boolean;
  proxyName?: string;
  remarks?: string;
}

interface AttendanceFormData {
  attendanceDate: string;
  parties: PartyAttendance[];
  remarks?: string;
}

const AttendanceForm = ({ caseId }: { caseId: number }) => {
  const [parties, setParties] = useState<PartyInfoDTO[]>([]);
  const [attendance, setAttendance] = useState<AttendanceFormData>({
    attendanceDate: new Date().toISOString().split('T')[0],
    parties: []
  });

  useEffect(() => {
    // Load parties
    loadParties(caseId).then(partyList => {
      setParties(partyList);
      // Initialize attendance data
      setAttendance(prev => ({
        ...prev,
        parties: partyList.map(party => ({
          partyId: party.partyId,
          partyName: party.partyName,
          partyType: party.partyType,
          isPresent: false,
          isProxy: false,
          proxyName: '',
          remarks: ''
        }))
      }));
    });

    // Load existing attendance if available
    loadExistingAttendance(caseId).then(existing => {
      if (existing) {
        setAttendance(existing);
      }
    });
  }, [caseId]);

  const handlePartyChange = (partyId: string, field: string, value: any) => {
    setAttendance(prev => ({
      ...prev,
      parties: prev.parties.map(party =>
        party.partyId === partyId ? { ...party, [field]: value } : party
      )
    }));
  };

  const handleSubmit = async () => {
    const formData = {
      formData: JSON.stringify(attendance),
      remarks: attendance.remarks || 'Attendance marked'
    };

    try {
      const response = await fetch(`/api/cases/${caseId}/module-forms/ATTENDANCE/submit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      });

      const result = await response.json();
      if (result.success) {
        alert('Attendance marked successfully!');
      } else {
        alert('Error: ' + result.message);
      }
    } catch (error) {
      console.error('Error submitting attendance:', error);
      alert('Error submitting attendance');
    }
  };

  return (
    <div className="attendance-form">
      <h3>Mark Attendance</h3>
      
      <div className="form-group">
        <label>Attendance Date</label>
        <input
          type="date"
          value={attendance.attendanceDate}
          onChange={(e) => setAttendance(prev => ({ ...prev, attendanceDate: e.target.value }))}
        />
      </div>

      <div className="parties-section">
        <h4>Parties</h4>
        {attendance.parties.map(party => (
          <div key={party.partyId} className="party-row">
            <div className="party-info">
              <label>
                <input
                  type="checkbox"
                  checked={party.isPresent}
                  onChange={(e) => handlePartyChange(party.partyId, 'isPresent', e.target.checked)}
                />
                <strong>{party.partyName}</strong> ({party.partyType})
              </label>
            </div>

            {party.isPresent && (
              <div className="party-details">
                <label>
                  <input
                    type="checkbox"
                    checked={party.isProxy}
                    onChange={(e) => handlePartyChange(party.partyId, 'isProxy', e.target.checked)}
                  />
                  Represented by Proxy
                </label>

                {party.isProxy && (
                  <div className="proxy-details">
                    <input
                      type="text"
                      placeholder="Proxy Name"
                      value={party.proxyName || ''}
                      onChange={(e) => handlePartyChange(party.partyId, 'proxyName', e.target.value)}
                    />
                  </div>
                )}

                <textarea
                  placeholder="Remarks (optional)"
                  value={party.remarks || ''}
                  onChange={(e) => handlePartyChange(party.partyId, 'remarks', e.target.value)}
                />
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="form-group">
        <label>General Remarks</label>
        <textarea
          value={attendance.remarks || ''}
          onChange={(e) => setAttendance(prev => ({ ...prev, remarks: e.target.value }))}
          placeholder="Additional remarks..."
        />
      </div>

      <button onClick={handleSubmit} className="submit-btn">
        Submit Attendance
      </button>
    </div>
  );
};
```

### Step 5: HTML Template

```html
<div class="attendance-form">
  <h3>Mark Attendance</h3>
  
  <div class="form-group">
    <label>Attendance Date</label>
    <input type="date" id="attendanceDate" />
  </div>

  <div class="parties-section">
    <h4>Parties</h4>
    <div id="partiesList">
      <!-- Parties will be dynamically added here -->
    </div>
  </div>

  <div class="form-group">
    <label>General Remarks</label>
    <textarea id="remarks" placeholder="Additional remarks..."></textarea>
  </div>

  <button onclick="submitAttendance()" class="submit-btn">
    Submit Attendance
  </button>
</div>
```

### Step 6: CSS Styling

```css
.attendance-form {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.parties-section {
  margin: 20px 0;
}

.party-row {
  margin: 15px 0;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.party-info {
  margin-bottom: 10px;
}

.party-info label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: bold;
}

.party-details {
  margin-top: 10px;
  padding-left: 30px;
}

.proxy-details {
  margin: 10px 0;
}

.proxy-details input {
  width: 100%;
  padding: 8px;
  margin-top: 5px;
}

.party-details textarea {
  width: 100%;
  padding: 8px;
  margin-top: 10px;
  min-height: 60px;
}

.submit-btn {
  background-color: #007bff;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.submit-btn:hover {
  background-color: #0056b3;
}
```

---

## Database Schema

### Table: `case_module_form_submissions`

Stores attendance form submissions:

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `case_id` | BIGINT | Foreign key to `cases` |
| `case_nature_id` | BIGINT | Foreign key to `case_nature` |
| `module_type` | VARCHAR(30) | `ATTENDANCE` |
| `form_data` | TEXT | JSON string with attendance data |
| `submitted_by_officer_id` | BIGINT | Officer who submitted |
| `submitted_at` | TIMESTAMP | Submission timestamp |
| `remarks` | TEXT | General remarks |

### Table: `case_workflow_instance`

Workflow flag is set in `workflow_data` JSON:

```json
{
  "ATTENDANCE_SUBMITTED": true,
  "HEARING_SUBMITTED": true,
  ...
}
```

---

## Workflow Integration Example

### Checklist Condition

To require attendance before proceeding to next state:

```json
{
  "workflowDataFieldsRequired": ["ATTENDANCE_SUBMITTED"]
}
```

### Transition Flow

1. **State**: `PROCEEDINGS_IN_PROGRESS`
2. **Action**: Officer marks attendance
3. **Form Submission**: `ATTENDANCE_SUBMITTED = true`
4. **Next Transition**: `PROCEED_TO_FINAL_HEARING` (now enabled because checklist passes)

---

## Admin Configuration

### Step 1: Create Attendance Form Fields

Use admin API to create attendance form fields:

```bash
POST /api/admin/module-forms/fields
{
  "caseNatureId": 1,
  "caseTypeId": null,
  "moduleType": "ATTENDANCE",
  "fieldName": "attendanceDate",
  "fieldLabel": "Attendance Date",
  "fieldType": "DATE",
  "isRequired": true,
  "displayOrder": 1
}
```

### Step 2: Configure Workflow Condition

Add attendance requirement to workflow transition:

```bash
PUT /api/admin/workflow/permissions/{permissionId}
{
  "conditions": {
    "workflowDataFieldsRequired": ["ATTENDANCE_SUBMITTED"]
  }
}
```

---

## Testing Checklist

- [ ] Load parties for a case
- [ ] Display parties with checkboxes
- [ ] Mark party as present
- [ ] Mark party as proxy
- [ ] Enter proxy name
- [ ] Add remarks for party
- [ ] Submit attendance form
- [ ] Verify `ATTENDANCE_SUBMITTED` flag is set
- [ ] Load existing attendance data
- [ ] Edit and resubmit attendance
- [ ] Verify workflow transition is enabled after attendance submission

---

## Common Issues & Solutions

### Issue 1: No parties found

**Solution**: Ensure `caseData` contains `petitionerName` or `respondentName` fields. System falls back to `applicant` name if not found.

### Issue 2: Attendance form not showing

**Solution**: Ensure `ATTENDANCE` module form fields are configured for the case nature.

### Issue 3: Workflow transition not enabled

**Solution**: Check that `ATTENDANCE_SUBMITTED` flag is set in `workflow_data` and transition permission has correct condition.

---

**Document Version:** 1.0  
**Last Updated:** 2026-03-09  
**For:** Chandigarh RCCMS Implementation
