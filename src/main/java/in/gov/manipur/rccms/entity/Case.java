package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Case Entity
 * Represents a case filed in the RCCMS system
 * Each case is linked to a workflow instance
 */
@Entity
@Table(name = "cases", indexes = {
    @Index(name = "idx_case_case_number", columnList = "case_number"),
    @Index(name = "idx_case_type", columnList = "case_type_id"),
    @Index(name = "idx_case_nature", columnList = "case_nature_id"),
    @Index(name = "idx_case_applicant", columnList = "applicant_id"),
    @Index(name = "idx_case_unit", columnList = "unit_id"),
    @Index(name = "idx_case_status", columnList = "status"),
    @Index(name = "idx_case_court", columnList = "court_id"),
    @Index(name = "idx_case_hearing_date", columnList = "hearing_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber; // Auto-generated case number

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_case_case_type"))
    private CaseType caseType; // Case Type (NEW_FILE, APPEAL, etc.) - previously CaseNature

    @Column(name = "case_type_id", insertable = false, updatable = false)
    private Long caseTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_nature_id", nullable = false, foreignKey = @ForeignKey(name = "fk_case_case_nature"))
    private CaseNature caseNature; // Case Nature (MUTATION_GIFT_SALE, PARTITION, etc.) - previously CaseType

    @Column(name = "case_nature_id", insertable = false, updatable = false)
    private Long caseNatureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_case_applicant"))
    private Citizen applicant;

    @Column(name = "applicant_id", insertable = false, updatable = false)
    private Long applicantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_case_unit"))
    private AdminUnit unit;

    @Column(name = "unit_id", insertable = false, updatable = false)
    private Long unitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", foreignKey = @ForeignKey(name = "fk_case_court"))
    private Court court; // Court where petition is filed

    @Column(name = "court_id", insertable = false, updatable = false)
    private Long courtId;

    @Column(name = "original_order_level", length = 20)
    private String originalOrderLevel; // For appeals - level of original order (CIRCLE, SUB_DIVISION, DISTRICT)

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // Current workflow state code

    @Column(name = "priority", length = 20)
    private String priority; // "LOW", "MEDIUM", "HIGH", "URGENT"

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "hearing_date")
    private LocalDate hearingDate; // Set when hearing is scheduled (from HEARING form)

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "case_data", columnDefinition = "TEXT")
    private String caseData; // JSON string for case-specific data

    /** JSON array of {fieldName, fieldLabel, fieldGroup, groupLabel, value, displayOrder} for display. */
    @Column(name = "case_data_display", columnDefinition = "TEXT")
    private String caseDataDisplay;

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
        if (applicationDate == null) {
            applicationDate = LocalDate.now();
        }
        // Set default status if not already set (fallback)
        if (status == null || status.trim().isEmpty()) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

