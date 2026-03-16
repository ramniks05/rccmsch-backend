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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Officer roles API – roles from role_master for officer postings only.
 * Excludes CITIZEN, LAWYER, RESPONDENT, SUPER_ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/officer")
@RequiredArgsConstructor
@Tag(name = "Officer Roles", description = "Roles from role_master for officers (non-officer roles excluded)")
public class OfficerRoleController {

    private static final Set<String> NON_OFFICER_ROLE_CODES = Set.of("CITIZEN", "LAWYER", "RESPONDENT", "SUPER_ADMIN");

    private final RoleMasterRepository roleMasterRepository;

    /**
     * Get officer roles from role_master (excludes citizen, lawyer, respondent, super admin)
     * GET /api/admin/officer/roles
     */
    @Operation(summary = "Get Officer Roles", 
               description = "Get roles from role_master for officers only. Excludes citizen, lawyer, respondent, super admin.")
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleMasterDTO>>> getOfficerRoles() {
        log.info("Get officer roles from role_master (non-officer roles excluded)");
        List<RoleMaster> roles = roleMasterRepository.findByRoleCodeNotInOrderByRoleCodeAsc(NON_OFFICER_ROLE_CODES);
        List<RoleMasterDTO> dtos = roles.stream()
                .map(r -> new RoleMasterDTO(r.getId(), r.getRoleCode(), r.getRoleName(), r.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Officer roles retrieved successfully", dtos));
    }
}
