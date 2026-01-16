package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Case Workflow Instance Entity
 * Tracks the workflow state of each case
 * Links a case to its current workflow state and assigned personnel
 */
@Entity
@Table(name = "case_workflow_instance", uniqueConstraints = {
    @UniqueConstraint(columnNames = "case_id", name = "uk_case_workflow_instance")
}, indexes = {
    @Index(name = "idx_workflow_case", columnList = "case_id"),
    @Index(name = "idx_workflow_state", columnList = "current_state_id"),
    @Index(name = "idx_workflow_assigned", columnList = "assigned_to_officer_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseWorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_instance_case"))
    private Case caseEntity;

    @Column(name = "case_id", insertable = false, updatable = false)
    private Long caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false, foreignKey = @ForeignKey(name = "fk_instance_workflow"))
    private WorkflowDefinition workflow;

    @Column(name = "workflow_id", insertable = false, updatable = false)
    private Long workflowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_state_id", nullable = false, foreignKey = @ForeignKey(name = "fk_instance_state"))
    private WorkflowState currentState;

    @Column(name = "current_state_id", insertable = false, updatable = false)
    private Long currentStateId;

    @Column(name = "assigned_to_role", length = 50)
    private String assignedToRole; // Current role responsible

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_unit_id", foreignKey = @ForeignKey(name = "fk_instance_unit"))
    private AdminUnit assignedToUnit;

    @Column(name = "assigned_to_unit_id", insertable = false, updatable = false)
    private Long assignedToUnitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_officer_id", foreignKey = @ForeignKey(name = "fk_instance_officer"))
    private Officer assignedToOfficer;

    @Column(name = "assigned_to_officer_id", insertable = false, updatable = false)
    private Long assignedToOfficerId;

    @Column(name = "workflow_data", columnDefinition = "TEXT")
    private String workflowData; // JSON string for workflow-specific data

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

