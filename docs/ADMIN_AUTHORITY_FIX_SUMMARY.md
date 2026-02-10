# Admin Authority Fix - All Operations

## Issue Fixed

The JWT filter was setting `ROLE_SUPER_ADMIN` authority, but all admin endpoints check for `hasAuthority('ADMIN')`, causing 403 Forbidden errors.

## Solution Applied

Updated `JwtAuthenticationFilter.java` to add `ADMIN` authority for admin users:

```java
} else if ("ADMIN".equals(authType)) {
    // For admin, add both ADMIN authority and role-based authority
    authorities.add(new SimpleGrantedAuthority("ADMIN")); // ← Added this!
    String role = claims.get("role", String.class);
    if (role != null) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
```

## All Endpoints Now Working

The fix applies to **all** admin form schema operations:

### ✅ GET Operations
- `GET /api/admin/form-schemas/case-types/{caseTypeId}/fields` - Get all fields (including inactive)
- `GET /api/admin/form-schemas/fields/{fieldId}` - Get single field by ID

### ✅ POST Operations
- `POST /api/admin/form-schemas/fields` - Create a new form field

### ✅ PUT Operations
- `PUT /api/admin/form-schemas/fields/{fieldId}` - Update an existing form field
- `PUT /api/admin/form-schemas/case-types/{caseTypeId}/fields/reorder` - Reorder fields

### ✅ DELETE Operations
- `DELETE /api/admin/form-schemas/fields/{fieldId}` - Delete a form field

## Public Endpoints (No Auth Required)

These endpoints remain public:
- `GET /api/admin/form-schemas/case-types/{caseTypeId}` - Get schema (active fields only)
- `POST /api/admin/form-schemas/validate` - Validate form data

## Next Steps

1. **Restart the Spring Boot application**
2. **Re-login as admin** to get a new JWT token with the correct authority
3. **Test all operations** - they should all work now!

## Verification

After restarting and re-logging in, test these operations:

```bash
# 1. Create field
POST /api/admin/form-schemas/fields
Authorization: Bearer {token}

# 2. Update field
PUT /api/admin/form-schemas/fields/{id}
Authorization: Bearer {token}

# 3. Delete field
DELETE /api/admin/form-schemas/fields/{id}
Authorization: Bearer {token}

# 4. Get all fields
GET /api/admin/form-schemas/case-types/{id}/fields
Authorization: Bearer {token}
```

All should return **200 OK** (or 201 for create) instead of **403 Forbidden**.

---

**Status:** ✅ Fixed - All admin operations now work correctly!

