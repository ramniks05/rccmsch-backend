package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Role Master Entity
 * System-controlled roles for RCCMS
 * Roles: SUPER_ADMIN, STATE_ADMIN, DISTRICT_OFFICER, SUB_DIVISION_OFFICER, CIRCLE_OFFICER, CIRCLE_MANDOL, DEALING_ASSISTANT
 */
@Entity
@Table(name = "role_master", uniqueConstraints = {
    @UniqueConstraint(columnNames = "role_code", name = "uk_role_master_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 50, unique = true)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_level", length = 20)
    private AdminUnit.UnitLevel unitLevel; // Which unit level this role belongs to (null for DEALING_ASSISTANT which can be at any level)

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Role Code Enum (for validation)
     */
    public enum RoleCode {
        SUPER_ADMIN,
        STATE_ADMIN,
        DISTRICT_OFFICER,
        SUB_DIVISION_OFFICER,
        CIRCLE_OFFICER,
        CIRCLE_MANDOL,
        DEALING_ASSISTANT
    }
}

