# Officer Login API Documentation

**Base URL:** `http://localhost:8080/api/admin/auth`

This document provides comprehensive documentation for Officer/DA Login APIs in the RCCMS system.

---

## Table of Contents

1. [Overview](#overview)
2. [Important Concepts](#important-concepts)
3. [API Response Format](#api-response-format)
4. [Error Handling](#error-handling)
5. [Officer Login Flow](#officer-login-flow)
6. [API Endpoints](#api-endpoints)
7. [Frontend Implementation Guide](#frontend-implementation-guide)
8. [Complete Flow Examples](#complete-flow-examples)

---

## Overview

The RCCMS Officer Authentication system supports:
- **Post-Based Login** using UserID (ROLE@LGD format) and password
- **Mandatory Password Reset** on first login
- **Mobile Verification** (optional, for first login)
- **JWT-based Authentication** with posting information

### Key Features:
- Officers are created by Admin (no self-registration)
- Login uses UserID format: `ROLE_CODE@UNIT_LGD_CODE`
- Default password: `Rccms@<last4MobileDigits>`
- JWT tokens include posting details (role, unit, level)
- Token expiration: Access token (1 hour), Refresh token (7 days)

---

## Important Concepts

### UserID Format
- **Format:** `ROLE_CODE@UNIT_LGD_CODE`
- **Examples:**
  - `STATE_ADMIN@MNSTATE01`
  - `DISTRICT_OFFICER@IMW001`
  - `SUB_DIVISION_OFFICER@LAM001`
  - `CIRCLE_OFFICER@SEK001`
  - `DEALING_ASSISTANT@SEK001`

### Default Password
- **Format:** `Rccms@<last4MobileDigits>`
- **Example:** Mobile `9876543210` → Password `Rccms@3210`
- Generated automatically when officer is created by admin
- Logged to console (admin can share with officer)
- **Mandatory reset on first login**

### Login Requirements
1. Officer account must be **active** (`isActive = true`)
2. Posting must be **active** (`isCurrent = true`)
3. Password reset must be **completed** (`isPasswordResetRequired = false`)
4. UserID must match an active posting

### JWT Token Claims (Post-Based)
The JWT token includes:
- `userid`: Posting UserID (ROLE@LGD)
- `roleCode`: Role code (e.g., DISTRICT_OFFICER)
- `unitId`: Unit ID
- `unitLevel`: Unit level (STATE, DISTRICT, SUB_DIVISION, CIRCLE)
- `userId`: Officer (person) ID
- `authType`: "POST_BASED"

---

## API Response Format

All API responses follow a standard format:

### Success Response:
```json
{
  "success": true,
  "message": "Operation successful message",
  "data": {
    // Response data here
  }
}
```

### Error Response:
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Error message",
  "path": "/api/admin/auth/officer-login"
}
```

---

## Error Handling

### HTTP Status Codes:
- **200 OK**: Request successful
- **400 Bad Request**: Validation errors or invalid input
- **401 Unauthorized**: Invalid credentials, password reset required, or account inactive
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server error

### Common Error Messages:
- `"Password reset required. Please reset your password."`
- `"Invalid UserID or password"`
- `"Officer account is not active"`
- `"Invalid UserID or posting is not active"`
- `"Password and confirm password do not match"`
- `"Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"`

---

## Officer Login Flow

### Step-by-Step Process:

1. **Admin Creates Officer** → Temporary password generated (`Rccms@<last4MobileDigits>`)
2. **Admin Assigns Officer to Post** → UserID generated (`ROLE@LGD`)
3. **Officer Attempts Login** → Uses UserID + temporary password
4. **First Login Response** → 401 error: "Password reset required"
5. **Officer Resets Password** → Sets new password meeting complexity requirements
6. **Officer Logs In Again** → Success with JWT tokens

### Flow Diagram:
```
[Admin Creates Officer] → [Admin Assigns to Post] → [Officer Login Attempt] 
  → [Password Reset Required] → [Reset Password] → [Login Success] → [JWT Tokens]
```

---

## API Endpoints

### 1. Officer/DA Login

**Endpoint:** `POST /api/admin/auth/officer-login`

**Description:** Login for government officers and dealing assistants using UserID (ROLE@LGD format) and password.

**Request:**
```http
POST /api/admin/auth/officer-login
Content-Type: application/json
```

**Request Body:**
```json
{
  "userid": "DISTRICT_OFFICER@IMW001",
  "password": "Rccms@3210"
}
```

**Field Validations:**
- `userid`: Required, format: `ROLE_CODE@UNIT_LGD_CODE`
- `password`: Required, temporary password or reset password

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Officer login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "citizenType": null,
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

**Error Responses:**

**401 - Password Reset Required (First Login):**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Password reset required. Please reset your password.",
  "path": "/api/admin/auth/officer-login"
}
```

**401 - Invalid Credentials:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid UserID or password",
  "path": "/api/admin/auth/officer-login"
}
```

**401 - Account Inactive:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Officer account is not active",
  "path": "/api/admin/auth/officer-login"
}
```

**401 - Posting Not Active:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid UserID or posting is not active",
  "path": "/api/admin/auth/officer-login"
}
```

**Frontend Notes:**
- Store `token` and `refreshToken` securely (localStorage/sessionStorage)
- Include `token` in `Authorization` header for authenticated requests: `Authorization: Bearer <token>`
- Token expires in `expiresIn` seconds (default: 3600 = 1 hour)
- If response is 401 with "Password reset required", redirect to password reset page

---

### 2. Reset Password (First Login)

**Endpoint:** `POST /api/admin/auth/reset-password`

**Description:** Reset password for first login (mandatory after temporary password). Password must meet complexity requirements.

**Request:**
```http
POST /api/admin/auth/reset-password
Content-Type: application/json
```

**Request Body:**
```json
{
  "userid": "DISTRICT_OFFICER@IMW001",
  "newPassword": "NewSecure@123",
  "confirmPassword": "NewSecure@123"
}
```

**Field Validations:**
- `userid`: Required, format: `ROLE_CODE@UNIT_LGD_CODE`
- `newPassword`: Required, 8-100 characters, must contain:
  - At least one uppercase letter (A-Z)
  - At least one lowercase letter (a-z)
  - At least one number (0-9)
  - At least one special character (@$!%*?&)
- `confirmPassword`: Required, must match `newPassword`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": {
    "message": "Password reset successfully",
    "userid": "DISTRICT_OFFICER@IMW001"
  }
}
```

**Error Responses:**

**400 - Password Mismatch:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Password and confirm password do not match",
  "path": "/api/admin/auth/reset-password"
}
```

**400 - Validation Error:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "newPassword",
      "message": "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    }
  ],
  "path": "/api/admin/auth/reset-password"
}
```

**Frontend Notes:**
- After successful password reset, redirect to login page
- Pre-fill UserID in login form
- Show success message before redirect

---

### 3. Verify Mobile with OTP (Optional)

**Endpoint:** `POST /api/admin/auth/verify-mobile`

**Description:** Verify mobile number with OTP (for first login profile update). Currently simplified - full OTP implementation can be added later.

**Request:**
```http
POST /api/admin/auth/verify-mobile
Content-Type: application/json
```

**Request Body:**
```json
{
  "userid": "DISTRICT_OFFICER@IMW001",
  "otp": "123456"
}
```

**Field Validations:**
- `userid`: Required, format: `ROLE_CODE@UNIT_LGD_CODE`
- `otp`: Required, 6-digit OTP code

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Mobile number verified successfully",
  "data": {
    "message": "Mobile number verified successfully",
    "userid": "DISTRICT_OFFICER@IMW001"
  }
}
```

**Error Responses:**

**400 - Invalid Request:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "UserID and OTP are required",
  "path": "/api/admin/auth/verify-mobile"
}
```

**401 - Invalid UserID or OTP:**
```json
{
  "timestamp": "2026-01-09T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid UserID or posting is not active",
  "path": "/api/admin/auth/verify-mobile"
}
```

**Frontend Notes:**
- This is optional and can be done after password reset
- OTP can be sent via SMS (when SMS API is integrated)
- Currently simplified - full OTP flow can be implemented later

---

## Frontend Implementation Guide

### Angular/TypeScript Example

#### 1. API Service (`officer-auth.service.ts`)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  userId: number;
  citizenType: string | null;
  email: string | null;
  mobileNumber: string | null;
  expiresIn: number;
}

export interface PostBasedLoginRequest {
  userid: string; // Format: ROLE@LGD
  password: string;
}

export interface PasswordResetRequest {
  userid: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class OfficerAuthService {
  private baseUrl = 'http://localhost:8080/api/admin/auth';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Officer/DA Login
   */
  login(userid: string, password: string): Observable<ApiResponse<AuthResponse>> {
    const request: PostBasedLoginRequest = { userid, password };
    
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.baseUrl}/officer-login`,
      request
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.storeTokens(response.data);
        }
      }),
      catchError(error => {
        // Handle password reset required error
        if (error.status === 401 && 
            error.error?.message?.includes('Password reset required')) {
          // Store userid for password reset page
          sessionStorage.setItem('reset_userid', userid);
          // Redirect to password reset page
          this.router.navigate(['/officer/reset-password']);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Reset Password (First Login)
   */
  resetPassword(
    userid: string, 
    newPassword: string, 
    confirmPassword: string
  ): Observable<ApiResponse<any>> {
    const request: PasswordResetRequest = {
      userid,
      newPassword,
      confirmPassword
    };
    
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/reset-password`,
      request
    ).pipe(
      tap(response => {
        if (response.success) {
          // Clear reset userid from session
          sessionStorage.removeItem('reset_userid');
        }
      })
    );
  }

  /**
   * Verify Mobile with OTP
   */
  verifyMobile(userid: string, otp: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/verify-mobile`,
      { userid, otp }
    );
  }

  /**
   * Store tokens in localStorage
   */
  private storeTokens(authData: AuthResponse): void {
    localStorage.setItem('officer_access_token', authData.token);
    localStorage.setItem('officer_refresh_token', authData.refreshToken);
    localStorage.setItem('officer_user_id', authData.userId.toString());
    localStorage.setItem('officer_email', authData.email || '');
    localStorage.setItem('officer_mobile', authData.mobileNumber || '');
  }

  /**
   * Get access token
   */
  getAccessToken(): string | null {
    return localStorage.getItem('officer_access_token');
  }

  /**
   * Get refresh token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem('officer_refresh_token');
  }

  /**
   * Check if officer is authenticated
   */
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  /**
   * Logout
   */
  logout(): void {
    localStorage.removeItem('officer_access_token');
    localStorage.removeItem('officer_refresh_token');
    localStorage.removeItem('officer_user_id');
    localStorage.removeItem('officer_email');
    localStorage.removeItem('officer_mobile');
    this.router.navigate(['/officer/login']);
  }

  /**
   * Get HTTP headers with authorization
   */
  getAuthHeaders(): HttpHeaders {
    const token = this.getAccessToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }
}
```

#### 2. Login Component (`officer-login.component.ts`)

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { OfficerAuthService } from '../services/officer-auth.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-officer-login',
  templateUrl: './officer-login.component.html'
})
export class OfficerLoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage: string | null = null;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private officerAuthService: OfficerAuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      userid: ['', [
        Validators.required,
        Validators.pattern('^[A-Z_]+@[A-Z0-9]+$') // ROLE@LGD format
      ]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // Check if already authenticated
    if (this.officerAuthService.isAuthenticated()) {
      this.router.navigate(['/officer/dashboard']);
    }

    // Pre-fill userid from query params (after password reset)
    this.route.queryParams.subscribe(params => {
      if (params['userid']) {
        this.loginForm.patchValue({ userid: params['userid'] });
      }
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = null;

      const { userid, password } = this.loginForm.value;

      this.officerAuthService.login(userid, password).subscribe({
        next: (response) => {
          if (response.success) {
            // Login successful - redirect to dashboard
            this.router.navigate(['/officer/dashboard']);
          }
        },
        error: (error) => {
          this.isLoading = false;
          
          if (error.status === 401) {
            const message = error.error?.message || 'Login failed';
            
            if (message.includes('Password reset required')) {
              // This will be handled by the service (redirects to reset page)
              this.errorMessage = 'Password reset required. Redirecting...';
            } else {
              this.errorMessage = message;
            }
          } else {
            this.errorMessage = error.error?.message || 'An error occurred. Please try again.';
          }
        }
      });
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
    }
  }

  getErrorMessage(fieldName: string): string {
    const control = this.loginForm.get(fieldName);
    if (control?.hasError('required')) {
      return `${fieldName} is required`;
    }
    if (control?.hasError('pattern')) {
      return 'Invalid UserID format. Expected: ROLE@LGD_CODE (e.g., DISTRICT_OFFICER@IMW001)';
    }
    return '';
  }
}
```

#### 3. Login Component Template (`officer-login.component.html`)

```html
<div class="login-container">
  <div class="login-card">
    <h2>Officer Login</h2>
    <p class="subtitle">Enter your UserID and password to login</p>

    <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
      <!-- UserID Field -->
      <div class="form-group">
        <label for="userid">UserID</label>
        <input
          type="text"
          id="userid"
          formControlName="userid"
          placeholder="e.g., DISTRICT_OFFICER@IMW001"
          class="form-control"
          [class.is-invalid]="loginForm.get('userid')?.invalid && loginForm.get('userid')?.touched"
        />
        <small class="form-text text-muted">
          Format: ROLE_CODE@UNIT_LGD_CODE
        </small>
        <div *ngIf="loginForm.get('userid')?.invalid && loginForm.get('userid')?.touched" class="invalid-feedback">
          {{ getErrorMessage('userid') }}
        </div>
      </div>

      <!-- Password Field -->
      <div class="form-group">
        <label for="password">Password</label>
        <input
          type="password"
          id="password"
          formControlName="password"
          placeholder="Enter your password"
          class="form-control"
          [class.is-invalid]="loginForm.get('password')?.invalid && loginForm.get('password')?.touched"
        />
        <div *ngIf="loginForm.get('password')?.invalid && loginForm.get('password')?.touched" class="invalid-feedback">
          {{ getErrorMessage('password') }}
        </div>
      </div>

      <!-- Error Message -->
      <div *ngIf="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>

      <!-- Submit Button -->
      <button
        type="submit"
        class="btn btn-primary btn-block"
        [disabled]="loginForm.invalid || isLoading"
      >
        <span *ngIf="!isLoading">Login</span>
        <span *ngIf="isLoading">
          <span class="spinner-border spinner-border-sm" role="status"></span>
          Logging in...
        </span>
      </button>
    </form>

    <!-- Help Text -->
    <div class="help-text">
      <p><strong>First time login?</strong></p>
      <p>Use your temporary password provided by admin. You will be prompted to reset your password.</p>
      <p><strong>Default Password Format:</strong></p>
      <p>Rccms@&lt;last4MobileDigits&gt; (e.g., Rccms@3210)</p>
      <p><strong>Forgot your UserID?</strong></p>
      <p>Contact your administrator.</p>
    </div>
  </div>
</div>
```

#### 4. Password Reset Component (`officer-reset-password.component.ts`)

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { OfficerAuthService } from '../services/officer-auth.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-officer-reset-password',
  templateUrl: './officer-reset-password.component.html'
})
export class OfficerResetPasswordComponent implements OnInit {
  resetForm: FormGroup;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  isLoading = false;
  userid: string = '';

  constructor(
    private fb: FormBuilder,
    private officerAuthService: OfficerAuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.resetForm = this.fb.group({
      userid: ['', [Validators.required, Validators.pattern('^[A-Z_]+@[A-Z0-9]+$')]],
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(100),
        this.passwordValidator
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    // Get userid from query params or session storage
    this.route.queryParams.subscribe(params => {
      if (params['userid']) {
        this.userid = params['userid'];
        this.resetForm.patchValue({ userid: this.userid });
      }
    });

    // Or get from session storage (set during login redirect)
    if (!this.userid) {
      const storedUserid = sessionStorage.getItem('reset_userid');
      if (storedUserid) {
        this.userid = storedUserid;
        this.resetForm.patchValue({ userid: this.userid });
      }
    }
  }

  passwordValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumber = /[0-9]/.test(value);
    const hasSpecialChar = /[@$!%*?&]/.test(value);

    if (!hasUpperCase || !hasLowerCase || !hasNumber || !hasSpecialChar) {
      return { passwordStrength: true };
    }

    return null;
  }

  passwordMatchValidator(form: FormGroup): ValidationErrors | null {
    const password = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    
    return null;
  }

  onSubmit(): void {
    if (this.resetForm.valid) {
      this.isLoading = true;
      this.errorMessage = null;
      this.successMessage = null;

      const { userid, newPassword, confirmPassword } = this.resetForm.value;

      this.officerAuthService.resetPassword(userid, newPassword, confirmPassword).subscribe({
        next: (response) => {
          if (response.success) {
            this.successMessage = 'Password reset successfully! Redirecting to login...';
            setTimeout(() => {
              this.router.navigate(['/officer/login'], {
                queryParams: { userid: userid }
              });
            }, 2000);
          }
        },
        error: (error) => {
          this.isLoading = false;
          
          if (error.error?.errors && Array.isArray(error.error.errors)) {
            // Validation errors
            const firstError = error.error.errors[0];
            this.errorMessage = firstError.message || 'Validation failed';
          } else {
            this.errorMessage = error.error?.message || 'Failed to reset password. Please try again.';
          }
        }
      });
    } else {
      // Mark all fields as touched
      Object.keys(this.resetForm.controls).forEach(key => {
        this.resetForm.get(key)?.markAsTouched();
      });
    }
  }

  getPasswordErrorMessage(): string {
    const control = this.resetForm.get('newPassword');
    if (control?.hasError('required')) {
      return 'Password is required';
    }
    if (control?.hasError('minlength')) {
      return 'Password must be at least 8 characters';
    }
    if (control?.hasError('passwordStrength')) {
      return 'Password must contain uppercase, lowercase, number, and special character (@$!%*?&)';
    }
    return '';
  }

  getConfirmPasswordErrorMessage(): string {
    const control = this.resetForm.get('confirmPassword');
    if (control?.hasError('required')) {
      return 'Confirm password is required';
    }
    if (control?.hasError('passwordMismatch')) {
      return 'Passwords do not match';
    }
    return '';
  }
}
```

#### 5. Password Reset Component Template (`officer-reset-password.component.html`)

```html
<div class="reset-password-container">
  <div class="reset-password-card">
    <h2>Reset Password</h2>
    <p class="subtitle">Set a new password for your account</p>

    <form [formGroup]="resetForm" (ngSubmit)="onSubmit()">
      <!-- UserID Field (Read-only if from redirect) -->
      <div class="form-group">
        <label for="userid">UserID</label>
        <input
          type="text"
          id="userid"
          formControlName="userid"
          placeholder="e.g., DISTRICT_OFFICER@IMW001"
          class="form-control"
          [readonly]="!!userid"
          [class.is-invalid]="resetForm.get('userid')?.invalid && resetForm.get('userid')?.touched"
        />
        <div *ngIf="resetForm.get('userid')?.invalid && resetForm.get('userid')?.touched" class="invalid-feedback">
          UserID is required
        </div>
      </div>

      <!-- New Password Field -->
      <div class="form-group">
        <label for="newPassword">New Password</label>
        <input
          type="password"
          id="newPassword"
          formControlName="newPassword"
          placeholder="Enter new password"
          class="form-control"
          [class.is-invalid]="resetForm.get('newPassword')?.invalid && resetForm.get('newPassword')?.touched"
        />
        <small class="form-text text-muted">
          Must be 8-100 characters with uppercase, lowercase, number, and special character (@$!%*?&)
        </small>
        <div *ngIf="resetForm.get('newPassword')?.invalid && resetForm.get('newPassword')?.touched" class="invalid-feedback">
          {{ getPasswordErrorMessage() }}
        </div>
      </div>

      <!-- Confirm Password Field -->
      <div class="form-group">
        <label for="confirmPassword">Confirm Password</label>
        <input
          type="password"
          id="confirmPassword"
          formControlName="confirmPassword"
          placeholder="Confirm new password"
          class="form-control"
          [class.is-invalid]="resetForm.get('confirmPassword')?.invalid && resetForm.get('confirmPassword')?.touched"
        />
        <div *ngIf="resetForm.get('confirmPassword')?.invalid && resetForm.get('confirmPassword')?.touched" class="invalid-feedback">
          {{ getConfirmPasswordErrorMessage() }}
        </div>
      </div>

      <!-- Success Message -->
      <div *ngIf="successMessage" class="alert alert-success">
        {{ successMessage }}
      </div>

      <!-- Error Message -->
      <div *ngIf="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>

      <!-- Submit Button -->
      <button
        type="submit"
        class="btn btn-primary btn-block"
        [disabled]="resetForm.invalid || isLoading"
      >
        <span *ngIf="!isLoading">Reset Password</span>
        <span *ngIf="isLoading">
          <span class="spinner-border spinner-border-sm" role="status"></span>
          Resetting...
        </span>
      </button>
    </form>

    <!-- Back to Login -->
    <div class="text-center mt-3">
      <a routerLink="/officer/login">Back to Login</a>
    </div>
  </div>
</div>
```

#### 6. HTTP Interceptor for Officer Authentication (`officer-auth.interceptor.ts`)

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { OfficerAuthService } from './officer-auth.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';

@Injectable()
export class OfficerAuthInterceptor implements HttpInterceptor {
  constructor(
    private officerAuthService: OfficerAuthService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    // Skip token for auth endpoints
    if (req.url.includes('/api/admin/auth/')) {
      return next.handle(req);
    }

    // Only add token for officer/admin endpoints
    if (req.url.includes('/api/admin/') || req.url.includes('/api/officer/')) {
      const token = this.officerAuthService.getAccessToken();
      
      if (token) {
        req = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
      }
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Unauthorized - logout and redirect to login
          this.officerAuthService.logout();
          this.router.navigate(['/officer/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
```

---

## Complete Flow Examples

### Example 1: Complete Officer Login Flow

```typescript
// Complete Officer Login Flow
async function completeOfficerLoginFlow() {
  const userid = "DISTRICT_OFFICER@IMW001";
  const tempPassword = "Rccms@3210";

  try {
    // Step 1: Attempt login with temporary password
    const loginResponse = await officerAuthService.login(userid, tempPassword).toPromise();
    console.log("Login successful:", loginResponse);
    // Redirect to dashboard
    
  } catch (error) {
    if (error.status === 401 && error.error?.message?.includes('Password reset required')) {
      // Step 2: Reset password
      const resetResponse = await officerAuthService.resetPassword(
        userid,
        "NewSecure@123",
        "NewSecure@123"
      ).toPromise();
      
      console.log("Password reset successful:", resetResponse);
      
      // Step 3: Login again with new password
      const finalLogin = await officerAuthService.login(
        userid,
        "NewSecure@123"
      ).toPromise();
      
      console.log("Final login successful:", finalLogin);
      // Redirect to dashboard
    }
  }
}
```

### Example 2: Login with Error Handling

```typescript
// Login with comprehensive error handling
officerAuthService.login(userid, password).subscribe({
  next: (response) => {
    if (response.success) {
      // Store tokens and redirect
      console.log('Login successful');
      router.navigate(['/officer/dashboard']);
    }
  },
  error: (error) => {
    switch (error.status) {
      case 401:
        const message = error.error?.message || 'Login failed';
        if (message.includes('Password reset required')) {
          // Redirect handled by service
        } else if (message.includes('account is not active')) {
          alert('Your account is inactive. Please contact administrator.');
        } else if (message.includes('posting is not active')) {
          alert('Your posting is not active. Please contact administrator.');
        } else {
          alert('Invalid UserID or password. Please try again.');
        }
        break;
      case 400:
        alert('Invalid request. Please check your input.');
        break;
      default:
        alert('An error occurred. Please try again later.');
    }
  }
});
```

---

## Important Notes

1. **UserID Format**: Must match `ROLE_CODE@UNIT_LGD_CODE` pattern (e.g., `DISTRICT_OFFICER@IMW001`)

2. **Default Password**: Format is `Rccms@<last4MobileDigits>` (e.g., `Rccms@3210`)

3. **Password Reset**: Mandatory on first login - cannot skip this step

4. **Token Storage**: Use separate keys for officer tokens (`officer_access_token`) to avoid conflicts with citizen tokens

5. **Token Expiration**: 
   - Access token: 1 hour
   - Refresh token: 7 days

6. **Posting Required**: Officer must have an active posting (`isCurrent = true`) to login

7. **Account Status**: Officer account must be active (`isActive = true`)

8. **Password Complexity**: Must contain:
   - At least 8 characters
   - At least one uppercase letter
   - At least one lowercase letter
   - At least one number
   - At least one special character (@$!%*?&)

9. **JWT Token Claims**: Token includes posting information (role, unit, level) for authorization

10. **Error Handling**: Always handle password reset required error and redirect appropriately

---

## Support

For any issues or questions, please contact the backend development team.

**Last Updated:** January 2026

