package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Workflow History Entity
 * Audit trail of all workflow transitions
 * Records who did what, when, and why
 */
@Entity
@Table(name = "workflow_history", indexes = {
    @Index(name = "idx_history_instance", columnList = "instance_id"),
    @Index(name = "idx_history_case", columnList = "case_id"),
    @Index(name = "idx_history_officer", columnList = "performed_by_officer_id"),
    @Index(name = "idx_history_date", columnList = "performed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_history_instance"))
    private CaseWorkflowInstance instance;

    @Column(name = "instance_id", insertable = false, updatable = false)
    private Long instanceId;

    @Column(name = "case_id", nullable = false)
    private Long caseId; // Denormalized for easier querying

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_state_id", foreignKey = @ForeignKey(name = "fk_history_from_state"))
    private WorkflowState fromState;

    @Column(name = "from_state_id", insertable = false, updatable = false)
    private Long fromStateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_state_id", nullable = false, foreignKey = @ForeignKey(name = "fk_history_to_state"))
    private WorkflowState toState;

    @Column(name = "to_state_id", insertable = false, updatable = false)
    private Long toStateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_history_transition"))
    private WorkflowTransition transition;

    @Column(name = "transition_id", insertable = false, updatable = false)
    private Long transitionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_officer_id", foreignKey = @ForeignKey(name = "fk_history_officer"))
    private Officer performedByOfficer;

    @Column(name = "performed_by_officer_id", insertable = false, updatable = false)
    private Long performedByOfficerId;

    @Column(name = "performed_by_role", length = 50)
    private String performedByRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_at_unit_id", foreignKey = @ForeignKey(name = "fk_history_unit"))
    private AdminUnit performedAtUnit;

    @Column(name = "performed_at_unit_id", insertable = false, updatable = false)
    private Long performedAtUnitId;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional metadata

    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @PrePersist
    protected void onCreate() {
        performedAt = LocalDateTime.now();
    }
}

