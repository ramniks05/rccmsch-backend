# Workflow API Testing Guide

## Quick Fix Applied ✅

I've enabled method security in `SecurityConfig.java` by adding `@EnableMethodSecurity(prePostEnabled = true)`. This allows the `@PreAuthorize("hasAuthority('ADMIN')")` annotation to work properly.

## Steps to Test Workflow APIs

### Step 1: Login as Admin

**Endpoint:** `POST http://localhost:8080/api/admin/auth/login`

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin@123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Admin login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 0,
    "expiresIn": 3600
  }
}
```

**Copy the `token` value from the response.**

### Step 2: Use Token in Workflow API Requests

**Add Authorization Header:**
```
Authorization: Bearer <your_token_here>
```

### Step 3: Test Workflow Endpoints

**Example: Get All Workflows**
```bash
GET http://localhost:8080/api/admin/workflow/definitions
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Using cURL

### 1. Login and Save Token
```bash
# Login
TOKEN=$(curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin@123"}' \
  | jq -r '.data.token')

echo "Token: $TOKEN"
```

### 2. Test Workflow Endpoints
```bash
# Get all workflows
curl -X GET http://localhost:8080/api/admin/workflow/definitions \
  -H "Authorization: Bearer $TOKEN"

# Get workflow by code
curl -X GET http://localhost:8080/api/admin/workflow/definitions/MUTATION_GIFT_SALE \
  -H "Authorization: Bearer $TOKEN"

# Get workflow states
curl -X GET http://localhost:8080/api/admin/workflow/1/states \
  -H "Authorization: Bearer $TOKEN"
```

## Using Postman

1. **Create a new request** for admin login:
   - Method: `POST`
   - URL: `http://localhost:8080/api/admin/auth/login`
   - Body (raw JSON):
     ```json
     {
       "username": "admin",
       "password": "admin@123"
     }
     ```
   - Send request and copy the `token` from response

2. **Set Environment Variable** (optional):
   - Create environment variable `adminToken`
   - Set value to the token from login response

3. **Create workflow requests**:
   - Method: `GET`
   - URL: `http://localhost:8080/api/admin/workflow/definitions`
   - Headers:
     - `Authorization`: `Bearer {{adminToken}}` (if using environment variable)
     - OR `Authorization`: `Bearer <your_token_here>`

## Using Swagger UI

1. **Open Swagger UI**: `http://localhost:8080/swagger-ui.html`

2. **Login as Admin**:
   - Find `/api/admin/auth/login` endpoint
   - Click "Try it out"
   - Enter:
     ```json
     {
       "username": "admin",
       "password": "admin@123"
     }
     ```
   - Click "Execute"
   - Copy the `token` from response

3. **Authorize**:
   - Click the "Authorize" button (top right)
   - Enter: `Bearer <your_token>`
   - Click "Authorize"
   - Click "Close"

4. **Test Workflow APIs**:
   - All workflow endpoints are now authorized
   - Click "Try it out" on any workflow endpoint
   - Execute the request

## Troubleshooting

### Error: 401 Unauthorized
**Cause:** No token or invalid token
**Solution:** 
- Login again to get a new token
- Check that token is included in Authorization header
- Format: `Bearer <token>` (with space after Bearer)

### Error: 403 Forbidden
**Cause:** Token doesn't have ADMIN authority
**Solution:**
- Make sure you logged in using `/api/admin/auth/login` (not officer login)
- Token must have `authType: "ADMIN"` in claims
- Try logging in again

### Error: Token Expired
**Cause:** Token expired (default: 1 hour)
**Solution:**
- Login again to get a new token
- Or use refresh token endpoint (if implemented)

---

## Return for Correction: Citizen Resubmission API

After an officer returns a case for correction (state: `RETURNED_FOR_CORRECTION`), the citizen can resubmit updated data using:

```
PUT /api/cases/{caseId}/resubmit
Header: X-User-Id: <citizenId>
Authorization: Bearer <citizenToken>
```

```json
{
  "caseData": "{ ...corrected JSON... }",
  "remarks": "Updated documents and corrected details"
}
```

**Note:** This API only works if the case is currently in `RETURNED_FOR_CORRECTION` state.

---

## Transition Conditions (Optional)

You can add JSON conditions inside `WorkflowPermission.conditions`.
If conditions fail, the transition is not allowed.

### Supported keys
- `caseTypeCodesAllowed`
- `casePriorityIn`
- `caseDataFieldsRequired`
- `caseDataFieldEquals`
- `workflowDataFieldsRequired`

### Example Permission (with conditions)
```json
{
  "roleCode": "CIRCLE_OFFICER",
  "unitLevel": "CIRCLE",
  "canInitiate": true,
  "canApprove": true,
  "hierarchyRule": "SAME_UNIT",
  "conditions": "{\"casePriorityIn\":[\"HIGH\"],\"caseDataFieldsRequired\":[\"registeredDeedNumber\"]}",
  "isActive": true
}
```

## Verify Token Has ADMIN Authority

You can decode your JWT token at https://jwt.io to verify it contains:
```json
{
  "authType": "ADMIN",
  "role": "SUPER_ADMIN",
  ...
}
```

## Default Admin Credentials

- **Username:** `admin`
- **Password:** `admin@123`

⚠️ **Change these in production!**

## Security Configuration

The workflow endpoints are protected by:
1. **Authentication**: JWT token required
2. **Authorization**: `@PreAuthorize("hasAuthority('ADMIN')")` - requires ADMIN authority

The JWT filter automatically sets ADMIN authority when:
- Token has `authType: "ADMIN"`
- Token was generated by admin login endpoint

---

**Status:** ✅ Fixed - Method security enabled, workflow APIs now accessible with admin token
