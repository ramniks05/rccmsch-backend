package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.RoleMasterDTO;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Role Controller
 * Exposes APIs for listing system roles (role_master).
 * Used by admin UI screens such as workflow permission configuration.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Admin Roles", description = "Role master management")
public class AdminRoleController {

    private final RoleMasterRepository roleMasterRepository;

    /**
     * Get all roles ordered by role code.
     * GET /api/admin/roles
     */
    @GetMapping
    @Operation(summary = "Get All Roles", description = "Retrieve all roles from role_master ordered by roleCode")
    public ResponseEntity<ApiResponse<List<RoleMasterDTO>>> getAllRoles() {
        log.info("Get all roles request");

        List<RoleMaster> roles = roleMasterRepository.findAllByOrderByRoleCodeAsc();

        List<RoleMasterDTO> roleDTOs = roles.stream()
                .map(role -> new RoleMasterDTO(
                        role.getId(),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getUnitLevel(),
                        role.getDescription()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Roles retrieved successfully", roleDTOs)
        );
    }
}

