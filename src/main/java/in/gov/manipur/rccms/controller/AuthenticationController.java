package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.User;
import in.gov.manipur.rccms.service.AuthService;
import in.gov.manipur.rccms.service.OtpService;
import in.gov.manipur.rccms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 * Handles user authentication (registration, login, OTP, refresh token)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints (Citizen/Operator)")
public class AuthenticationController {

    private final AuthService authService;
    private final UserService userService;
    private final OtpService otpService;

    /**
     * Citizen Registration
     * POST /api/auth/citizen/register
     */
    @Operation(
            summary = "Citizen Registration",
            description = "Register a new citizen. OTP will be sent to mobile number for verification."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Registration successful. OTP sent to mobile number.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation errors",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email/Mobile/Aadhar already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/citizen/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerCitizen(
            @Valid @RequestBody UserRegistrationDTO request) {
        log.info("Citizen registration request received for email: {}", maskEmail(request.getEmail()));
        
        Map<String, Object> registrationResult = userService.registerCitizen(request);
        Long userId = (Long) registrationResult.get("userId");
        String otpCode = (String) registrationResult.get("otpCode");
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Registration successful. OTP sent to mobile number.");
        response.put("userId", userId);
        response.put("otpCode", otpCode); // Temporary - OTP included in response for testing
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. OTP sent to mobile number.", response));
    }

    /**
     * Send OTP for Registration Verification (After Registration)
     * POST /api/auth/registration/send-otp
     * Use this endpoint AFTER registration to verify mobile number
     * User account is inactive at this point
     */
    @Operation(
            summary = "Send OTP for Registration Verification",
            description = "Send OTP to mobile number for registration verification. Use this AFTER registration when account is inactive. Rate limited to 3 requests per 15 minutes."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Mobile number not registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too many OTP requests",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/registration/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendRegistrationOtp(
            @RequestBody OtpRequestDTO request) {
        log.info("Registration OTP request received for mobile: {}", maskMobile(request.getMobileNumber()));
        
        // Validate mobile number
        if (request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        
        String trimmedMobile = request.getMobileNumber().trim();
        
        // Validate mobile number format
        if (!trimmedMobile.matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits starting with 6-9");
        }
        
        // For registration verification, userType is always CITIZEN (operators don't register)
        // If not provided or null, default to CITIZEN
        User.UserType userType = request.getUserType();
        if (userType == null) {
            userType = User.UserType.CITIZEN;
            log.info("UserType not provided, defaulting to CITIZEN for registration verification");
        }
        
        // For registration verification, allow OTP generation even if user lookup fails
        // (user might be in registration process or transaction not committed yet)
        // OtpService will handle this gracefully with allowInactive=true
        String otpCode = otpService.generateOtp(trimmedMobile, userType, true);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "OTP sent successfully for registration verification");
        response.put("otpCode", otpCode); // TODO: Remove this when SMS API is integrated - OTP should not be in response
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");
        
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully for registration verification", response));
    }

    /**
     * Send OTP for Mobile Login (For Active Users)
     * POST /api/auth/mobile/send-otp
     * Use this endpoint for LOGIN when user account is already active
     */
    @Operation(
            summary = "Send OTP for Login",
            description = "Send OTP to mobile number for login. Use this for LOGIN when user account is already active and verified. Rate limited to 3 requests per 15 minutes."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Account not active or mobile number not registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too many OTP requests",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/mobile/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendLoginOtp(
            @Valid @RequestBody OtpRequestDTO request) {
        log.info("Login OTP request received for mobile: {}", maskMobile(request.getMobileNumber()));
        
        String trimmedMobile = request.getMobileNumber().trim();
        
        // For login, require active user (allowInactive=false)
        String otpCode = otpService.generateOtp(trimmedMobile, request.getUserType(), false);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "OTP sent successfully for login");
        response.put("otpCode", otpCode); // TODO: Remove this when SMS API is integrated - OTP should not be in response
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");
        
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully for login", response));
    }

    /**
     * Verify OTP and Login
     * POST /api/auth/mobile/verify-otp
     */
    @Operation(
            summary = "Verify OTP and Login",
            description = "Verify OTP code and login. Returns JWT access token and refresh token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid OTP, expired OTP, or invalid CAPTCHA",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/mobile/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> verifyOtpAndLogin(
            @Valid @RequestBody OtpVerificationDTO request) {
        log.info("OTP verification request received for mobile: {}", maskMobile(request.getMobileNumber()));
        
        AuthResponseDTO response = authService.loginWithOtp(request);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Mobile/Email & Password Login
     * POST /api/auth/password/login
     */
    @Operation(
            summary = "Password Login",
            description = "Login with mobile number/email and password. Returns JWT access token and refresh token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials or invalid CAPTCHA",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Account not active or not verified",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/password/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> loginWithPassword(
            @Valid @RequestBody LoginRequestDTO request) {
        log.info("Password login request received for username: {}", maskUsername(request.getUsername()));
        
        AuthResponseDTO response = authService.loginWithPassword(request);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refresh Token
     * POST /api/auth/refresh-token
     */
    @Operation(
            summary = "Refresh Token",
            description = "Refresh access token using refresh token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(
            @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        
        AuthResponseDTO response = authService.refreshToken(refreshToken);
        
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Verify Mobile OTP (for registration)
     * POST /api/auth/verify-registration-otp
     */
    @Operation(
            summary = "Verify Registration OTP",
            description = "Verify mobile OTP sent during registration. Activates the account."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Mobile number verified successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyRegistrationOtp(
            @RequestBody Map<String, String> request) {
        String mobileNumber = request.get("mobileNumber");
        String otp = request.get("otp");
        
        if (mobileNumber == null || otp == null) {
            throw new IllegalArgumentException("Mobile number and OTP are required");
        }
        
        log.info("Registration OTP verification request for mobile: {}", maskMobile(mobileNumber));
        
        userService.verifyMobileOtp(mobileNumber, otp);
        
        Map<String, Object> response = Map.of(
                "message", "Mobile number verified successfully",
                "mobileNumber", mobileNumber
        );
        
        return ResponseEntity.ok(ApiResponse.success("Mobile number verified successfully", response));
    }

    /**
     * Mask email for logging
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return "****@" + parts[1];
        }
        return parts[0].substring(0, 2) + "****@" + parts[1];
    }

    /**
     * Mask mobile for logging
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 10) {
            return "****";
        }
        return mobile.substring(0, 2) + "****" + mobile.substring(8);
    }

    /**
     * Mask username for logging
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 4) {
            return "****";
        }
        return username.substring(0, 2) + "****" + username.substring(username.length() - 2);
    }
}
