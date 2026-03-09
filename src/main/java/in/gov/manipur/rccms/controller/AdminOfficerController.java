package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.OfficerDTO;
import in.gov.manipur.rccms.service.OfficerService;
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
 * Admin API for Officer (government employee) management.
 * GET /api/admin/officers, GET /api/admin/officers/{id}, POST /api/admin/officers, etc.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/officers")
@RequiredArgsConstructor
@Tag(name = "Admin Officers", description = "Officer management (government employees)")
public class AdminOfficerController {

    private final OfficerService officerService;

    @GetMapping
    @Operation(summary = "Get all officers", description = "Returns all officers ordered by full name")
    public ResponseEntity<ApiResponse<List<OfficerDTO>>> getAllOfficers() {
        log.info("Get all officers request");
        List<OfficerDTO> officers = officerService.getAllOfficers();
        return ResponseEntity.ok(ApiResponse.success("Officers retrieved successfully", officers));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get officer by ID")
    public ResponseEntity<ApiResponse<OfficerDTO>> getOfficerById(@PathVariable Long id) {
        log.info("Get officer by id: {}", id);
        OfficerDTO officer = officerService.getOfficerById(id);
        return ResponseEntity.ok(ApiResponse.success("Officer retrieved successfully", officer));
    }

    @PostMapping
    @Operation(summary = "Create officer", description = "Create a new officer. Temporary password is auto-generated (Rccms@<last4MobileDigits>).")
    public ResponseEntity<ApiResponse<OfficerDTO>> createOfficer(@Valid @RequestBody OfficerDTO dto) {
        log.info("Create officer request: {}", dto.getFullName());
        OfficerDTO created = officerService.createOfficer(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Officer created successfully. Temporary password generated.", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update officer")
    public ResponseEntity<ApiResponse<OfficerDTO>> updateOfficer(
            @PathVariable Long id,
            @Valid @RequestBody OfficerDTO dto) {
        log.info("Update officer request: id={}", id);
        OfficerDTO updated = officerService.updateOfficer(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Officer updated successfully", updated));
    }
}
