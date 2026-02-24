package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.AdminLoginDTO;
import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.AuthResponseDTO;
import in.gov.manipur.rccms.dto.PostBasedLoginDTO;
import in.gov.manipur.rccms.dto.ResetPasswordDTO;
import in.gov.manipur.rccms.service.AdminAuthService;
import in.gov.manipur.rccms.service.PostBasedAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin Authentication Controller
 * Handles admin and officer authentication endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "Admin and officer authentication endpoints")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final PostBasedAuthService postBasedAuthService;

    /**
     * Admin Login
     * POST /api/admin/auth/login
     */
    @Operation(summary = "Admin Login", description = "Login with default admin credentials")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> adminLogin(
            @Valid @RequestBody AdminLoginDTO request) {
        log.info("Admin login attempt for username: {}", request.getUsername());
        
        AuthResponseDTO response = adminAuthService.adminLogin(request);
        
        return ResponseEntity.ok(ApiResponse.success("Admin login successful", response));
    }

    /**
     * Officer/DA Login (Post-Based)
     * POST /api/admin/auth/officer-login
     */
    @Operation(summary = "Officer/DA Login", 
               description = "Login for government employees using UserID (ROLE_CODE@COURT_CODE or ROLE_CODE@UNIT_LGD_CODE) and password")
    @PostMapping("/officer-login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> officerLogin(
            @Valid @RequestBody PostBasedLoginDTO request) {
        log.info("Officer login attempt for UserID: {}", request.getUserid());
        
        AuthResponseDTO response = postBasedAuthService.loginWithPostBasedCredentials(request);
        
        return ResponseEntity.ok(ApiResponse.success("Officer login successful", response));
    }

    /**
     * Reset Password (First Login)
     * POST /api/admin/auth/reset-password
     */
    @Operation(summary = "Reset Password", 
               description = "Reset password for first login. Required when isPasswordResetRequired is true.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody ResetPasswordDTO request) {
        log.info("Password reset request for UserID: {}", request.getUserid());
        
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
        
        postBasedAuthService.resetPassword(request.getUserid(), request.getNewPassword());
        
        Map<String, Object> response = Map.of(
            "message", "Password reset successful. You can now login with your new password.",
            "userid", request.getUserid()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", response));
    }

    /**
     * Verify Mobile Number
     * POST /api/admin/auth/verify-mobile
     */
    @Operation(summary = "Verify Mobile Number", 
               description = "Verify mobile number with OTP for officer profile")
    @PostMapping("/verify-mobile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMobile(
            @RequestBody Map<String, String> request) {
        String userid = request.get("userid");
        String otp = request.get("otp");
        
        if (userid == null || otp == null) {
            throw new IllegalArgumentException("UserID and OTP are required");
        }
        
        log.info("Mobile verification request for UserID: {}", userid);
        
        postBasedAuthService.verifyMobileWithOtp(userid, otp);
        
        Map<String, Object> response = Map.of(
            "message", "Mobile number verified successfully",
            "userid", userid
        );
        
        return ResponseEntity.ok(ApiResponse.success("Mobile number verified successfully", response));
    }
}
