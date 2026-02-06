package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.SystemSettingsDTO;
import in.gov.manipur.rccms.dto.UpdateSystemSettingsDTO;
import in.gov.manipur.rccms.dto.WhatsNewDTO;
import in.gov.manipur.rccms.entity.WhatsNew;
import in.gov.manipur.rccms.service.SystemSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * System Settings Controller
 * Admin endpoints for managing system-wide settings (logo, header, footer, etc.)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system-settings")
@RequiredArgsConstructor
@Tag(name = "System Settings", description = "Manage system-wide settings like logo, header, footer, state name")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    /**
     * Get current system settings
     * Public endpoint - can be used by frontend to display settings
     * GET /api/admin/system-settings
     */
    @Operation(
            summary = "Get System Settings",
            description = "Retrieve current system settings (logo, header, footer, etc.). Public endpoint."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<SystemSettingsDTO>> getSystemSettings() {
        log.info("Getting system settings");
        SystemSettingsDTO settings = systemSettingsService.getSystemSettings();
        return ResponseEntity.ok(ApiResponse.success("System settings retrieved successfully", settings));
    }

    /**
     * Update system settings
     * Admin only - requires ADMIN authority
     * PUT /api/admin/system-settings
     */
    @Operation(
            summary = "Update System Settings",
            description = "Update system settings (logo, header, footer, etc.). Admin only. Partial updates supported - only provided fields will be updated."
    )
    @PutMapping
//    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingsDTO>> updateSystemSettings(
            @Valid @RequestBody UpdateSystemSettingsDTO dto) {
        log.info("Updating system settings. Request data: {}", dto);
        SystemSettingsDTO updated = systemSettingsService.updateSystemSettings(dto);
        log.info("System settings updated successfully");
        return ResponseEntity.ok(ApiResponse.success("System settings updated successfully", updated));
    }


    @PostMapping("/create/whats-new")
    public ResponseEntity<ApiResponse<List<WhatsNewDTO>>> saveWhatsNew(@Valid @RequestBody List<WhatsNewDTO> dto) {

        return ResponseEntity.ok(ApiResponse.success("WhatsNew created successfully", systemSettingsService.createWhatsNew(dto)));
    }

    @GetMapping("/fetch/whats-new-list")
    public ResponseEntity<ApiResponse<List<WhatsNewDTO>>> fetchWhatsNewList() {

        return ResponseEntity.ok(ApiResponse.success("WhatsNew list fetched successfully", systemSettingsService.fetchWhatsNewList()));
    }

    @PutMapping("/update/whats-new/{whatsNewId}/{itemId}")
    public ResponseEntity<ApiResponse<WhatsNewDTO>> updateWhatsNew(@PathVariable Long whatsNewId, @PathVariable Integer itemId, @Valid @RequestBody WhatsNewDTO dto) {

        return ResponseEntity.ok(ApiResponse.success("WhatsNew updated successfully", systemSettingsService.updateWhatsNew(whatsNewId, itemId, dto)));
    }

    @DeleteMapping("/delete/whats-new/{whatsNewId}")
    public ResponseEntity<ApiResponse<WhatsNewDTO>> deleteWhatsNew(@PathVariable Long whatsNewId,@RequestParam(required = false)Integer itemId) {

        return ResponseEntity.ok(ApiResponse.success("WhatsNew deleted successfully", systemSettingsService.deleteWhatsNew(whatsNewId,itemId)));
    }


}

