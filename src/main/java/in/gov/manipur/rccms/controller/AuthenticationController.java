package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.Citizen;
import in.gov.manipur.rccms.service.AuthService;
import in.gov.manipur.rccms.service.CitizenService;
import in.gov.manipur.rccms.service.LawyerAuthService;
import in.gov.manipur.rccms.service.LawyerService;
import in.gov.manipur.rccms.service.OtpService;
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
    private final CitizenService citizenService;
    private final LawyerService lawyerService;
    private final LawyerAuthService lawyerAuthService;
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
            @Valid @RequestBody CitizenRegistrationDTO request) {
        log.info("Citizen registration request received for email: {}", maskEmail(request.getEmail()));
        
        Map<String, Object> registrationResult = citizenService.registerCitizen(request);
        Long citizenId = (Long) registrationResult.get("citizenId");
        String otpCode = (String) registrationResult.get("otpCode");
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Registration successful. OTP sent to mobile number.");
        response.put("citizenId", citizenId);
        response.put("otpCode", otpCode); // Temporary - OTP included in response for testing
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. OTP sent to mobile number.", response));
    }

    /**
     * Lawyer Registration
     * POST /api/auth/lawyer/register
     */
    @Operation(
            summary = "Lawyer Registration",
            description = "Register a new lawyer. OTP will be sent to mobile number for verification."
    )
    @PostMapping("/lawyer/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerLawyer(
            @Valid @RequestBody CitizenRegistrationDTO request) {
        log.info("Lawyer registration request received for email: {}", maskEmail(request.getEmail()));

        Map<String, Object> registrationResult = lawyerService.registerLawyer(request);
        Long citizenId = (Long) registrationResult.get("citizenId");
        String otpCode = (String) registrationResult.get("otpCode");

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Registration successful. OTP sent to mobile number.");
        response.put("citizenId", citizenId);
        response.put("otpCode", otpCode);
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
    @PostMapping("/citizen/registration/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendCitizenRegistrationOtp(
            @RequestBody OtpRequestDTO request) {
        log.info("Citizen registration OTP request received for mobile: {}", maskMobile(request.getMobileNumber()));
        
        // Validate mobile number
        if (request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        
        String trimmedMobile = request.getMobileNumber().trim();
        
        // Validate mobile number format
        if (!trimmedMobile.matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits starting with 6-9");
        }
        
        // For registration verification, citizenType is always CITIZEN (operators don't register)
        // If not provided or null, default to CITIZEN
        Citizen.CitizenType citizenType = request.getCitizenType();
        if (citizenType == null) {
            citizenType = Citizen.CitizenType.CITIZEN;
            log.info("CitizenType not provided, defaulting to CITIZEN for registration verification");
        }
        
        // For registration verification, allow OTP generation even if citizen lookup fails
        // (citizen might be in registration process or transaction not committed yet)
        // OtpService will handle this gracefully with allowInactive=true
        String otpCode = otpService.generateOtp(trimmedMobile, citizenType, true);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "OTP sent successfully for registration verification");
        response.put("otpCode", otpCode); // TODO: Remove this when SMS API is integrated - OTP should not be in response
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");
        
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully for registration verification", response));
    }

    /**
     * Send OTP for Lawyer Registration Verification
     * POST /api/auth/lawyer/registration/send-otp
     */
    @Operation(
            summary = "Send OTP for Lawyer Registration Verification",
            description = "Send OTP to mobile number for lawyer registration verification. Use this AFTER registration when account is inactive."
    )
    @PostMapping("/lawyer/registration/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendLawyerRegistrationOtp(
            @RequestBody OtpRequestDTO request) {
        log.info("Lawyer registration OTP request received for mobile: {}", maskMobile(request.getMobileNumber()));

        if (request.getMobileNumber() == null || request.getMobileNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        String trimmedMobile = request.getMobileNumber().trim();
        if (!trimmedMobile.matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits starting with 6-9");
        }

        String otpCode = otpService.generateOtp(trimmedMobile, Citizen.CitizenType.LAWYER, true);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "OTP sent successfully for lawyer registration verification");
        response.put("otpCode", otpCode);
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");

        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully for lawyer registration verification", response));
    }

    /**
     * Send OTP for Citizen Login
     * POST /api/auth/citizen/send-otp
     * Use this endpoint for LOGIN when citizen account is already active
     */
    @Operation(
            summary = "Send OTP for Citizen Login",
            description = "Send OTP to mobile number for citizen login. Use this for LOGIN when citizen account is already active and verified."
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
            )
    })
    @PostMapping("/citizen/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendCitizenLoginOtp(
            @Valid @RequestBody OtpRequestDTO request) {
        log.info("Citizen login OTP request received for mobile: {}", maskMobile(request.getMobileNumber()));
        
        String trimmedMobile = request.getMobileNumber().trim();
        
        if (request.getCitizenType() == null) {
            request.setCitizenType(Citizen.CitizenType.CITIZEN);
        }

        // For login, require active citizen (allowInactive=false)
        String otpCode = otpService.generateOtp(trimmedMobile, request.getCitizenType(), false);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "OTP sent successfully for citizen login");
        response.put("otpCode", otpCode); // TODO: Remove this when SMS API is integrated - OTP should not be in response
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");
        
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully for citizen login", response));
    }

    /**
     * Send OTP for Lawyer Login
     * POST /api/auth/lawyer/send-otp
     */
    @Operation(
            summary = "Send OTP for Lawyer Login",
            description = "Send OTP to mobile number for lawyer login. Use this for LOGIN when lawyer account is already active and verified."
    )
    @PostMapping("/lawyer/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendLawyerLoginOtp(
            @Valid @RequestBody OtpRequestDTO request) {
        log.info("Lawyer login OTP request received for mobile: {}", maskMobile(request.getMobileNumber()));

        String otpCode = otpService.generateOtp(request.getMobileNumber().trim(), Citizen.CitizenType.LAWYER, false);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "OTP sent successfully for lawyer login");
        response.put("otpCode", otpCode);
        response.put("expiryMinutes", 5);
        response.put("note", "OTP is also logged to console. This is temporary until SMS API is integrated.");

        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully for lawyer login", response));
    }

    /**
     * Citizen Login (OTP-based)
     * POST /api/auth/citizen/otp-login
     */
    @Operation(
            summary = "Citizen OTP Login",
            description = "Login for citizens by verifying OTP code sent to mobile number. Returns JWT access token and refresh token."
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
    @PostMapping("/citizen/otp-login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> citizenOtpLogin(
            @Valid @RequestBody OtpVerificationDTO request) {
        log.info("Citizen OTP login request received for mobile: {}", maskMobile(request.getMobileNumber()));
        
        if (request.getCitizenType() == null) {
            request.setCitizenType(Citizen.CitizenType.CITIZEN);
        }

        AuthResponseDTO response = authService.loginWithOtp(request);
        
        return ResponseEntity.ok(ApiResponse.success("Citizen login successful", response));
    }

    /**
     * Lawyer Login (OTP-based)
     * POST /api/auth/lawyer/otp-login
     */
    @Operation(
            summary = "Lawyer OTP Login",
            description = "Login for lawyers by verifying OTP code sent to mobile number. Returns JWT access token and refresh token."
    )
    @PostMapping("/lawyer/otp-login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> lawyerOtpLogin(
            @Valid @RequestBody OtpVerificationDTO request) {
        log.info("Lawyer OTP login request received for mobile: {}", maskMobile(request.getMobileNumber()));

        AuthResponseDTO response = lawyerAuthService.loginWithOtp(request);

        return ResponseEntity.ok(ApiResponse.success("Lawyer login successful", response));
    }

    /**
     * Citizen Login (Password-based)
     * POST /api/auth/citizen/login
     */
    @Operation(
            summary = "Citizen Login",
            description = "Login for citizens with mobile number/email and password. Returns JWT access token and refresh token."
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
    @PostMapping("/citizen/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> citizenLogin(
            @Valid @RequestBody LoginRequestDTO request) {
        log.info("Citizen login request received for username: {}", maskUsername(request.getUsername()));
        
        if (request.getCitizenType() == null) {
            request.setCitizenType(Citizen.CitizenType.CITIZEN);
        }

        AuthResponseDTO response = authService.loginWithPassword(request);
        
        return ResponseEntity.ok(ApiResponse.success("Citizen login successful", response));
    }

    /**
     * Lawyer Login (Password-based)
     * POST /api/auth/lawyer/login
     */
    @Operation(
            summary = "Lawyer Login",
            description = "Login for lawyers with mobile number/email and password. Returns JWT access token and refresh token."
    )
    @PostMapping("/lawyer/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> lawyerLogin(
            @Valid @RequestBody LoginRequestDTO request) {
        log.info("Lawyer login request received for username: {}", maskUsername(request.getUsername()));

        AuthResponseDTO response = lawyerAuthService.loginWithPassword(request);

        return ResponseEntity.ok(ApiResponse.success("Lawyer login successful", response));
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
     * Verify Citizen Registration OTP
     * POST /api/auth/citizen/registration/verify-otp
     * 
     * This endpoint verifies the OTP sent during citizen registration.
     * Upon successful verification:
     * - Marks mobile number as verified
     * - Activates the citizen account (sets isActive = true)
     * - Marks the OTP as used
     */
    @Operation(
            summary = "Verify Registration OTP",
            description = "Verify mobile OTP sent during citizen registration. This endpoint activates the citizen account after successful OTP verification. The OTP must be valid (not expired, not used) and match the mobile number used during registration."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Mobile number verified successfully. Citizen account activated.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - missing or invalid mobile number/OTP format",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Citizen not found with the provided mobile number",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/citizen/registration/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyCitizenRegistrationOtp(
            @Valid @RequestBody RegistrationOtpVerificationDTO request) {
        log.info("Citizen registration OTP verification request for mobile: {}", maskMobile(request.getMobileNumber()));
        
        // Verify OTP and activate account
        citizenService.verifyMobileOtp(request.getMobileNumber().trim(), request.getOtp().trim());
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Mobile number verified successfully. Citizen account activated.");
        response.put("mobileNumber", request.getMobileNumber());
        response.put("status", "ACTIVE");
        response.put("isMobileVerified", true);
        
        log.info("Registration OTP verified successfully for mobile: {}", maskMobile(request.getMobileNumber()));
        
        return ResponseEntity.ok(ApiResponse.success("Mobile number verified successfully. Citizen account activated.", response));
    }

    /**
     * Verify Lawyer Registration OTP
     * POST /api/auth/lawyer/registration/verify-otp
     */
    @Operation(
            summary = "Verify Lawyer Registration OTP",
            description = "Verify mobile OTP sent during lawyer registration. This endpoint activates the lawyer account after successful OTP verification."
    )
    @PostMapping("/lawyer/registration/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyLawyerRegistrationOtp(
            @Valid @RequestBody RegistrationOtpVerificationDTO request) {
        log.info("Lawyer registration OTP verification request received for mobile: {}", maskMobile(request.getMobileNumber()));

        lawyerService.verifyMobileOtp(request.getMobileNumber(), request.getOtp());

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Mobile number verified successfully. Lawyer account activated.");
        response.put("mobileNumber", request.getMobileNumber());

        return ResponseEntity.ok(ApiResponse.success("Lawyer mobile number verified successfully", response));
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
