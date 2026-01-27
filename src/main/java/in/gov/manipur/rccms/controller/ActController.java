package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ActDTO;
import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CreateActDTO;
import in.gov.manipur.rccms.service.ActService;
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
 * Act Controller
 * Handles CRUD operations for Acts
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/acts")
@RequiredArgsConstructor
@Tag(name = "Acts", description = "Act master data CRUD operations")
public class ActController {

    private final ActService actService;

    @PostMapping
    @Operation(summary = "Create Act", description = "Create a new act. Code must be unique.")
    public ResponseEntity<ApiResponse<ActDTO>> createAct(@Valid @RequestBody CreateActDTO request) {
        log.info("Create act request received: {}", request.getActCode());
        ActDTO created = actService.createAct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Act created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Act by ID")
    public ResponseEntity<ApiResponse<ActDTO>> getActById(@PathVariable Long id) {
        ActDTO act = actService.getActById(id);
        return ResponseEntity.ok(ApiResponse.success("Act retrieved successfully", act));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get Act by Code")
    public ResponseEntity<ApiResponse<ActDTO>> getActByCode(@PathVariable String code) {
        ActDTO act = actService.getActByCode(code);
        return ResponseEntity.ok(ApiResponse.success("Act retrieved successfully", act));
    }

    @GetMapping
    @Operation(summary = "Get All Active Acts")
    public ResponseEntity<ApiResponse<List<ActDTO>>> getActiveActs() {
        List<ActDTO> acts = actService.getActiveActs();
        return ResponseEntity.ok(ApiResponse.success("Acts retrieved successfully", acts));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Act")
    public ResponseEntity<ApiResponse<ActDTO>> updateAct(
            @PathVariable Long id,
            @Valid @RequestBody CreateActDTO request) {
        ActDTO updated = actService.updateAct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Act updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Act", description = "Soft delete an act")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAct(@PathVariable Long id) {
        actService.deleteAct(id);
        Map<String, Object> response = Map.of("message", "Act deleted successfully", "id", id);
        return ResponseEntity.ok(ApiResponse.success("Act deleted successfully", response));
    }
}
