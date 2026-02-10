# Lawyer Registration & Login API Documentation

## Overview

Lawyer accounts are stored in a separate `lawyers` table.
These APIs allow lawyers to register, verify OTP, and login using OTP or password.

All endpoints are under `/api/auth`.

---

## 1. Lawyer Registration

**Endpoint:** `POST /api/auth/lawyer/register`

**Request Body:**
```json
{
  "firstName": "Arjun",
  "lastName": "Singh",
  "email": "arjun.law@example.com",
  "mobileNumber": "9876543210",
  "dateOfBirth": "1985-05-10",
  "gender": "MALE",
  "address": "Imphal West, Manipur",
  "district": "Imphal West",
  "pincode": "795001",
  "aadharNumber": "123456789012",
  "password": "SecurePass@123",
  "confirmPassword": "SecurePass@123"
}
```

**Request Parameters:**
- `firstName` (string, required)
- `lastName` (string, required)
- `email` (string, required)
- `mobileNumber` (string, required)
- `dateOfBirth` (date, required)
- `gender` (string, required: MALE/FEMALE/OTHER)
- `address` (string, required)
- `district` (string, required)
- `pincode` (string, required)
- `aadharNumber` (string, required)
- `password` (string, required)
- `confirmPassword` (string, required)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful. OTP sent to mobile number.",
  "data": {
    "message": "Registration successful. OTP sent to mobile number.",
    "citizenId": 21,
    "otpCode": "123456",
    "expiryMinutes": 5,
    "note": "OTP is also logged to console. This is temporary until SMS API is integrated."
  }
}
```

**Response Parameters:**
- `citizenId` (number) → Lawyer ID
- `otpCode` (string) → temporary (also logged in console)
- `expiryMinutes` (number)

---

## 2. Send OTP for Lawyer Registration Verification

**Endpoint:** `POST /api/auth/lawyer/registration/send-otp`

**Request Body:**
```json
{
  "mobileNumber": "9876543210"
}
```

**Request Parameters:**
- `mobileNumber` (string, required)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP sent successfully for lawyer registration verification",
  "data": {
    "message": "OTP sent successfully for lawyer registration verification",
    "otpCode": "123456",
    "expiryMinutes": 5,
    "note": "OTP is also logged to console. This is temporary until SMS API is integrated."
  }
}
```

**Response Parameters:**
- `otpCode` (string) → temporary (also logged in console)
- `expiryMinutes` (number)

---

## 3. Verify Lawyer Registration OTP

**Endpoint:** `POST /api/auth/lawyer/registration/verify-otp`

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "otp": "123456"
}
```

**Request Parameters:**
- `mobileNumber` (string, required)
- `otp` (string, required)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Lawyer mobile number verified successfully",
  "data": {
    "message": "Mobile number verified successfully. Lawyer account activated.",
    "mobileNumber": "9876543210"
  }
}
```

---

## 4. Send OTP for Lawyer Login

**Endpoint:** `POST /api/auth/lawyer/send-otp`

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "citizenType": "LAWYER"
}
```

**Request Parameters:**
- `mobileNumber` (string, required)
- `citizenType` (string, optional) → LAWYER (if omitted backend defaults)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP sent successfully for lawyer login",
  "data": {
    "message": "OTP sent successfully for lawyer login",
    "otpCode": "123456",
    "expiryMinutes": 5,
    "note": "OTP is also logged to console. This is temporary until SMS API is integrated."
  }
}
```

**Response Parameters:**
- `otpCode` (string) → temporary (also logged in console)
- `expiryMinutes` (number)

---

## 5. Lawyer OTP Login

**Endpoint:** `POST /api/auth/lawyer/otp-login`

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "otp": "123456",
  "captcha": "ABC123",
  "captchaId": "uuid-here",
  "citizenType": "LAWYER"
}
```

**Request Parameters:**
- `mobileNumber` (string, required)
- `otp` (string, required)
- `captcha` (string, required)
- `captchaId` (string, required)
- `citizenType` (string, optional) → LAWYER (if omitted backend defaults)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Lawyer login successful",
  "data": {
    "token": "jwt-access-token",
    "refreshToken": "refresh-token",
    "userId": 21,
    "citizenType": "LAWYER",
    "email": "arjun.law@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

**Response Parameters:**
- `token` (string)
- `refreshToken` (string)
- `userId` (number) → Lawyer ID
- `citizenType` (string) → LAWYER
- `email` (string)
- `mobileNumber` (string)
- `expiresIn` (number)

---

## 6. Lawyer Password Login

**Endpoint:** `POST /api/auth/lawyer/login`

**Request Body:**
```json
{
  "username": "9876543210",
  "password": "SecurePass@123",
  "captcha": "XYZ789",
  "captchaId": "uuid-here",
  "citizenType": "LAWYER"
}
```

**Request Parameters:**
- `username` (string, required) → mobile or email
- `password` (string, required)
- `captcha` (string, required)
- `captchaId` (string, required)
- `citizenType` (string, optional) → LAWYER (if omitted backend defaults)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Lawyer login successful",
  "data": {
    "token": "jwt-access-token",
    "refreshToken": "refresh-token",
    "userId": 21,
    "citizenType": "LAWYER",
    "email": "arjun.law@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

---

## Notes

- Lawyers are stored in the `lawyers` table (separate from citizens).
- OTP login requires the lawyer account to be active and mobile verified.
- Password login does not require mobile verification (same as citizen).
- CAPTCHA is required for login.
- OTP is returned in response and logged to console until SMS API is integrated.

---

**Last Updated:** After lawyer auth endpoints implementation
