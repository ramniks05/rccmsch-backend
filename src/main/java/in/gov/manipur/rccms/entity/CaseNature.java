package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Case Nature Entity (Previously CaseType)
 * Master table for case natures that users can choose from
 * Examples: MUTATION_GIFT_SALE, PARTITION, etc.
 */
@Entity
@Table(name = "case_natures", uniqueConstraints = {
    @UniqueConstraint(columnNames = "code", name = "uk_case_nature_code"),
    @UniqueConstraint(columnNames = "name", name = "uk_case_nature_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseNature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "act_id", foreignKey = @ForeignKey(name = "fk_case_nature_act"))
    private Act act;

    @Column(name = "act_id", insertable = false, updatable = false)
    private Long actId;

    @Column(name = "workflow_code", length = 50)
    private String workflowCode; // Links to workflow_definition.workflow_code

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
