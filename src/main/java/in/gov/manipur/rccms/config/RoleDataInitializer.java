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
import java.util.ArrayList;
import java.util.List;

/**
 * Role Master Data Initializer
 * Automatically inserts system-controlled roles when application starts
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
     * Initialize system-controlled roles
     */
    private void initializeRoles() {
        List<RoleMaster> rolesToInsert = new ArrayList<>();

        // SUPER_ADMIN - System administrator (State level)
        rolesToInsert.add(createRole(
            "SUPER_ADMIN",
            "Super Administrator",
            AdminUnit.UnitLevel.STATE,
            "System administrator with full access"
        ));

        // STATE_ADMIN - State Administrator (State level)
        rolesToInsert.add(createRole(
            "STATE_ADMIN",
            "State Administrator",
            AdminUnit.UnitLevel.STATE,
            "State-level administrator for Land Resource Department"
        ));

        // DISTRICT_OFFICER - District Officer (District level)
        rolesToInsert.add(createRole(
            "DISTRICT_OFFICER",
            "District Officer",
            AdminUnit.UnitLevel.DISTRICT,
            "Revenue Court / Revenue Tribunal Officer"
        ));

        // SUB_DIVISION_OFFICER - Sub-Division Officer (Sub-Division level)
        rolesToInsert.add(createRole(
            "SUB_DIVISION_OFFICER",
            "Sub-Division Officer",
            AdminUnit.UnitLevel.SUB_DIVISION,
            "Office of the SDO (Sub-Divisional Officer)"
        ));

        // CIRCLE_OFFICER - Circle Officer (Circle level)
        rolesToInsert.add(createRole(
            "CIRCLE_OFFICER",
            "Circle Officer",
            AdminUnit.UnitLevel.CIRCLE,
            "Office of the SDC (Sub-Divisional Circle)"
        ));

        // DEALING_ASSISTANT - Dealing Assistant (All levels)
        rolesToInsert.add(createRole(
            "DEALING_ASSISTANT",
            "Dealing Assistant",
            null, // Can be at any level
            "Dealing Assistant supporting officers at all levels"
        ));

        // Insert roles if they don't exist
        int insertedCount = 0;
        int skippedCount = 0;

        for (RoleMaster role : rolesToInsert) {
            if (!roleMasterRepository.existsByRoleCode(role.getRoleCode())) {
                roleMasterRepository.save(role);
                insertedCount++;
                log.info("Inserted role: {} - {}", role.getRoleCode(), role.getRoleName());
            } else {
                skippedCount++;
                log.debug("Role already exists, skipping: {}", role.getRoleCode());
            }
        }

        log.info("Role Master initialization completed: {} inserted, {} skipped (already exist)",
                insertedCount, skippedCount);
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

