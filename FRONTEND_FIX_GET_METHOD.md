# Frontend Fix: Use GET Method for Form Schema

## Issue

**Error**: `403 Forbidden` when calling `/api/admin/form-schemas/case-types/1`

**Root Cause**: Frontend is using **PUT** method instead of **GET** method.

---

## Solution

### ✅ Correct HTTP Method

The endpoint `/api/admin/form-schemas/case-types/{caseTypeId}` is a **GET** endpoint, not PUT.

### Frontend Code Fix

#### ❌ Wrong (Current)
```typescript
// DON'T DO THIS - Using PUT
this.http.put(`http://localhost:8080/api/admin/form-schemas/case-types/${caseTypeId}`, {})
```

#### ✅ Correct
```typescript
// DO THIS - Using GET
this.http.get(`http://localhost:8080/api/admin/form-schemas/case-types/${caseTypeId}`)
```

---

## Complete Frontend Service Example

### Angular

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class FormSchemaService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  /**
   * Get form schema for a case type
   * ✅ Uses GET method
   */
  getFormSchema(caseTypeId: number): Observable<FormSchema> {
    return this.http.get<ApiResponse<FormSchema>>(
      `${this.baseUrl}/api/admin/form-schemas/case-types/${caseTypeId}`
    ).pipe(
      map(response => response.data)
    );
  }
}
```

### React/Axios

```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

export const formSchemaService = {
  /**
   * Get form schema for a case type
   * ✅ Uses GET method
   */
  async getFormSchema(caseTypeId: number): Promise<FormSchema> {
    const response = await axios.get(
      `${API_BASE_URL}/api/admin/form-schemas/case-types/${caseTypeId}`
    );
    return response.data.data;
  }
};
```

### Fetch API

```typescript
/**
 * Get form schema for a case type
 * ✅ Uses GET method
 */
async function getFormSchema(caseTypeId: number): Promise<FormSchema> {
  const response = await fetch(
    `http://localhost:8080/api/admin/form-schemas/case-types/${caseTypeId}`,
    {
      method: 'GET',  // ✅ Explicitly use GET
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  
  const data = await response.json();
  return data.data;
}
```

---

## API Endpoint Reference

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| **GET** | `/api/admin/form-schemas/case-types/{caseTypeId}` | Get form schema | ❌ No |
| GET | `/api/admin/form-schemas/case-types/{caseTypeId}/fields` | Get all fields (admin) | ✅ Yes |
| POST | `/api/admin/form-schemas/fields` | Create field | ✅ Yes |
| PUT | `/api/admin/form-schemas/fields/{fieldId}` | Update field | ✅ Yes |
| DELETE | `/api/admin/form-schemas/fields/{fieldId}` | Delete field | ✅ Yes |
| POST | `/api/admin/form-schemas/validate` | Validate form data | ❌ No |

---

## Testing

### Test with Browser Console

```javascript
// ✅ Correct - GET request
fetch('http://localhost:8080/api/admin/form-schemas/case-types/1', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json'
  }
})
  .then(res => res.json())
  .then(data => console.log('Success:', data))
  .catch(err => console.error('Error:', err));
```

### Test with cURL

```bash
# ✅ Correct - GET request
curl -X GET http://localhost:8080/api/admin/form-schemas/case-types/1

# ❌ Wrong - PUT request (will fail)
curl -X PUT http://localhost:8080/api/admin/form-schemas/case-types/1
```

---

## Common Mistakes

### ❌ Mistake 1: Using PUT instead of GET
```typescript
// Wrong
this.http.put(url, {})
```

### ❌ Mistake 2: Sending body with GET
```typescript
// Wrong - GET requests don't have body
this.http.get(url, { body: data })
```

### ❌ Mistake 3: Using wrong endpoint
```typescript
// Wrong endpoint
this.http.get('/api/admin/form-schemas/fields/1')  // This is for getting a single field, not schema
```

### ✅ Correct
```typescript
// Correct
this.http.get(`/api/admin/form-schemas/case-types/${caseTypeId}`)
```

---

## Quick Checklist

- [ ] Using **GET** method (not PUT, POST, DELETE)
- [ ] Endpoint: `/api/admin/form-schemas/case-types/{caseTypeId}`
- [ ] No request body (GET requests don't have body)
- [ ] No Authorization header needed (public endpoint)
- [ ] Case type ID is a number (not string)

---

## Expected Response

```json
{
  "success": true,
  "message": "Form schema retrieved successfully",
  "data": {
    "caseTypeId": 1,
    "caseTypeName": "Mutation (after Gift/Sale Deeds)",
    "caseTypeCode": "MUTATION_GIFT_SALE",
    "totalFields": 8,
    "fields": [
      {
        "id": 1,
        "fieldName": "registeredDeedNumber",
        "fieldLabel": "Registered Deed Number",
        "fieldType": "TEXT",
        "isRequired": true,
        "displayOrder": 1
      }
      // ... more fields
    ]
  }
}
```

---

## Summary

**The fix is simple**: Change your frontend code from **PUT** to **GET** method when calling the form schema endpoint.

**Correct Endpoint**:
```
GET http://localhost:8080/api/admin/form-schemas/case-types/{caseTypeId}
```

**No authentication required** for this endpoint.

