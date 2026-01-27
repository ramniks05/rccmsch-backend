package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import in.gov.manipur.rccms.service.AdminAuthService;
import in.gov.manipur.rccms.service.AdminUnitService;
import in.gov.manipur.rccms.service.CaseNatureService;
import in.gov.manipur.rccms.service.CaseTypeService;
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
    private final AdminUnitService adminUnitService;
    private final CaseNatureService caseNatureService;
    private final CaseTypeService caseTypeService;
    private final RoleMasterRepository roleMasterRepository;

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

    /**
     * Update officer
     * PUT /api/admin/officers/{id}
     */
    @Operation(summary = "Update Officer", description = "Update an existing officer")
    @PutMapping("/officers/{id}")
    public ResponseEntity<ApiResponse<OfficerDTO>> updateOfficer(
            @PathVariable Long id,
            @Valid @RequestBody OfficerDTO request) {
        log.info("Update officer request for ID: {}", id);
        
        OfficerDTO updated = officerService.updateOfficer(id, request);
        
        return ResponseEntity.ok(ApiResponse.success("Officer updated successfully", updated));
    }

    /**
     * Deactivate/Activate officer
     * PATCH /api/admin/officers/{id}/status
     */
    @Operation(summary = "Update Officer Status", description = "Activate or deactivate an officer")
    @PatchMapping("/officers/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOfficerStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean isActive = request.get("isActive");
        if (isActive == null) {
            throw new IllegalArgumentException("isActive field is required");
        }
        
        log.info("Update officer status request for ID: {}, isActive: {}", id, isActive);
        
        OfficerDTO officer = officerService.getOfficerById(id);
        officer.setIsActive(isActive);
        OfficerDTO updated = officerService.updateOfficer(id, officer);
        
        Map<String, Object> response = Map.of(
                "id", updated.getId(),
                "isActive", updated.getIsActive(),
                "message", isActive ? "Officer activated successfully" : "Officer deactivated successfully"
        );
        
        return ResponseEntity.ok(ApiResponse.success(
                isActive ? "Officer activated successfully" : "Officer deactivated successfully", 
                response));
    }

    /**
     * Get active officers
     * GET /api/admin/officers/active
     */
    @Operation(summary = "Get Active Officers", description = "Retrieve all active officers")
    @GetMapping("/officers/active")
    public ResponseEntity<ApiResponse<List<OfficerDTO>>> getActiveOfficers() {
        List<OfficerDTO> officers = officerService.getActiveOfficers();
        return ResponseEntity.ok(ApiResponse.success("Active officers retrieved successfully", officers));
    }

    // ==================== Posting Management ====================

    /**
     * Assign a person to a post
     * POST /api/admin/postings
     */
    @Operation(
            summary = "Assign Person to Post",
            description = "Assign a person to a post (COURT + ROLE). Existing active posting for same court+role will be closed."
    )
    @PostMapping("/postings")
    public ResponseEntity<ApiResponse<PostingDTO>> assignPersonToPost(
            @Valid @RequestBody PostingAssignmentDTO request) {
        log.info("Assign officer to post request - Court: {}, Role: {}, Officer: {}", 
                request.getCourtId(), request.getRoleCode(), request.getOfficerId());
        
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
        log.info("Transfer person request - Court: {}, Role: {}, User: {}", 
                request.getCourtId(), request.getRoleCode(), request.getOfficerId());
        
        PostingDTO posting = postingService.transferPerson(request);
        
        return ResponseEntity.ok(ApiResponse.success("Person transferred successfully", posting));
    }

    /**
     * Get active posting by UserID
     * GET /api/admin/postings/userid/{userid}
     */
    @Operation(summary = "Get Posting by UserID", description = "Retrieve active posting by UserID (ROLE_CODE@COURT_CODE format)")
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
     * Get all postings by court
     * GET /api/admin/postings/court/{courtId}
     */
    @Operation(summary = "Get Postings by Court", description = "Retrieve all postings (history) for a court")
    @GetMapping("/postings/court/{courtId}")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getPostingsByCourt(@PathVariable Long courtId) {
        List<PostingDTO> postings = postingService.getPostingsByCourt(courtId);
        return ResponseEntity.ok(ApiResponse.success("Postings retrieved successfully", postings));
    }

    /**
     * Get all active postings by unit (through court)
     * GET /api/admin/postings/unit/{unitId}/active
     */
    @Operation(summary = "Get Active Postings by Unit", description = "Retrieve all active postings for a unit (through courts)")
    @GetMapping("/postings/unit/{unitId}/active")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getActivePostingsByUnit(@PathVariable Long unitId) {
        List<PostingDTO> postings = postingService.getActivePostingsByUnit(unitId);
        return ResponseEntity.ok(ApiResponse.success("Active postings retrieved successfully", postings));
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

    // ==================== Administrative Units Management ====================

    /**
     * Create administrative unit
     * POST /api/admin/admin-units
     */
    @Operation(summary = "Create Administrative Unit", description = "Create a new administrative unit")
    @PostMapping("/admin-units")
    public ResponseEntity<ApiResponse<AdminUnitDTO>> createAdminUnit(
            @Valid @RequestBody AdminUnitDTO request) {
        log.info("Create admin unit request: {}", request.getUnitCode());
        
        AdminUnitDTO created = adminUnitService.createAdminUnit(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admin unit created successfully", created));
    }

    /**
     * Get all administrative units
     * GET /api/admin/admin-units
     */
    @Operation(summary = "Get All Administrative Units", description = "Retrieve all administrative units")
    @GetMapping("/admin-units")
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getAllAdminUnits() {
        List<AdminUnitDTO> adminUnits = adminUnitService.getAllAdminUnits();
        return ResponseEntity.ok(ApiResponse.success("Admin units retrieved successfully", adminUnits));
    }

    /**
     * Get administrative unit by ID
     * GET /api/admin/admin-units/{id}
     */
    @Operation(summary = "Get Administrative Unit by ID", description = "Retrieve an administrative unit by ID")
    @GetMapping("/admin-units/{id}")
    public ResponseEntity<ApiResponse<AdminUnitDTO>> getAdminUnitById(@PathVariable Long id) {
        AdminUnitDTO adminUnit = adminUnitService.getAdminUnitById(id);
        return ResponseEntity.ok(ApiResponse.success("Admin unit retrieved successfully", adminUnit));
    }

    /**
     * Update administrative unit
     * PUT /api/admin/admin-units/{id}
     */
    @Operation(summary = "Update Administrative Unit", description = "Update an existing administrative unit")
    @PutMapping("/admin-units/{id}")
    public ResponseEntity<ApiResponse<AdminUnitDTO>> updateAdminUnit(
            @PathVariable Long id,
            @Valid @RequestBody AdminUnitDTO request) {
        log.info("Update admin unit request for ID: {}", id);
        
        AdminUnitDTO updated = adminUnitService.updateAdminUnit(id, request);
        
        return ResponseEntity.ok(ApiResponse.success("Admin unit updated successfully", updated));
    }

    /**
     * Delete administrative unit (soft delete)
     * DELETE /api/admin/admin-units/{id}
     */
    @Operation(summary = "Delete Administrative Unit", description = "Soft delete an administrative unit")
    @DeleteMapping("/admin-units/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAdminUnit(@PathVariable Long id) {
        log.info("Delete admin unit request for ID: {}", id);
        
        adminUnitService.deleteAdminUnit(id);
        
        Map<String, Object> response = Map.of(
                "message", "Admin unit deleted successfully",
                "id", id
        );
        
        return ResponseEntity.ok(ApiResponse.success("Admin unit deleted successfully", response));
    }

    /**
     * Get administrative units by level
     * GET /api/admin/admin-units/level/{level}
     */
    @Operation(summary = "Get Administrative Units by Level", description = "Retrieve units by level")
    @GetMapping("/admin-units/level/{level}")
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getAdminUnitsByLevel(
            @PathVariable in.gov.manipur.rccms.entity.AdminUnit.UnitLevel level) {
        List<AdminUnitDTO> adminUnits = adminUnitService.getAdminUnitsByLevel(level);
        return ResponseEntity.ok(ApiResponse.success("Admin units retrieved successfully", adminUnits));
    }

    /**
     * Get administrative units by parent
     * GET /api/admin/admin-units/parent/{parentId}
     */
    @Operation(summary = "Get Administrative Units by Parent", description = "Retrieve child units by parent ID")
    @GetMapping("/admin-units/parent/{parentId}")
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getAdminUnitsByParent(@PathVariable Long parentId) {
        List<AdminUnitDTO> adminUnits = adminUnitService.getAdminUnitsByParent(parentId);
        return ResponseEntity.ok(ApiResponse.success("Admin units retrieved successfully", adminUnits));
    }

    // ==================== Case Types Management ====================
    // Note: Case Type CRUD operations are handled by CaseTypeController
    // This section is kept for reference but methods are removed to avoid conflicts

    // ==================== Role Master Management ====================

    /**
     * Get all roles
     * GET /api/admin/roles
     */
    @Operation(summary = "Get All Roles", description = "Retrieve all system roles (read-only)")
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleMasterDTO>>> getAllRoles() {
        List<RoleMaster> roles = roleMasterRepository.findAllByOrderByRoleCodeAsc();
        List<RoleMasterDTO> roleDTOs = roles.stream()
                .map(role -> {
                    RoleMasterDTO dto = new RoleMasterDTO();
                    dto.setId(role.getId());
                    dto.setRoleCode(role.getRoleCode());
                    dto.setRoleName(role.getRoleName());
                    dto.setUnitLevel(role.getUnitLevel());
                    dto.setDescription(role.getDescription());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roleDTOs));
    }

    /**
     * Get role by ID
     * GET /api/admin/roles/{id}
     */
    @Operation(summary = "Get Role by ID", description = "Retrieve a role by ID")
    @GetMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<RoleMasterDTO>> getRoleById(@PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        RoleMaster role = roleMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + id));
        
        RoleMasterDTO dto = new RoleMasterDTO();
        dto.setId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setUnitLevel(role.getUnitLevel());
        dto.setDescription(role.getDescription());
        
        return ResponseEntity.ok(ApiResponse.success("Role retrieved successfully", dto));
    }

    /**
     * Get roles by unit level
     * GET /api/admin/roles/level/{level}
     */
    @Operation(summary = "Get Roles by Unit Level", description = "Retrieve roles by unit level")
    @GetMapping("/roles/level/{level}")
    public ResponseEntity<ApiResponse<List<RoleMasterDTO>>> getRolesByLevel(
            @PathVariable in.gov.manipur.rccms.entity.AdminUnit.UnitLevel level) {
        List<RoleMaster> roles = roleMasterRepository.findByUnitLevel(level);
        List<RoleMasterDTO> roleDTOs = roles.stream()
                .map(role -> {
                    RoleMasterDTO dto = new RoleMasterDTO();
                    dto.setId(role.getId());
                    dto.setRoleCode(role.getRoleCode());
                    dto.setRoleName(role.getRoleName());
                    dto.setUnitLevel(role.getUnitLevel());
                    dto.setDescription(role.getDescription());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roleDTOs));
    }

    // ==================== Dashboard / Statistics ====================

    /**
     * Get dashboard statistics
     * GET /api/admin/dashboard/stats
     */
    @Operation(summary = "Get Dashboard Statistics", description = "Retrieve dashboard statistics for admin")
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        long totalOfficers = officerService.getAllOfficers().size();
        long activeOfficers = officerService.getActiveOfficers().size();
        long totalPostings = postingService.getAllActivePostings().size();
        long totalAdminUnits = adminUnitService.getAllAdminUnits().size();
        long activeAdminUnits = adminUnitService.getActiveAdminUnits().size();
        long totalCaseTypes = caseTypeService.getAllCaseTypes().size();
        long totalCaseNatures = caseNatureService.getAllCaseNatures().size();
        long activeCaseNatures = caseNatureService.getActiveCaseNatures().size();
        long totalRoles = roleMasterRepository.count();
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalOfficers", totalOfficers);
        stats.put("activeOfficers", activeOfficers);
        stats.put("inactiveOfficers", totalOfficers - activeOfficers);
        stats.put("totalActivePostings", totalPostings);
        stats.put("totalAdminUnits", totalAdminUnits);
        stats.put("activeAdminUnits", activeAdminUnits);
        stats.put("totalCaseTypes", totalCaseTypes);
        stats.put("totalCaseNatures", totalCaseNatures);
        stats.put("activeCaseNatures", activeCaseNatures);
        stats.put("totalRoles", totalRoles);
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", stats));
    }
}

