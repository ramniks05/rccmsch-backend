package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores module form submission data (e.g., hearing details).
 */
@Entity
@Table(name = "case_module_form_submissions", indexes = {
        @Index(name = "idx_module_form_case", columnList = "case_id"),
        @Index(name = "idx_module_form_case_nature", columnList = "case_nature_id"),
        @Index(name = "idx_module_form_module_type", columnList = "module_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseModuleFormSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, foreignKey = @ForeignKey(name = "fk_module_form_case"))
    private Case caseEntity;

    @Column(name = "case_id", insertable = false, updatable = false)
    private Long caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_nature_id", nullable = false, foreignKey = @ForeignKey(name = "fk_module_form_case_nature"))
    private CaseNature caseNature;

    @Column(name = "case_nature_id", insertable = false, updatable = false)
    private Long caseNatureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", nullable = false, length = 30)
    private ModuleType moduleType;

    @Column(name = "form_data", columnDefinition = "TEXT")
    private String formData; // JSON string

    @Column(name = "submitted_by_officer_id")
    private Long submittedByOfficerId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}

