package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Workflow State Entity
 * Defines states/stages in a workflow
 * Each workflow can have multiple states
 */
@Entity
@Table(name = "workflow_state", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workflow_id", "state_code"}, name = "uk_workflow_state_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false, foreignKey = @ForeignKey(name = "fk_state_workflow"))
    private WorkflowDefinition workflow;

    @Column(name = "workflow_id", insertable = false, updatable = false)
    private Long workflowId;

    @Column(name = "state_code", nullable = false, length = 50)
    private String stateCode; // e.g., "DRAFT", "SUBMITTED", "APPROVED"

    @Column(name = "state_name", nullable = false, length = 200)
    private String stateName;

    @Column(name = "state_order", nullable = false)
    private Integer stateOrder; // Sequence order in workflow

    @Column(name = "is_initial_state", nullable = false)
    private Boolean isInitialState = false;

    @Column(name = "is_final_state", nullable = false)
    private Boolean isFinalState = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isInitialState == null) {
            isInitialState = false;
        }
        if (isFinalState == null) {
            isFinalState = false;
        }
    }
}

