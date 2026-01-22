package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseNatureDTO;
import in.gov.manipur.rccms.dto.CreateCaseNatureDTO;
import in.gov.manipur.rccms.service.CaseNatureService;
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
 * Case Nature Controller
 * Handles CRUD operations for Case Natures
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/case-natures")
@RequiredArgsConstructor
@Tag(name = "Case Natures", description = "Case Nature master data CRUD operations")
public class CaseNatureController {

    private final CaseNatureService caseNatureService;

    @PostMapping
    @Operation(summary = "Create Case Nature", description = "Create a new case nature. Code must be unique for case type.")
    public ResponseEntity<ApiResponse<CaseNatureDTO>> createCaseNature(@Valid @RequestBody CreateCaseNatureDTO request) {
        log.info("Create case nature request received: {}", request.getNatureCode());
        CaseNatureDTO created = caseNatureService.createCaseNature(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case nature created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Case Nature by ID")
    public ResponseEntity<ApiResponse<CaseNatureDTO>> getCaseNatureById(@PathVariable Long id) {
        CaseNatureDTO caseNature = caseNatureService.getCaseNatureById(id);
        return ResponseEntity.ok(ApiResponse.success("Case nature retrieved successfully", caseNature));
    }

    @GetMapping
    @Operation(summary = "Get All Active Case Natures")
    public ResponseEntity<ApiResponse<List<CaseNatureDTO>>> getAllCaseNatures() {
        List<CaseNatureDTO> caseNatures = caseNatureService.getAllCaseNatures();
        return ResponseEntity.ok(ApiResponse.success("Case natures retrieved successfully", caseNatures));
    }

    @GetMapping("/case-type/{caseTypeId}")
    @Operation(summary = "Get Case Natures by Case Type")
    public ResponseEntity<ApiResponse<List<CaseNatureDTO>>> getCaseNaturesByCaseType(@PathVariable Long caseTypeId) {
        List<CaseNatureDTO> caseNatures = caseNatureService.getCaseNaturesByCaseType(caseTypeId);
        return ResponseEntity.ok(ApiResponse.success("Case natures retrieved successfully", caseNatures));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Case Nature")
    public ResponseEntity<ApiResponse<CaseNatureDTO>> updateCaseNature(
            @PathVariable Long id,
            @Valid @RequestBody CreateCaseNatureDTO request) {
        CaseNatureDTO updated = caseNatureService.updateCaseNature(id, request);
        return ResponseEntity.ok(ApiResponse.success("Case nature updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Case Nature", description = "Soft delete a case nature")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCaseNature(@PathVariable Long id) {
        caseNatureService.deleteCaseNature(id);
        Map<String, Object> response = Map.of("message", "Case nature deleted successfully", "id", id);
        return ResponseEntity.ok(ApiResponse.success("Case nature deleted successfully", response));
    }
}
