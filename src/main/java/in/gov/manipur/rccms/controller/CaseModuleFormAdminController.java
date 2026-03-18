package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.ModuleType;
import in.gov.manipur.rccms.service.CaseModuleFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin APIs for module form configuration (hearing, notice, ordersheet, judgement)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/module-forms")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class CaseModuleFormAdminController {

    private final CaseModuleFormService moduleFormService;

    @GetMapping("/module-types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getModuleTypes() {
        List<Map<String, String>> moduleTypes = java.util.Arrays.stream(ModuleType.values())
                .map(mt -> Map.of(
                        "code", mt.name(),
                        "name", mt.name().replace('_', ' ')))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Module types retrieved", moduleTypes));
    }

    @GetMapping("/case-natures/{caseNatureId}/modules/{moduleType}/fields")
    public ResponseEntity<ApiResponse<List<ModuleFormFieldDTO>>> getAllFields(
            @PathVariable Long caseNatureId,
            @PathVariable ModuleType moduleType,
            @RequestParam(value = "caseTypeId", required = false) Long caseTypeId) {
        List<ModuleFormFieldDTO> fields = moduleFormService.getAllFields(caseNatureId, caseTypeId, moduleType);
        return ResponseEntity.ok(ApiResponse.success("Module form fields retrieved", fields));
    }

    @PostMapping("/fields")
    public ResponseEntity<ApiResponse<ModuleFormFieldDTO>> createField(
            @Valid @RequestBody CreateModuleFormFieldDTO dto) {
        ModuleFormFieldDTO field = moduleFormService.createField(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Module form field created", field));
    }

    @PutMapping("/fields/{fieldId}")
    public ResponseEntity<ApiResponse<ModuleFormFieldDTO>> updateField(
            @PathVariable Long fieldId,
            @Valid @RequestBody UpdateModuleFormFieldDTO dto) {
        ModuleFormFieldDTO field = moduleFormService.updateField(fieldId, dto);
        return ResponseEntity.ok(ApiResponse.success("Module form field updated", field));
    }

    @DeleteMapping("/fields/{fieldId}")
    public ResponseEntity<ApiResponse<Void>> deleteField(@PathVariable Long fieldId) {
        moduleFormService.deleteField(fieldId);
        return ResponseEntity.ok(ApiResponse.success("Module form field deleted", null));
    }
}

