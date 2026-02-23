package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Role Master Data Initializer
 * Initializes only the default SUPER_ADMIN role required for system access.
 * All other roles should be configured via admin APIs for Chandigarh implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Initialize roles first (before admin units)
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleMasterRepository roleMasterRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing Role Master...");
        initializeRoles();
        log.info("Role Master initialization completed");
    }

    /**
     * Initialize only default SUPER_ADMIN role
     * Other roles should be configured via admin APIs
     */
    private void initializeRoles() {
        // Only initialize SUPER_ADMIN - default admin role required for system access
        if (!roleMasterRepository.existsByRoleCode("SUPER_ADMIN")) {
            RoleMaster superAdmin = createRole(
                "SUPER_ADMIN",
                "Super Administrator",
                AdminUnit.UnitLevel.STATE,
                "System administrator with full access - Default admin role"
            );
            roleMasterRepository.save(superAdmin);
            log.info("Inserted default admin role: SUPER_ADMIN - Super Administrator");
        } else {
            log.debug("SUPER_ADMIN role already exists, skipping");
        }
        
        log.info("Role Master initialization completed - Only SUPER_ADMIN initialized");
        log.info("Other roles should be configured via admin APIs for Chandigarh");
    }

    /**
     * Create a RoleMaster entity
     */
    private RoleMaster createRole(String code, String name, AdminUnit.UnitLevel unitLevel, String description) {
        RoleMaster role = new RoleMaster();
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setUnitLevel(unitLevel);
        role.setDescription(description);
        role.setCreatedAt(LocalDateTime.now());
        return role;
    }
}

