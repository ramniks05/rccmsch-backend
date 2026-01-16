# CORS and Authentication Fix - Frontend Integration Guide

## Issue Fixed

**Problem**: Frontend getting `403 Forbidden` when calling `/api/admin/case-types` from browser, but works in Swagger.

**Root Causes**:
1. ✅ **Security Configuration**: GET endpoints required authentication
2. ✅ **CORS Configuration**: Limited frontend origins
3. ⚠️ **Frontend**: May not be sending JWT token correctly

---

## Changes Made

### 1. Security Configuration Updated

**File**: `src/main/java/in/gov/manipur/rccms/security/SecurityConfig.java`

**Changes**:
- Made **GET** endpoints public (read-only) for:
  - `/api/case-types/**` - Get case types
  - `/api/admin/case-types/**` - Get case types (admin endpoint)
  - `/api/admin/form-schemas/case-types/**` - Get form schema
- Made **POST** `/api/admin/form-schemas/validate` public (form validation)

**Result**: Frontend can now fetch case types and form schemas **without authentication**.

### 2. CORS Configuration Updated

**File**: `src/main/java/in/gov/manipur/rccms/config/CorsConfig.java`

**Changes**: Added more frontend origins:
- `http://localhost:4200` (Angular default)
- `http://localhost:4201` (Angular alternative)
- `http://localhost:3000` (React default)
- `http://localhost:5173` (Vite default)
- `http://127.0.0.1:4200` (Alternative format)
- `http://127.0.0.1:3000` (Alternative format)

**Note**: If your frontend runs on a different port, add it to the `allowedOrigins` list.

---

## Frontend Implementation

### Option 1: Use Public Endpoints (Recommended for Read Operations)

**No authentication required** for GET requests:

```typescript
// ✅ Works without token
GET http://localhost:8080/api/case-types
GET http://localhost:8080/api/case-types/active
GET http://localhost:8080/api/admin/case-types
GET http://localhost:8080/api/admin/form-schemas/case-types/1
```

**Example (Angular)**:
```typescript
// No Authorization header needed
getCaseTypes(): Observable<CaseType[]> {
  return this.http.get<ApiResponse<CaseType[]>>(
    'http://localhost:8080/api/case-types/active'
  ).pipe(
    map(response => response.data)
  );
}
```

**Example (React/Axios)**:
```typescript
// No Authorization header needed
const getCaseTypes = async () => {
  const response = await axios.get(
    'http://localhost:8080/api/case-types/active'
  );
  return response.data.data;
};
```

### Option 2: Use Authenticated Endpoints (For Write Operations)

**Authentication required** for POST, PUT, DELETE:

```typescript
// ❌ Requires JWT token
POST http://localhost:8080/api/case-types
PUT http://localhost:8080/api/case-types/1
DELETE http://localhost:8080/api/case-types/1
```

**Example (Angular with Interceptor)**:
```typescript
// HTTP Interceptor automatically adds token
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('accessToken');
    
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

**Example (React/Axios with Interceptor)**:
```typescript
// Axios interceptor
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

---

## Testing the Fix

### 1. Test Public Endpoint (No Auth)

```bash
# Should work without token
curl http://localhost:8080/api/case-types/active
```

**Expected**: `200 OK` with case types data

### 2. Test from Browser Console

```javascript
// Open browser console on your frontend
fetch('http://localhost:8080/api/case-types/active')
  .then(res => res.json())
  .then(data => console.log(data))
  .catch(err => console.error(err));
```

**Expected**: Should return data without CORS error

### 3. Test with Postman/Thunder Client

**Request**:
```
GET http://localhost:8080/api/case-types/active
Headers: (No Authorization header needed)
```

**Expected**: `200 OK`

---

## Common Issues & Solutions

### Issue 1: Still Getting 403 Forbidden

**Possible Causes**:
1. **Frontend origin not in CORS whitelist**
   - **Solution**: Add your frontend URL to `CorsConfig.java`
   - Example: If frontend runs on `http://localhost:8081`, add it to `allowedOrigins`

2. **Using wrong HTTP method**
   - **Solution**: Only GET requests are public. POST/PUT/DELETE require authentication.

3. **Browser cache**
   - **Solution**: Clear browser cache or use incognito mode

### Issue 2: CORS Error in Browser

**Error**: `Access to fetch at 'http://localhost:8080/...' from origin 'http://localhost:4200' has been blocked by CORS policy`

**Solution**:
1. Check if your frontend origin is in `CorsConfig.java`
2. Verify backend is running
3. Check browser console for exact CORS error message

### Issue 3: 401 Unauthorized (For Write Operations)

**Error**: `401 Unauthorized` when creating/updating/deleting

**Solution**:
1. Ensure JWT token is sent in `Authorization` header
2. Format: `Authorization: Bearer <token>`
3. Check if token is expired (tokens expire after 1 hour)
4. Refresh token if needed

---

## Frontend Code Examples

### Complete Angular Service Example

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class CaseTypeService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // ✅ Public endpoint - no auth needed
  getActiveCaseTypes(): Observable<CaseType[]> {
    return this.http.get<ApiResponse<CaseType[]>>(
      `${this.baseUrl}/api/case-types/active`
    ).pipe(
      map(response => response.data)
    );
  }

  // ✅ Public endpoint - no auth needed
  getFormSchema(caseTypeId: number): Observable<FormSchema> {
    return this.http.get<ApiResponse<FormSchema>>(
      `${this.baseUrl}/api/admin/form-schemas/case-types/${caseTypeId}`
    ).pipe(
      map(response => response.data)
    );
  }

  // ❌ Requires authentication
  createCaseType(caseType: CaseType): Observable<CaseType> {
    return this.http.post<ApiResponse<CaseType>>(
      `${this.baseUrl}/api/case-types`,
      caseType
      // Token will be added by HTTP interceptor
    ).pipe(
      map(response => response.data)
    );
  }
}
```

### Complete React Service Example

```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

// Setup axios interceptor for auth
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const caseTypeService = {
  // ✅ Public endpoint - no auth needed
  async getActiveCaseTypes(): Promise<CaseType[]> {
    const response = await axios.get(
      `${API_BASE_URL}/api/case-types/active`
    );
    return response.data.data;
  },

  // ✅ Public endpoint - no auth needed
  async getFormSchema(caseTypeId: number): Promise<FormSchema> {
    const response = await axios.get(
      `${API_BASE_URL}/api/admin/form-schemas/case-types/${caseTypeId}`
    );
    return response.data.data;
  },

  // ❌ Requires authentication (token added by interceptor)
  async createCaseType(caseType: CaseType): Promise<CaseType> {
    const response = await axios.post(
      `${API_BASE_URL}/api/case-types`,
      caseType
    );
    return response.data.data;
  }
};
```

---

## Endpoint Summary

### Public Endpoints (No Auth Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/case-types` | Get all case types |
| GET | `/api/case-types/active` | Get active case types |
| GET | `/api/case-types/{id}` | Get case type by ID |
| GET | `/api/admin/case-types` | Get all case types (admin) |
| GET | `/api/admin/case-types/active` | Get active case types (admin) |
| GET | `/api/admin/form-schemas/case-types/{id}` | Get form schema |
| POST | `/api/admin/form-schemas/validate` | Validate form data |

### Protected Endpoints (Auth Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/case-types` | Create case type |
| PUT | `/api/case-types/{id}` | Update case type |
| DELETE | `/api/case-types/{id}` | Delete case type |
| POST | `/api/admin/form-schemas/fields` | Create form field |
| PUT | `/api/admin/form-schemas/fields/{id}` | Update form field |
| DELETE | `/api/admin/form-schemas/fields/{id}` | Delete form field |

---

## Next Steps

1. ✅ **Restart Backend**: Restart Spring Boot application to apply changes
2. ✅ **Test Public Endpoints**: Verify GET requests work without token
3. ✅ **Update Frontend**: Use public endpoints for read operations
4. ✅ **Add HTTP Interceptor**: For authenticated endpoints (write operations)
5. ✅ **Test End-to-End**: Create a case and verify form submission

---

## Verification Checklist

- [ ] Backend restarted successfully
- [ ] GET `/api/case-types/active` works without token
- [ ] GET `/api/admin/form-schemas/case-types/1` works without token
- [ ] No CORS errors in browser console
- [ ] Frontend can fetch case types
- [ ] Frontend can fetch form schema
- [ ] Write operations require authentication (expected)

---

## Support

If you still encounter issues:

1. **Check Backend Logs**: Look for CORS or security errors
2. **Check Browser Console**: Look for CORS or network errors
3. **Verify Frontend Origin**: Ensure it's in CORS whitelist
4. **Test with Postman**: Isolate if it's a frontend or backend issue
5. **Check Network Tab**: Verify request headers and response

---

**Last Updated**: After security configuration fix

