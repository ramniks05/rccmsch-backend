package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.PostingAssignmentDTO;
import in.gov.manipur.rccms.dto.PostingDTO;
import in.gov.manipur.rccms.repository.CourtRepository;
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

/**
 * Posting Controller
 * Handles posting management and field officer search APIs
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/postings")
@RequiredArgsConstructor
@Tag(name = "Posting Management", description = "APIs for managing officer postings and searching field officers")
public class PostingController {

    private final PostingService postingService;
    private final CourtRepository courtRepository;

    /**
     * Assign person to post (court-based or unit-based)
     * POST /api/admin/postings
     */
    @Operation(summary = "Assign Person to Post", 
               description = "Assign an officer to either a court (court-based) or unit (unit-based posting)")
    @PostMapping
    public ResponseEntity<ApiResponse<PostingDTO>> assignPersonToPost(
            @Valid @RequestBody PostingAssignmentDTO dto) {
        log.info("Assign person to post request: {}", dto);
        PostingDTO posting = postingService.assignPersonToPost(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Person assigned to post successfully", posting));
    }

    /**
     * Get all active postings
     * GET /api/admin/postings/active
     */
    @Operation(summary = "Get Active Postings", 
               description = "Get all current (active) postings (court-based and unit-based)")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getActivePostings() {
        log.info("Get active postings request");
        List<PostingDTO> postings = postingService.getAllActivePostings();
        return ResponseEntity.ok(ApiResponse.success("Active postings retrieved successfully", postings));
    }

    /**
     * Get field officers (unit-based postings) available to a court
     * GET /api/admin/postings/field-officers/court/{courtId}?roleCode={roleCode}
     */
    @Operation(summary = "Get Field Officers for Court", 
               description = "Find field officers (unit-based postings) available to a court. Searches unit hierarchy to find officers in units under the court's jurisdiction.")
    @GetMapping("/field-officers/court/{courtId}")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getFieldOfficersForCourt(
            @PathVariable Long courtId,
            @RequestParam(required = false) String roleCode) {
        log.info("Get field officers for court: courtId={}, roleCode={}", courtId, roleCode);
        
        List<PostingDTO> fieldOfficers;
        if (roleCode != null && !roleCode.trim().isEmpty()) {
            // Get field officers by specific role
            fieldOfficers = postingService.getFieldOfficersForCourt(courtId, roleCode);
        } else {
            // Get all field officers (all roles) - get unit from court
            var court = courtRepository.findById(courtId)
                    .orElseThrow(() -> new RuntimeException("Court not found with ID: " + courtId));
            if (court.getUnit() == null) {
                throw new RuntimeException("Court does not have an associated unit");
            }
            fieldOfficers = postingService.getAllFieldOfficersBelowUnit(court.getUnit().getUnitId());
        }
        
        return ResponseEntity.ok(ApiResponse.success("Field officers retrieved successfully", fieldOfficers));
    }

    /**
     * Get all field officers below a unit (all roles)
     * GET /api/admin/postings/field-officers/unit/{unitId}
     */
    @Operation(summary = "Get All Field Officers Below Unit", 
               description = "Get all unit-based postings (field officers) in units below the given unit in hierarchy. Returns all roles (PATWARI, KANUNGO, etc.)")
    @GetMapping("/field-officers/unit/{unitId}")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getAllFieldOfficersBelowUnit(
            @PathVariable Long unitId) {
        log.info("Get all field officers below unit: unitId={}", unitId);
        List<PostingDTO> fieldOfficers = postingService.getAllFieldOfficersBelowUnit(unitId);
        return ResponseEntity.ok(ApiResponse.success("Field officers retrieved successfully", fieldOfficers));
    }

    /**
     * Get field officers by role (all units)
     * GET /api/admin/postings/field-officers/role/{roleCode}
     */
    @Operation(summary = "Get Field Officers by Role", 
               description = "Get all unit-based postings (field officers) for a specific role across all units")
    @GetMapping("/field-officers/role/{roleCode}")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getFieldOfficersByRole(
            @PathVariable String roleCode) {
        log.info("Get field officers by role: roleCode={}", roleCode);
        List<PostingDTO> fieldOfficers = postingService.getUnitBasedPostingsByRole(roleCode);
        return ResponseEntity.ok(ApiResponse.success("Field officers retrieved successfully", fieldOfficers));
    }

    /**
     * Get field officers by unit and role
     * GET /api/admin/postings/field-officers/unit/{unitId}/role/{roleCode}
     */
    @Operation(summary = "Get Field Officers by Unit and Role", 
               description = "Get unit-based postings for a specific unit and role")
    @GetMapping("/field-officers/unit/{unitId}/role/{roleCode}")
    public ResponseEntity<ApiResponse<List<PostingDTO>>> getFieldOfficersByUnitAndRole(
            @PathVariable Long unitId,
            @PathVariable String roleCode) {
        log.info("Get field officers by unit and role: unitId={}, roleCode={}", unitId, roleCode);
        List<PostingDTO> fieldOfficers = postingService.getUnitBasedPostingsByUnitAndRole(unitId, roleCode);
        return ResponseEntity.ok(ApiResponse.success("Field officers retrieved successfully", fieldOfficers));
    }
}
