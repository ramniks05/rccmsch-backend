package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Workflow Permission Entity
 * Defines which roles can perform which transitions
 * Supports hierarchy-based permissions (SAME_UNIT, PARENT_UNIT, etc.)
 */
@Entity
@Table(name = "workflow_permission", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"transition_id", "role_code", "unit_level"}, 
                     name = "uk_workflow_permission")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_permission_transition"))
    private WorkflowTransition transition;

    @Column(name = "transition_id", insertable = false, updatable = false)
    private Long transitionId;

    /** Role from role_master (acting role for permission check). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_permission_role"))
    private RoleMaster role;

    @Column(name = "role_id", insertable = false, updatable = false)
    private Long roleId;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode; // denormalized from role_master

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_level", length = 20)
    private AdminUnit.UnitLevel unitLevel; // NULL means all levels

    @Column(name = "can_initiate", nullable = false)
    private Boolean canInitiate = false; // Can start this transition

    @Column(name = "can_approve", nullable = false)
    private Boolean canApprove = false; // Can approve this transition (for multi-step approvals)

    @Column(name = "hierarchy_rule", length = 50)
    private String hierarchyRule; // "SAME_UNIT", "PARENT_UNIT", "ANY_UNIT", "SUPERVISOR"

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions; // JSON string for additional conditions

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (canInitiate == null) {
            canInitiate = false;
        }
        if (canApprove == null) {
            canApprove = false;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}

