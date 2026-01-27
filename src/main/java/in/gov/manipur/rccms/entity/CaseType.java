package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Case Type Entity (Previously CaseNature)
 * Defines different types of cases (New File, Appeal, Revision, Review, etc.)
 * Links case natures to court levels and court types
 */
@Entity
@Table(name = "case_types", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"case_nature_id", "type_code"}, 
                     name = "uk_case_type_nature_code")
}, indexes = {
    @Index(name = "idx_case_type_nature", columnList = "case_nature_id"),
    @Index(name = "idx_case_type_level", columnList = "court_level"),
    @Index(name = "idx_case_type_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_nature_id", nullable = false, foreignKey = @ForeignKey(name = "fk_case_type_case_nature"))
    private CaseNature caseNature;

    @Column(name = "case_nature_id", insertable = false, updatable = false)
    private Long caseNatureId;

    @Column(name = "type_code", nullable = false, length = 50)
    private String typeCode; // "NEW_FILE", "FIRST_APPEAL", "SECOND_APPEAL", "REVISION", "REVIEW"

    @Column(name = "type_name", nullable = false, length = 200)
    private String typeName; // "New File", "First Appeal", "Revision"

    @Enumerated(EnumType.STRING)
    @Column(name = "court_level", nullable = false, length = 20)
    private CourtLevel courtLevel; // CIRCLE, SUB_DIVISION, DISTRICT, STATE

    @Column(name = "court_types", nullable = false, length = 200)
    private String courtTypes; // JSON array: ["DC_COURT", "REVENUE_TRIBUNAL"] or single value "SDC_COURT"

    // For appeals/revisions - which level can this type be filed from
    @Enumerated(EnumType.STRING)
    @Column(name = "from_level", length = 20)
    private CourtLevel fromLevel; // "CIRCLE", "SUB_DIVISION", "DISTRICT" - original order level

    @Column(name = "is_appeal", nullable = false)
    private Boolean isAppeal = false; // true for appeals

    @Column(name = "appeal_order", nullable = false)
    private Integer appealOrder = 0; // 1 for first appeal, 2 for second appeal, 0 for others

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "workflow_code", length = 50)
    private String workflowCode; // Links to workflow_definition.workflow_code

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
        if (isAppeal == null) {
            isAppeal = false;
        }
        if (appealOrder == null) {
            appealOrder = 0;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
