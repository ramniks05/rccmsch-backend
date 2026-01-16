package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Workflow Transition Entity
 * Defines transitions between states in a workflow
 * Each transition represents an action that moves a case from one state to another
 */
@Entity
@Table(name = "workflow_transition", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workflow_id", "from_state_id", "to_state_id", "transition_code"}, 
                     name = "uk_workflow_transition")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transition_workflow"))
    private WorkflowDefinition workflow;

    @Column(name = "workflow_id", insertable = false, updatable = false)
    private Long workflowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_state_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transition_from_state"))
    private WorkflowState fromState;

    @Column(name = "from_state_id", insertable = false, updatable = false)
    private Long fromStateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_state_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transition_to_state"))
    private WorkflowState toState;

    @Column(name = "to_state_id", insertable = false, updatable = false)
    private Long toStateId;

    @Column(name = "transition_code", nullable = false, length = 50)
    private String transitionCode; // e.g., "SUBMIT", "APPROVE", "REJECT"

    @Column(name = "transition_name", nullable = false, length = 200)
    private String transitionName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "requires_comment", nullable = false)
    private Boolean requiresComment = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (requiresComment == null) {
            requiresComment = false;
        }
    }
}

