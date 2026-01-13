package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.service.AdminAuthService;
import in.gov.manipur.rccms.service.OfficerService;
import in.gov.manipur.rccms.service.PostBasedAuthService;
import in.gov.manipur.rccms.service.PostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller
 * Handles administrative operations for master data, users, and postings
 * Access restricted to SUPER_ADMIN and STATE_ADMIN roles
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations for master data, users, and postings")
public class AdminController {

    private final OfficerService officerService;
    private final PostingService postingService;
    private final PostBasedAuthService postBasedAuthService;
    private final AdminAuthService adminAuthService;

    // ==================== User Master (Person) Management ====================

    /**
     * Create a new officer (government employee)
     * POST /api/admin/officers
     */
    @Operation(
            summary = "Create Officer (Government Employee)",
            description = "Create a new government employee/officer. Temporary password will be auto-generated."
    )
    @PostMapping("/officers")
    public ResponseEntity<ApiResponse<OfficerDTO>> createOfficer(
            @Valid @RequestBody OfficerDTO request) {
        log.info("Create officer request received: {}", request.getFullName());
        
        OfficerDTO created = officerService.createOfficer(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Officer created successfully. Temporary password generated.", created));
    }

    /**
     * Get all officers
     * GET /api/admin/officers
     */
    @Operation(summary = "Get All Officers", description = "Retrieve all officers (government employees)")
    @GetMapping("/officers")
    public ResponseEntity<ApiResponse<List<OfficerDTO>>> getAllOfficers() {
        List<OfficerDTO> officers = officerService.getAllOfficers();
        return ResponseEntity.ok(ApiResponse.success("Officers retrieved successfully", officers));
    }

    /**
     * Get officer by ID
     * GET /api/admin/officers/{id}
     */
    @Operation(summary = "Get Officer by ID", description = "Retrieve an officer by ID")
    @GetMapping("/officers/{id}")
    public ResponseEntity<ApiResponse<OfficerDTO>> getOfficerById(@PathVariable Long id) {
        OfficerDTO officer = officerService.getOfficerById(id);
        return ResponseEntity.ok(ApiResponse.success("Officer retrieved successfully", officer));
    }

    // ==================== Posting Management ====================

    /**
     * Assign a person to a post
     * POST /api/admin/postings
     */
    @Operation(
            summary = "Assign Person to Post",
            description = "Assign a person to a post (UNIT + ROLE). Existing active posting for same unit+role will be closed."
    )
    @PostMapping("/postings")
    public ResponseEntity<ApiResponse<PostingDTO>> assignPersonToPost(
            @Valid @RequestBody PostingAssignmentDTO request) {
        log.info("Assign officer to post request - Unit: {}, Role: {}, Officer: {}", 
                request.getUnitId(), request.getRoleCode(), request.getOfficerId());
        
        PostingDTO posting = postingService.assignPersonToPost(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Person assigned to post successfully. UserID and temporary password generated.", posting));
    }

    /**
     * Transfer a person to a new post
     * PUT /api/admin/postings/transfer
     */
    @Operation(
            summary = "Transfer Person",
            description = "Transfer a person from current post to a new post. All active postings will be closed."
    )
    @PutMapping("/postings/transfer")
    public ResponseEntity<ApiResponse<PostingDTO>> transferPerson(
            @Valid @RequestBody PostingAssignmentDTO request) {
        log.info("Transfer person request - Unit: {}, Role: {}, User: {}", 
                request.getUnitId(), request.getRoleCode(), request.getOfficerId());
        
        PostingDTO posting = postingService.transferPerson(request);
        
        return ResponseEntity.ok(ApiResponse.success("Person transferred successfully", posting));
    }

    /**
     * Get active posting by UserID
     * GET /api/admin/postings/userid/{userid}
     */
    @Operation(summary = "Get Posting by UserID", description = "Retrieve active posting by UserID (ROLE@LGD format)")
    @GetMapping("/postings/userid/{userid}")
    public ResponseEntity<ApiResponse<PostingDTO>> getPostingByUserid(@PathVariable String userid) {
        PostingDTO posting = postingService.getActivePostingByUserid(userid);
        return ResponseEntity.ok(ApiResponse.success("Posting retrieved successfully", posting));
    }

    /**
     * Get all postings by user (person)
     * GET /api/admin/postings/officer/{officerId}
     */
    @Operation(summary = "Get Postings by Officer", description = "Retrieve all postings (history) for an officer")
    @GetMapping("/postings/officer/{officerId}")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getPostingsByOfficer(@PathVariable Long officerId) {
        List<PostingDTO> postings = postingService.getPostingsByOfficer(officerId);
        return ResponseEntity.ok(ApiResponse.success("Postings retrieved successfully", postings));
    }

    /**
     * Get all postings by unit
     * GET /api/admin/postings/unit/{unitId}
     */
    @Operation(summary = "Get Postings by Unit", description = "Retrieve all postings (history) for a unit")
    @GetMapping("/postings/unit/{unitId}")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getPostingsByUnit(@PathVariable Long unitId) {
        List<PostingDTO> postings = postingService.getPostingsByUnit(unitId);
        return ResponseEntity.ok(ApiResponse.success("Postings retrieved successfully", postings));
    }

    /**
     * Get all active postings
     * GET /api/admin/postings/active
     */
    @Operation(summary = "Get All Active Postings", description = "Retrieve all active postings")
    @GetMapping("/postings/active")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getAllActivePostings() {
        List<PostingDTO> postings = postingService.getAllActivePostings();
        return ResponseEntity.ok(ApiResponse.success("Active postings retrieved successfully", postings));
    }

    /**
     * Close a posting
     * PUT /api/admin/postings/{id}/close
     */
    @Operation(summary = "Close Posting", description = "Close an active posting")
    @PutMapping("/postings/{id}/close")
    public ResponseEntity<ApiResponse<Map<String, Object>>> closePosting(@PathVariable Long id) {
        postingService.closePosting(id);
        
        Map<String, Object> response = Map.of(
                "message", "Posting closed successfully",
                "id", id
        );
        
        return ResponseEntity.ok(ApiResponse.success("Posting closed successfully", response));
    }

    // ==================== Authentication ====================

    /**
     * Admin Login (Default Credentials)
     * POST /api/admin/auth/login
     */
    @Operation(
            summary = "Admin Login",
            description = "Login with default admin credentials. Default: username='admin', password='admin@123'"
    )
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> adminLogin(
            @Valid @RequestBody AdminLoginDTO request) {
        log.info("Admin login request for username: {}", request.getUsername());
        
        AuthResponseDTO response = adminAuthService.adminLogin(request);
        
        return ResponseEntity.ok(ApiResponse.success("Admin login successful", response));
    }

    /**
     * Officer/DA Login (Post-Based)
     * POST /api/admin/auth/officer-login
     */
    @Operation(
            summary = "Officer/DA Login",
            description = "Login for government officers and dealing assistants using UserID (ROLE@LGD format) and password"
    )
    @PostMapping("/auth/officer-login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> officerLogin(
            @Valid @RequestBody PostBasedLoginDTO request) {
        log.info("Officer/DA login request for UserID: {}", request.getUserid());
        
        AuthResponseDTO response = postBasedAuthService.loginWithPostBasedCredentials(request);
        
        return ResponseEntity.ok(ApiResponse.success("Officer login successful", response));
    }

    /**
     * Reset password (for first login)
     * POST /api/admin/auth/reset-password
     */
    @Operation(
            summary = "Reset Password",
            description = "Reset password for first login (mandatory after temporary password). Password must meet complexity requirements."
    )
    @PostMapping("/auth/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody PasswordResetDTO request) {
        // Validate password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }
        
        postBasedAuthService.resetPassword(request.getUserid(), request.getNewPassword());
        
        Map<String, Object> response = Map.of(
                "message", "Password reset successfully",
                "userid", request.getUserid()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", response));
    }

    /**
     * Verify mobile with OTP (for first login)
     * POST /api/admin/auth/verify-mobile
     */
    @Operation(
            summary = "Verify Mobile with OTP",
            description = "Verify mobile number with OTP (for first login profile update)"
    )
    @PostMapping("/auth/verify-mobile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMobile(
            @RequestBody Map<String, String> request) {
        String userid = request.get("userid");
        String otp = request.get("otp");
        
        if (userid == null || otp == null) {
            throw new IllegalArgumentException("UserID and OTP are required");
        }
        
        postBasedAuthService.verifyMobileWithOtp(userid, otp);
        
        Map<String, Object> response = Map.of(
                "message", "Mobile number verified successfully",
                "userid", userid
        );
        
        return ResponseEntity.ok(ApiResponse.success("Mobile number verified successfully", response));
    }
}

