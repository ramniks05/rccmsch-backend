package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseTypeDTO;
import in.gov.manipur.rccms.dto.CreateCaseTypeDTO;
import in.gov.manipur.rccms.service.CaseTypeService;
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
 * Case Type Controller (Previously CaseNatureController)
 * Handles CRUD operations for Case Types (NEW_FILE, APPEAL, REVISION, etc.)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/case-types")
@RequiredArgsConstructor
@Tag(name = "Case Types", description = "Case Type master data CRUD operations")
public class CaseTypeController {

    private final CaseTypeService caseTypeService;

    @PostMapping
    @Operation(summary = "Create Case Type", description = "Create a new case type. Code must be unique for case nature.")
    public ResponseEntity<ApiResponse<CaseTypeDTO>> createCaseType(@Valid @RequestBody CreateCaseTypeDTO request) {
        log.info("Create case type request received: {}", request.getTypeCode());
        CaseTypeDTO created = caseTypeService.createCaseType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case type created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Case Type by ID")
    public ResponseEntity<ApiResponse<CaseTypeDTO>> getCaseTypeById(@PathVariable Long id) {
        CaseTypeDTO caseType = caseTypeService.getCaseTypeById(id);
        return ResponseEntity.ok(ApiResponse.success("Case type retrieved successfully", caseType));
    }

    @GetMapping
    @Operation(summary = "Get All Active Case Types")
    public ResponseEntity<ApiResponse<List<CaseTypeDTO>>> getAllCaseTypes() {
        List<CaseTypeDTO> caseTypes = caseTypeService.getAllCaseTypes();
        return ResponseEntity.ok(ApiResponse.success("Case types retrieved successfully", caseTypes));
    }

    @GetMapping("/case-nature/{caseNatureId}")
    @Operation(summary = "Get Case Types by Case Nature")
    public ResponseEntity<ApiResponse<List<CaseTypeDTO>>> getCaseTypesByCaseNature(@PathVariable Long caseNatureId) {
        List<CaseTypeDTO> caseTypes = caseTypeService.getCaseTypesByCaseNature(caseNatureId);
        return ResponseEntity.ok(ApiResponse.success("Case types retrieved successfully", caseTypes));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Case Type")
    public ResponseEntity<ApiResponse<CaseTypeDTO>> updateCaseType(
            @PathVariable Long id,
            @Valid @RequestBody CreateCaseTypeDTO request) {
        CaseTypeDTO updated = caseTypeService.updateCaseType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Case type updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Case Type", description = "Soft delete a case type")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCaseType(@PathVariable Long id) {
        caseTypeService.deleteCaseType(id);
        Map<String, Object> response = Map.of("message", "Case type deleted successfully", "id", id);
        return ResponseEntity.ok(ApiResponse.success("Case type deleted successfully", response));
    }
}
