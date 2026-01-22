package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.RegistrationFormSchemaDTO;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import in.gov.manipur.rccms.service.RegistrationFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public Registration Form Controller
 * Provides schema for citizen/lawyer registration forms
 */
@Slf4j
@RestController
@RequestMapping("/api/public/registration-forms")
@RequiredArgsConstructor
@Tag(name = "Registration Forms (Public)", description = "Public APIs for registration form schemas")
public class RegistrationFormController {

    private final RegistrationFormService registrationFormService;

    /**
     * Get registration form schema by type
     * GET /api/public/registration-forms/{type}
     */
    @Operation(summary = "Get Registration Form Schema", description = "Get registration form schema for CITIZEN or LAWYER")
    @GetMapping("/{type}")
    public ResponseEntity<ApiResponse<RegistrationFormSchemaDTO>> getSchema(@PathVariable String type) {
        RegistrationFormField.RegistrationType regType = RegistrationFormField.RegistrationType.valueOf(type.toUpperCase());
        RegistrationFormSchemaDTO schema = registrationFormService.getSchema(regType);
        return ResponseEntity.ok(ApiResponse.success("Registration form schema retrieved successfully", schema));
    }
}
