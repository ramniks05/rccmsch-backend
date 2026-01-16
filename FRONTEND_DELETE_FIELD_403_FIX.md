# Fix 403 Forbidden Error - Delete Field Endpoint

## Issue

Getting `403 Forbidden` when trying to DELETE a form field:
```
DELETE /api/admin/form-schemas/fields/{fieldId}
Status: 403 Forbidden
```

## Root Cause

The DELETE endpoint requires:
1. **Valid JWT token** in the Authorization header
2. **ADMIN authority** (role)

## Solutions

### Solution 1: Check Authentication Token

Make sure you're sending the JWT token in the request header:

```typescript
// Angular Service
deleteField(fieldId: number): Observable<void> {
  const token = localStorage.getItem('accessToken'); // or wherever you store the token
  
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  });

  return this.http.delete<ApiResponse<void>>(
    `${this.baseUrl}/api/admin/form-schemas/fields/${fieldId}`,
    { headers }
  ).pipe(
    map(() => undefined),
    catchError(this.handleError)
  );
}
```

### Solution 2: Verify User Has ADMIN Role

Check that the logged-in user has ADMIN authority:

```typescript
// Check user role before deleting
if (this.currentUser?.authorities?.includes('ADMIN')) {
  this.formSchemaService.deleteField(fieldId).subscribe({
    next: () => {
      console.log('Field deleted successfully');
      this.loadSchema();
    },
    error: (error) => {
      if (error.status === 403) {
        alert('You do not have permission to delete fields. Admin access required.');
      } else {
        alert('Delete failed: ' + error.message);
      }
    }
  });
} else {
  alert('Admin access required to delete fields');
}
```

### Solution 3: Check Token Expiration

The token might be expired. Check and refresh if needed:

```typescript
deleteField(fieldId: number) {
  // Check if token is expired
  const token = localStorage.getItem('accessToken');
  if (!token || this.isTokenExpired(token)) {
    // Refresh token or redirect to login
    this.authService.refreshToken().subscribe({
      next: () => {
        // Retry delete after token refresh
        this.deleteField(fieldId);
      },
      error: () => {
        // Redirect to login
        this.router.navigate(['/login']);
      }
    });
    return;
  }

  // Proceed with delete
  this.formSchemaService.deleteField(fieldId).subscribe({
    next: () => this.loadSchema(),
    error: (error) => {
      if (error.status === 401) {
        // Unauthorized - token expired
        this.authService.logout();
      } else if (error.status === 403) {
        // Forbidden - no permission
        alert('You do not have permission to delete fields');
      }
    }
  });
}
```

### Solution 4: Complete Example with Error Handling

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class FormSchemaService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      throw new Error('No authentication token found');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  deleteField(fieldId: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/api/admin/form-schemas/fields/${fieldId}`,
      { headers: this.getAuthHeaders() }
    ).pipe(
      map(() => undefined),
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An error occurred';
    
    if (error.status === 401) {
      errorMessage = 'Authentication failed. Please login again.';
      // Optionally redirect to login
    } else if (error.status === 403) {
      errorMessage = 'You do not have permission to perform this action. Admin access required.';
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    return throwError(() => new Error(errorMessage));
  }
}
```

## Common Issues

### Issue 1: Token Not Sent
**Symptom:** 403 Forbidden  
**Fix:** Ensure `Authorization: Bearer {token}` header is included

### Issue 2: Token Expired
**Symptom:** 401 Unauthorized or 403 Forbidden  
**Fix:** Refresh token or re-login

### Issue 3: User Not Admin
**Symptom:** 403 Forbidden  
**Fix:** Login with an admin account

### Issue 4: CORS Preflight
**Symptom:** 403 on OPTIONS request  
**Fix:** Backend already handles this, but check browser console for CORS errors

## Testing

### Test 1: Check Token in Request
Open browser DevTools → Network tab → Check request headers:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Test 2: Check User Role
After login, check user object:
```typescript
console.log('User authorities:', this.currentUser?.authorities);
// Should include 'ADMIN'
```

### Test 3: Test with Postman/curl
```bash
curl -X DELETE \
  http://localhost:8080/api/admin/form-schemas/fields/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json"
```

## Quick Checklist

- [ ] JWT token is stored after login
- [ ] Token is sent in `Authorization: Bearer {token}` header
- [ ] Token is not expired
- [ ] User has ADMIN role/authority
- [ ] CORS is properly configured (backend already done)
- [ ] Request method is DELETE (not GET/POST)

## Backend Endpoint Details

- **Endpoint:** `DELETE /api/admin/form-schemas/fields/{fieldId}`
- **Authorization:** Required (JWT token)
- **Role Required:** ADMIN
- **Response:** 200 OK with success message

## Still Getting 403?

1. **Check backend logs** for authentication errors
2. **Verify token** is valid by calling a simple authenticated endpoint first
3. **Check user role** in database - ensure user has ADMIN role
4. **Test with Swagger** - use Swagger UI to test the endpoint with token

---

**Note:** The endpoint is correctly configured to require ADMIN authority. The 403 error indicates an authentication/authorization issue on the frontend side.

