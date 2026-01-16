# Citizen Authentication API Documentation

**Base URL:** `http://localhost:8080/api/auth`

This document provides comprehensive documentation for all Citizen Registration and Login APIs, including OTP and CAPTCHA integration.

---

## Table of Contents

1. [Overview](#overview)
2. [API Response Format](#api-response-format)
3. [Error Handling](#error-handling)
4. [Citizen Registration Flow](#citizen-registration-flow)
5. [Citizen Login Flow](#citizen-login-flow)
6. [API Endpoints](#api-endpoints)
7. [Frontend Implementation Guide](#frontend-implementation-guide)
8. [Complete Flow Examples](#complete-flow-examples)

---

## Overview

The RCCMS Citizen Authentication system supports:
- **Citizen Registration** with mobile OTP verification
- **Password-based Login** with CAPTCHA
- **OTP-based Login** with CAPTCHA
- **Token Refresh** for maintaining sessions

### Key Features:
- Mobile number verification via OTP (6 digits, valid for 5 minutes)
- CAPTCHA protection for login endpoints
- JWT-based authentication with access and refresh tokens
- Account activation after mobile verification

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
  "timestamp": "2026-01-09T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error message",
  "errors": [
    {
      "field": "mobileNumber",
      "message": "Mobile number must be 10 digits starting with 6-9"
    }
  ],
  "path": "/api/auth/citizen/register"
}
```

---

## Error Handling

### HTTP Status Codes:
- **200 OK**: Request successful
- **201 Created**: Resource created successfully
- **400 Bad Request**: Validation errors or invalid input
- **401 Unauthorized**: Invalid credentials or expired token
- **403 Forbidden**: Account not active or not verified
- **404 Not Found**: Resource not found
- **409 Conflict**: Duplicate resource (email/mobile already exists)
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server error

### Common Error Messages:
- `"Mobile number must be 10 digits starting with 6-9"`
- `"OTP must be exactly 6 digits"`
- `"Invalid or expired OTP"`
- `"Mobile number not registered"`
- `"Account is inactive. Please contact support."`
- `"Invalid CAPTCHA"`
- `"Email already registered"`
- `"Mobile number already registered"`

---

## Citizen Registration Flow

### Step-by-Step Process:

1. **Generate CAPTCHA** (optional, but recommended)
2. **Register Citizen** → Returns OTP code (temporary - until SMS API integrated)
3. **Verify Registration OTP** → Activates account

### Flow Diagram:
```
[Frontend] → [1. Generate CAPTCHA] → [2. Register Citizen] → [3. Verify OTP] → [Account Activated]
```

---

## Citizen Login Flow

### Two Login Methods:

#### Method 1: Password-based Login
1. **Generate CAPTCHA**
2. **Login with Password** → Returns JWT tokens

#### Method 2: OTP-based Login
1. **Generate CAPTCHA**
2. **Request OTP** → OTP sent to mobile
3. **Login with OTP** → Returns JWT tokens

### Flow Diagram:
```
Password Login:
[Frontend] → [1. Generate CAPTCHA] → [2. Login with Password] → [JWT Tokens]

OTP Login:
[Frontend] → [1. Generate CAPTCHA] → [2. Request OTP] → [3. Login with OTP] → [JWT Tokens]
```

---

## API Endpoints

### 1. Generate CAPTCHA

**Endpoint:** `GET /api/auth/captcha/generate`

**Description:** Generate a new CAPTCHA code for login/registration forms.

**Request:**
```http
GET /api/auth/captcha/generate
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "CAPTCHA generated successfully",
  "data": {
    "captchaId": "550e8400-e29b-41d4-a716-446655440000",
    "captchaText": "A3B7K9"
  }
}
```

**Frontend Usage:**
- Store `captchaId` and `captchaText`
- Display `captchaText` to user
- Include `captchaId` and user-entered CAPTCHA in login/OTP requests

---

### 2. Citizen Registration

**Endpoint:** `POST /api/auth/citizen/register`

**Description:** Register a new citizen. OTP will be sent to mobile number (currently returned in response for testing).

**Request:**
```http
POST /api/auth/citizen/register
Content-Type: application/json
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "address": "123 Main Street, Imphal West",
  "district": "Imphal West",
  "pincode": "795001",
  "aadharNumber": "123456789012",
  "password": "Secure@123",
  "confirmPassword": "Secure@123"
}
```

**Field Validations:**
- `firstName`: Required, letters only, max 50 characters
- `lastName`: Required, letters only, max 50 characters
- `email`: Required, valid email format
- `mobileNumber`: Required, 10 digits starting with 6-9
- `dateOfBirth`: Required, must be in the past (YYYY-MM-DD)
- `gender`: Required, enum: `MALE`, `FEMALE`, `OTHER`
- `address`: Required, 10-500 characters
- `district`: Required, max 100 characters
- `pincode`: Required, exactly 6 digits
- `aadharNumber`: Required, exactly 12 digits
- `password`: Required, 8-100 characters, must contain uppercase, lowercase, number, and special character
- `confirmPassword`: Required, must match password

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful. OTP sent to mobile number.",
  "data": {
    "message": "Registration successful. OTP sent to mobile number.",
    "citizenId": 1,
    "otpCode": "123456",
    "expiryMinutes": 5,
    "note": "OTP is also logged to console. This is temporary until SMS API is integrated."
  }
}
```

**Error Responses:**
- **400**: Validation errors
- **409**: Email/Mobile/Aadhar already exists

**Frontend Notes:**
- Store `citizenId` and `otpCode` from response
- Show OTP input form to user
- OTP expires in 5 minutes
- Account is **INACTIVE** until OTP is verified

---

### 3. Send OTP for Registration Verification

**Endpoint:** `POST /api/auth/citizen/registration/send-otp`

**Description:** Resend OTP for registration verification. Use this if user didn't receive OTP or it expired.

**Request:**
```http
POST /api/auth/citizen/registration/send-otp
Content-Type: application/json
```

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "citizenType": "CITIZEN"
}
```

**Field Validations:**
- `mobileNumber`: Required, 10 digits starting with 6-9
- `citizenType`: Required, enum: `CITIZEN` or `OPERATOR` (defaults to `CITIZEN` if not provided)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP sent successfully for registration verification",
  "data": {
    "message": "OTP sent successfully for registration verification",
    "otpCode": "654321",
    "expiryMinutes": 5,
    "note": "OTP is also logged to console. This is temporary until SMS API is integrated."
  }
}
```

**Error Responses:**
- **400**: Invalid mobile number format
- **404**: Mobile number not registered

---

### 4. Verify Registration OTP

**Endpoint:** `POST /api/auth/citizen/registration/verify-otp`

**Description:** Verify OTP sent during registration. This activates the citizen account.

**Request:**
```http
POST /api/auth/citizen/registration/verify-otp
Content-Type: application/json
```

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "otp": "123456"
}
```

**Field Validations:**
- `mobileNumber`: Required, 10 digits starting with 6-9
- `otp`: Required, exactly 6 digits

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Mobile number verified successfully. Citizen account activated.",
  "data": {
    "message": "Mobile number verified successfully. Citizen account activated.",
    "mobileNumber": "9876543210",
    "status": "ACTIVE",
    "isMobileVerified": true
  }
}
```

**Error Responses:**
- **400**: Invalid request format or invalid/expired OTP
- **404**: Citizen not found with the provided mobile number

**Frontend Notes:**
- After successful verification, account is **ACTIVE**
- User can now login
- Show success message and redirect to login page

---

### 5. Send OTP for Citizen Login

**Endpoint:** `POST /api/auth/citizen/send-otp`

**Description:** Send OTP to mobile number for OTP-based login. Use this for login when account is already active.

**Request:**
```http
POST /api/auth/citizen/send-otp
Content-Type: application/json
```

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "citizenType": "CITIZEN"
}
```

**Field Validations:**
- `mobileNumber`: Required, 10 digits starting with 6-9
- `citizenType`: Required, enum: `CITIZEN` or `OPERATOR`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP sent successfully for citizen login",
  "data": {
    "message": "OTP sent successfully for citizen login",
    "otpCode": "789012",
    "expiryMinutes": 5,
    "note": "OTP is also logged to console. This is temporary until SMS API is integrated."
  }
}
```

**Error Responses:**
- **400**: Invalid mobile number format
- **401**: Account not active or mobile number not registered

**Frontend Notes:**
- Only works for **ACTIVE** accounts
- OTP expires in 5 minutes
- Use this before calling OTP login endpoint

---

### 6. Citizen Login (Password-based)

**Endpoint:** `POST /api/auth/citizen/login`

**Description:** Login with mobile number/email and password. Returns JWT access token and refresh token.

**Request:**
```http
POST /api/auth/citizen/login
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "9876543210",
  "password": "Secure@123",
  "captcha": "A3B7K9",
  "captchaId": "550e8400-e29b-41d4-a716-446655440000",
  "citizenType": "CITIZEN"
}
```

**Field Validations:**
- `username`: Required, mobile number (10 digits) or email
- `password`: Required, 1-100 characters
- `captcha`: Required, 4-10 characters (case-insensitive)
- `captchaId`: Required, UUID from CAPTCHA generation
- `citizenType`: Required, enum: `CITIZEN` or `OPERATOR`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Citizen login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "citizenType": "CITIZEN",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

**Error Responses:**
- **400**: Validation errors or invalid CAPTCHA
- **401**: Invalid credentials
- **403**: Account not active or not verified

**Frontend Notes:**
- Store `token` and `refreshToken` securely (localStorage/sessionStorage)
- Include `token` in `Authorization` header for authenticated requests: `Authorization: Bearer <token>`
- Token expires in `expiresIn` seconds (default: 3600 = 1 hour)
- Use `refreshToken` to get new access token when it expires

---

### 7. Citizen Login (OTP-based)

**Endpoint:** `POST /api/auth/citizen/otp-login`

**Description:** Login by verifying OTP code sent to mobile number. Returns JWT access token and refresh token.

**Request:**
```http
POST /api/auth/citizen/otp-login
Content-Type: application/json
```

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "otp": "789012",
  "captcha": "A3B7K9",
  "captchaId": "550e8400-e29b-41d4-a716-446655440000",
  "citizenType": "CITIZEN"
}
```

**Field Validations:**
- `mobileNumber`: Required, 10 digits starting with 6-9
- `otp`: Required, exactly 6 digits
- `captcha`: Required, 4-10 characters (case-insensitive)
- `captchaId`: Required, UUID from CAPTCHA generation
- `citizenType`: Required, enum: `CITIZEN` or `OPERATOR`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Citizen login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "citizenType": "CITIZEN",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

**Error Responses:**
- **400**: Invalid OTP, expired OTP, or invalid CAPTCHA
- **401**: Invalid credentials

**Frontend Notes:**
- Must call `/api/auth/citizen/send-otp` first to receive OTP
- OTP must be verified within 5 minutes
- Store tokens securely after successful login

---

### 8. Refresh Token

**Endpoint:** `POST /api/auth/refresh-token`

**Description:** Refresh access token using refresh token. Use this when access token expires.

**Request:**
```http
POST /api/auth/refresh-token
Content-Type: application/json
```

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "citizenType": "CITIZEN",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

**Error Responses:**
- **400**: Refresh token is required
- **401**: Invalid or expired refresh token

**Frontend Notes:**
- Call this endpoint when access token expires (401 Unauthorized)
- Update stored tokens with new values
- Refresh token expires in 7 days

---

## Frontend Implementation Guide

### Angular/TypeScript Example

#### 1. API Service (`auth.service.ts`)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface CaptchaResponse {
  captchaId: string;
  captchaText: string;
}

export interface RegistrationRequest {
  firstName: string;
  lastName: string;
  email: string;
  mobileNumber: string;
  dateOfBirth: string; // YYYY-MM-DD
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  address: string;
  district: string;
  pincode: string;
  aadharNumber: string;
  password: string;
  confirmPassword: string;
}

export interface RegistrationResponse {
  citizenId: number;
  otpCode: string;
  expiryMinutes: number;
}

export interface OtpVerificationRequest {
  mobileNumber: string;
  otp: string;
}

export interface LoginRequest {
  username: string; // mobile or email
  password: string;
  captcha: string;
  captchaId: string;
  citizenType: 'CITIZEN' | 'OPERATOR';
}

export interface OtpLoginRequest {
  mobileNumber: string;
  otp: string;
  captcha: string;
  captchaId: string;
  citizenType: 'CITIZEN' | 'OPERATOR';
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  userId: number;
  citizenType: 'CITIZEN' | 'OPERATOR';
  email: string;
  mobileNumber: string;
  expiresIn: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  // Generate CAPTCHA
  generateCaptcha(): Observable<ApiResponse<CaptchaResponse>> {
    return this.http.get<ApiResponse<CaptchaResponse>>(`${this.baseUrl}/captcha/generate`);
  }

  // Register Citizen
  registerCitizen(data: RegistrationRequest): Observable<ApiResponse<RegistrationResponse>> {
    return this.http.post<ApiResponse<RegistrationResponse>>(
      `${this.baseUrl}/citizen/register`,
      data
    );
  }

  // Send OTP for Registration Verification
  sendRegistrationOtp(mobileNumber: string, citizenType: 'CITIZEN' | 'OPERATOR' = 'CITIZEN'): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/citizen/registration/send-otp`,
      { mobileNumber, citizenType }
    );
  }

  // Verify Registration OTP
  verifyRegistrationOtp(mobileNumber: string, otp: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/citizen/registration/verify-otp`,
      { mobileNumber, otp }
    );
  }

  // Send OTP for Login
  sendLoginOtp(mobileNumber: string, citizenType: 'CITIZEN' | 'OPERATOR'): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/citizen/send-otp`,
      { mobileNumber, citizenType }
    );
  }

  // Password Login
  login(data: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.baseUrl}/citizen/login`,
      data
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.storeTokens(response.data);
        }
      })
    );
  }

  // OTP Login
  loginWithOtp(data: OtpLoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.baseUrl}/citizen/otp-login`,
      data
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.storeTokens(response.data);
        }
      })
    );
  }

  // Refresh Token
  refreshToken(refreshToken: string): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.baseUrl}/refresh-token`,
      { refreshToken }
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.storeTokens(response.data);
        }
      })
    );
  }

  // Store tokens
  private storeTokens(authData: AuthResponse): void {
    localStorage.setItem('access_token', authData.token);
    localStorage.setItem('refresh_token', authData.refreshToken);
    localStorage.setItem('user_id', authData.userId.toString());
    localStorage.setItem('citizen_type', authData.citizenType);
  }

  // Get access token
  getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  // Get refresh token
  getRefreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  // Logout
  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_id');
    localStorage.removeItem('citizen_type');
  }

  // Get HTTP headers with authorization
  getAuthHeaders(): HttpHeaders {
    const token = this.getAccessToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }
}
```

#### 2. Registration Component Example (`registration.component.ts`)

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html'
})
export class RegistrationComponent implements OnInit {
  registrationForm: FormGroup;
  captcha: any = null;
  registrationSuccess = false;
  otpSent = false;
  citizenId: number | null = null;
  otpCode: string | null = null;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registrationForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.pattern('^[a-zA-Z\\s]+$')]],
      lastName: ['', [Validators.required, Validators.pattern('^[a-zA-Z\\s]+$')]],
      email: ['', [Validators.required, Validators.email]],
      mobileNumber: ['', [Validators.required, Validators.pattern('^[6-9]\\d{9}$')]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      address: ['', [Validators.required, Validators.minLength(10)]],
      district: ['', Validators.required],
      pincode: ['', [Validators.required, Validators.pattern('^\\d{6}$')]],
      aadharNumber: ['', [Validators.required, Validators.pattern('^\\d{12}$')]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$')
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadCaptcha();
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
    }
    return null;
  }

  loadCaptcha(): void {
    this.authService.generateCaptcha().subscribe({
      next: (response) => {
        if (response.success) {
          this.captcha = response.data;
        }
      },
      error: (error) => {
        console.error('Failed to load CAPTCHA', error);
      }
    });
  }

  onSubmit(): void {
    if (this.registrationForm.valid) {
      const formData = this.registrationForm.value;
      this.errorMessage = null;

      this.authService.registerCitizen(formData).subscribe({
        next: (response) => {
          if (response.success) {
            this.registrationSuccess = true;
            this.citizenId = response.data.citizenId;
            this.otpCode = response.data.otpCode;
            this.otpSent = true;
            // Show OTP input form
          }
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Registration failed';
          this.loadCaptcha(); // Reload CAPTCHA on error
        }
      });
    }
  }

  resendOtp(): void {
    const mobileNumber = this.registrationForm.get('mobileNumber')?.value;
    if (mobileNumber) {
      this.authService.sendRegistrationOtp(mobileNumber).subscribe({
        next: (response) => {
          if (response.success) {
            this.otpCode = response.data.otpCode;
            alert('OTP resent successfully');
          }
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Failed to resend OTP';
        }
      });
    }
  }

  verifyOtp(otp: string): void {
    const mobileNumber = this.registrationForm.get('mobileNumber')?.value;
    if (mobileNumber && otp) {
      this.authService.verifyRegistrationOtp(mobileNumber, otp).subscribe({
        next: (response) => {
          if (response.success) {
            alert('Registration successful! Account activated.');
            this.router.navigate(['/login']);
          }
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Invalid OTP';
        }
      });
    }
  }
}
```

#### 3. Login Component Example (`login.component.ts`)

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  otpLoginForm: FormGroup;
  captcha: any = null;
  loginMode: 'password' | 'otp' = 'password';
  otpSent = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    // Password login form
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
      captcha: ['', Validators.required],
      captchaId: ['', Validators.required],
      citizenType: ['CITIZEN', Validators.required]
    });

    // OTP login form
    this.otpLoginForm = this.fb.group({
      mobileNumber: ['', [Validators.required, Validators.pattern('^[6-9]\\d{9}$')]],
      otp: ['', [Validators.required, Validators.pattern('^\\d{6}$')]],
      captcha: ['', Validators.required],
      captchaId: ['', Validators.required],
      citizenType: ['CITIZEN', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadCaptcha();
  }

  loadCaptcha(): void {
    this.authService.generateCaptcha().subscribe({
      next: (response) => {
        if (response.success) {
          this.captcha = response.data;
          this.loginForm.patchValue({ captchaId: this.captcha.captchaId });
          this.otpLoginForm.patchValue({ captchaId: this.captcha.captchaId });
        }
      },
      error: (error) => {
        console.error('Failed to load CAPTCHA', error);
      }
    });
  }

  switchMode(mode: 'password' | 'otp'): void {
    this.loginMode = mode;
    this.otpSent = false;
    this.errorMessage = null;
    this.loadCaptcha();
  }

  // Password Login
  onPasswordLogin(): void {
    if (this.loginForm.valid) {
      this.errorMessage = null;
      this.authService.login(this.loginForm.value).subscribe({
        next: (response) => {
          if (response.success) {
            this.router.navigate(['/dashboard']);
          }
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Login failed';
          this.loadCaptcha(); // Reload CAPTCHA on error
        }
      });
    }
  }

  // Request OTP for Login
  requestOtp(): void {
    const mobileNumber = this.otpLoginForm.get('mobileNumber')?.value;
    const citizenType = this.otpLoginForm.get('citizenType')?.value;
    
    if (mobileNumber && citizenType) {
      this.authService.sendLoginOtp(mobileNumber, citizenType).subscribe({
        next: (response) => {
          if (response.success) {
            this.otpSent = true;
            alert(`OTP sent to ${mobileNumber}. OTP: ${response.data.otpCode} (for testing)`);
          }
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Failed to send OTP';
        }
      });
    }
  }

  // OTP Login
  onOtpLogin(): void {
    if (this.otpLoginForm.valid) {
      this.errorMessage = null;
      this.authService.loginWithOtp(this.otpLoginForm.value).subscribe({
        next: (response) => {
          if (response.success) {
            this.router.navigate(['/dashboard']);
          }
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Login failed';
          this.loadCaptcha(); // Reload CAPTCHA on error
        }
      });
    }
  }
}
```

#### 4. HTTP Interceptor for Token (`auth.interceptor.ts`)

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './auth.service';
import { catchError, switchMap } from 'rxjs/operators';
import { throwError, BehaviorSubject } from 'rxjs';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    // Skip token for auth endpoints
    if (req.url.includes('/api/auth/')) {
      return next.handle(req);
    }

    const token = this.authService.getAccessToken();
    
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !req.url.includes('/refresh-token')) {
          return this.handle401Error(req, next);
        }
        return throwError(() => error);
      })
    );
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler) {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = this.authService.getRefreshToken();
      
      if (refreshToken) {
        return this.authService.refreshToken(refreshToken).pipe(
          switchMap((response: any) => {
            this.isRefreshing = false;
            this.refreshTokenSubject.next(response.data.token);
            return next.handle(this.addTokenHeader(request, response.data.token));
          }),
          catchError((err) => {
            this.isRefreshing = false;
            this.authService.logout();
            this.router.navigate(['/login']);
            return throwError(() => err);
          })
        );
      }
    }

    return this.refreshTokenSubject.pipe(
      switchMap((token) => {
        if (token) {
          return next.handle(this.addTokenHeader(request, token));
        }
        this.authService.logout();
        this.router.navigate(['/login']);
        return throwError(() => new Error('Token refresh failed'));
      })
    );
  }

  private addTokenHeader(request: HttpRequest<any>, token: string) {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
}
```

---

## Complete Flow Examples

### Example 1: Complete Registration Flow

```typescript
// Step 1: Generate CAPTCHA (optional for registration)
const captcha = await authService.generateCaptcha().toPromise();
// Store captcha.captchaId and captcha.captchaText

// Step 2: Register Citizen
const registrationData = {
  firstName: "John",
  lastName: "Doe",
  email: "john.doe@example.com",
  mobileNumber: "9876543210",
  dateOfBirth: "1990-01-15",
  gender: "MALE",
  address: "123 Main Street, Imphal West",
  district: "Imphal West",
  pincode: "795001",
  aadharNumber: "123456789012",
  password: "Secure@123",
  confirmPassword: "Secure@123"
};

const registrationResponse = await authService.registerCitizen(registrationData).toPromise();
// registrationResponse.data.otpCode contains the OTP (for testing)
// registrationResponse.data.citizenId contains the citizen ID

// Step 3: Verify OTP
const otpVerification = await authService.verifyRegistrationOtp(
  "9876543210",
  registrationResponse.data.otpCode
).toPromise();

// Account is now ACTIVE - user can login
```

### Example 2: Password Login Flow

```typescript
// Step 1: Generate CAPTCHA
const captcha = await authService.generateCaptcha().toPromise();

// Step 2: Login with Password
const loginData = {
  username: "9876543210", // or email
  password: "Secure@123",
  captcha: captcha.data.captchaText,
  captchaId: captcha.data.captchaId,
  citizenType: "CITIZEN"
};

const loginResponse = await authService.login(loginData).toPromise();
// Tokens are automatically stored in localStorage
// loginResponse.data.token - Access token
// loginResponse.data.refreshToken - Refresh token
```

### Example 3: OTP Login Flow

```typescript
// Step 1: Generate CAPTCHA
const captcha = await authService.generateCaptcha().toPromise();

// Step 2: Request OTP
const otpRequest = await authService.sendLoginOtp("9876543210", "CITIZEN").toPromise();
// otpRequest.data.otpCode contains the OTP (for testing)

// Step 3: Login with OTP
const otpLoginData = {
  mobileNumber: "9876543210",
  otp: otpRequest.data.otpCode,
  captcha: captcha.data.captchaText,
  captchaId: captcha.data.captchaId,
  citizenType: "CITIZEN"
};

const loginResponse = await authService.loginWithOtp(otpLoginData).toPromise();
// Tokens are automatically stored in localStorage
```

---

## Important Notes

1. **OTP Expiry**: OTPs expire in 5 minutes. User must verify within this time.

2. **CAPTCHA Expiry**: CAPTCHAs expire in 10 minutes. Generate a new one if expired.

3. **Token Storage**: Store tokens securely. Use `localStorage` or `sessionStorage` based on your security requirements.

4. **Token Refresh**: Access tokens expire in 1 hour. Use refresh token to get new access token before expiry.

5. **Account Status**: 
   - After registration: Account is **INACTIVE** until OTP is verified
   - After OTP verification: Account becomes **ACTIVE**
   - Only **ACTIVE** accounts can login

6. **Testing**: Currently, OTP is returned in API response for testing. This will be removed when SMS API is integrated.

7. **CORS**: Backend allows `localhost:4200` and `localhost:4201` for Angular development.

8. **Error Handling**: Always handle errors gracefully and show user-friendly messages.

---

## Support

For any issues or questions, please contact the backend development team.

**Last Updated:** January 2026

