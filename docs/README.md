# RCCMS Manipur Backend

Spring Boot backend application for RCCMS (Manipur) system with comprehensive authentication and user management.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Build Tool**: Maven
- **Database**: PostgreSQL (Production) / H2 (Development)
- **Security**: Spring Security with JWT authentication
- **API Documentation**: Swagger/OpenAPI 3 (SpringDoc)

## Project Structure

```
src/main/java/in/gov/manipur/rccms/
├── config/              # Configuration classes (CORS, OpenAPI, Security)
├── security/            # Spring Security configuration
├── controller/          # REST Controllers
├── service/             # Business logic layer
├── repository/          # Data access layer (JPA repositories)
├── entity/              # JPA entities (User, OTP, Captcha)
├── dto/                 # Data Transfer Objects
└── exception/           # Exception handlers
```

## Features

- ✅ Layered architecture (Controller → Service → Repository)
- ✅ JPA with audit fields (createdAt, updatedAt)
- ✅ Global exception handling with standardized error responses
- ✅ Standardized API response format
- ✅ CORS enabled for Angular frontend
- ✅ Spring Security with JWT authentication
- ✅ Refresh token support
- ✅ OTP-based authentication with rate limiting
- ✅ CAPTCHA generation and validation
- ✅ Password encryption (BCrypt)
- ✅ Aadhar number encryption (AES)
- ✅ Health check endpoint
- ✅ Profile-based configuration (dev/prod)
- ✅ Swagger/OpenAPI documentation with JWT Bearer authentication
- ✅ Scheduled tasks for cleanup (OTP, CAPTCHA)

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL (for production) or H2 (for development)

### Running the Application

1. **Development Mode (H2 Database)**:
   ```bash
   mvn spring-boot:run
   ```
   Or set active profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. **Production Mode (PostgreSQL)**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```
   Make sure PostgreSQL is running and update database credentials in `application.yml`

### API Endpoints

- **Health Check**: `GET http://localhost:8080/api/health`
- **Welcome Page**: `GET http://localhost:8080/`
- **H2 Console** (dev only): `http://localhost:8080/h2-console`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs (JSON)**: `http://localhost:8080/v3/api-docs`

## API Documentation

### Authentication APIs

#### 1. Citizen Registration
```
POST /api/auth/citizen/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "address": "123 Main Street, Imphal",
  "district": "Imphal East",
  "pincode": "795001",
  "aadharNumber": "123456789012",
  "password": "SecurePass@123",
  "confirmPassword": "SecurePass@123"
}
```

**Response (201 Created)**:
```json
{
  "success": true,
  "message": "Registration successful. OTP sent to mobile number.",
  "data": {
    "message": "Registration successful. OTP sent to mobile number.",
    "userId": 1
  }
}
```

**Validation Rules**:
- All fields required
- Email: Valid email format, unique
- Mobile: 10 digits, starting with 6-9, unique
- Password: Min 8 chars, must contain uppercase, lowercase, number, special character
- Aadhar: 12 digits, unique
- PIN: 6 digits
- Password and confirmPassword must match

**After Registration**:
- Password is hashed with BCrypt
- Aadhar number is encrypted (AES)
- 6-digit OTP is generated and logged to console (SMS API pending)
- Account is inactive until mobile verification
- OTP expires in 5 minutes

#### 2. Send OTP for Mobile Login
```
POST /api/auth/mobile/send-otp
Content-Type: application/json

{
  "mobileNumber": "9876543210",
  "userType": "CITIZEN"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "message": "OTP sent successfully",
    "expiryMinutes": 5
  }
}
```

**Rate Limiting**: Maximum 3 requests per 15 minutes per mobile number

#### 3. Verify OTP and Login
```
POST /api/auth/mobile/verify-otp
Content-Type: application/json

{
  "mobileNumber": "9876543210",
  "otp": "123456",
  "captcha": "ABC123",
  "captchaId": "uuid-here",
  "userType": "CITIZEN"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "jwt-access-token",
    "refreshToken": "refresh-token",
    "userId": 1,
    "userType": "CITIZEN",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

#### 4. Password Login
```
POST /api/auth/password/login
Content-Type: application/json

{
  "username": "9876543210",  // or "john.doe@example.com"
  "password": "SecurePass@123",
  "captcha": "XYZ789",
  "captchaId": "uuid-here",
  "userType": "CITIZEN"
}
```

**Response (200 OK)**: Same as OTP login response

#### 5. Refresh Token
```
POST /api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "refresh-token-here"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "token": "new-jwt-access-token",
    "refreshToken": "same-refresh-token",
    "userId": 1,
    "userType": "CITIZEN",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "expiresIn": 3600
  }
}
```

#### 6. Verify Registration OTP
```
POST /api/auth/verify-registration-otp
Content-Type: application/json

{
  "mobileNumber": "9876543210",
  "otp": "123456"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Mobile number verified successfully",
  "data": {
    "message": "Mobile number verified successfully",
    "mobileNumber": "9876543210"
  }
}
```

**After Verification**:
- `isMobileVerified` = true
- `isActive` = true
- OTP is marked as used

### CAPTCHA APIs

#### 1. Generate CAPTCHA
```
GET /api/auth/captcha/generate
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "CAPTCHA generated successfully",
  "data": {
    "captchaId": "uuid-here",
    "captchaText": "ABC123"
  }
}
```

**CAPTCHA Details**:
- 6 alphanumeric characters (case-insensitive)
- Expires in 10 minutes
- One-time use

#### 2. Validate CAPTCHA (Internal/Testing)
```
POST /api/auth/captcha/validate
Content-Type: application/json

{
  "captchaId": "uuid-here",
  "captchaText": "ABC123"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "CAPTCHA validation completed",
  "data": {
    "valid": true,
    "message": "Valid CAPTCHA"
  }
}
```

## Security Features

- **JWT Authentication**: Access tokens (1 hour expiry) and refresh tokens (7 days expiry)
- **Password Hashing**: BCrypt with strength 12
- **Aadhar Encryption**: AES encryption at rest
- **Rate Limiting**: 
  - OTP generation: Max 3 requests per 15 minutes per mobile
  - Login attempts: Max 5 attempts per 15 minutes per IP (to be implemented)
- **CORS**: Configured for Angular frontend (`http://localhost:4200`)
- **Input Validation**: Bean Validation annotations
- **SQL Injection Prevention**: Parameterized queries via JPA
- **XSS Prevention**: Input sanitization

## Token Usage

After successful login, include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt-token>
```

## Swagger UI

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

**Features**:
- Interactive API documentation
- Test endpoints directly from browser
- JWT Bearer token authentication support
- Click "Authorize" button to set JWT token for authenticated requests

## Configuration

### Application Profiles

- **dev**: Uses H2 in-memory database, detailed logging
- **prod**: Uses PostgreSQL, optimized logging

### Environment Variables

Update `application.yml` or use environment variables:

- `DB_USERNAME`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
- `JWT_SECRET`: JWT secret key (min 32 characters)
- `JWT_EXPIRATION`: Access token expiration in milliseconds (default: 3600000 = 1 hour)
- `JWT_REFRESH_EXPIRATION`: Refresh token expiration in milliseconds (default: 604800000 = 7 days)
- `ENCRYPTION_KEY`: AES encryption key for Aadhar numbers

## SMS Service

Currently, SMS service logs OTP to console. For production, integrate with SMS gateway:

- **Development**: OTP is logged to console
- **Production**: Integrate with Twilio, MSG91, or other SMS gateway

Example console output:
```
========================================
SMS TO: 9876543210
MESSAGE: Your RCCMS OTP is: 123456. Valid for 5 minutes.
========================================
```

## Scheduled Tasks

- **OTP Cleanup**: Runs every hour, deletes expired OTPs
- **CAPTCHA Cleanup**: Runs every hour, deletes expired CAPTCHAs

## Building the Project

```bash
mvn clean install
```

## Error Response Format

All error responses follow this format:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    },
    {
      "field": "mobileNumber",
      "message": "Invalid mobile number format"
    }
  ],
  "path": "/api/auth/citizen/register"
}
```

## Success Response Format

All success responses follow this format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

## Git Repository

- **GitHub**: https://github.com/ramniks05/rccmsmp-backend.git

## License

Government of Manipur - RCCMS Project
