package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CourtDTO;
import in.gov.manipur.rccms.dto.CreateCourtDTO;
import in.gov.manipur.rccms.entity.CourtLevel;
import in.gov.manipur.rccms.service.CourtService;
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
 * Court Controller
 * Handles CRUD operations for Courts
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/courts")
@RequiredArgsConstructor
@Tag(name = "Courts", description = "Court master data CRUD operations")
public class CourtController {

    private final CourtService courtService;

    @PostMapping
    @Operation(summary = "Create Court", description = "Create a new court. Code must be unique.")
    public ResponseEntity<ApiResponse<CourtDTO>> createCourt(@Valid @RequestBody CreateCourtDTO request) {
        log.info("Create court request received: {}", request.getCourtCode());
        CourtDTO created = courtService.createCourt(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Court created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Court by ID")
    public ResponseEntity<ApiResponse<CourtDTO>> getCourtById(@PathVariable Long id) {
        CourtDTO court = courtService.getCourtById(id);
        return ResponseEntity.ok(ApiResponse.success("Court retrieved successfully", court));
    }

    @GetMapping
    @Operation(summary = "Get All Active Courts")
    public ResponseEntity<ApiResponse<List<CourtDTO>>> getActiveCourts() {
        List<CourtDTO> courts = courtService.getActiveCourts();
        return ResponseEntity.ok(ApiResponse.success("Courts retrieved successfully", courts));
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "Get Courts by Level")
    public ResponseEntity<ApiResponse<List<CourtDTO>>> getCourtsByLevel(@PathVariable CourtLevel level) {
        List<CourtDTO> courts = courtService.getCourtsByLevel(level);
        return ResponseEntity.ok(ApiResponse.success("Courts retrieved successfully", courts));
    }

    @GetMapping("/unit/{unitId}")
    @Operation(summary = "Get Courts by Unit")
    public ResponseEntity<ApiResponse<List<CourtDTO>>> getCourtsByUnit(@PathVariable Long unitId) {
        List<CourtDTO> courts = courtService.getCourtsByUnit(unitId);
        return ResponseEntity.ok(ApiResponse.success("Courts retrieved successfully", courts));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Court")
    public ResponseEntity<ApiResponse<CourtDTO>> updateCourt(
            @PathVariable Long id,
            @Valid @RequestBody CreateCourtDTO request) {
        CourtDTO updated = courtService.updateCourt(id, request);
        return ResponseEntity.ok(ApiResponse.success("Court updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Court", description = "Soft delete a court")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCourt(@PathVariable Long id) {
        courtService.deleteCourt(id);
        Map<String, Object> response = Map.of("message", "Court deleted successfully", "id", id);
        return ResponseEntity.ok(ApiResponse.success("Court deleted successfully", response));
    }
}
