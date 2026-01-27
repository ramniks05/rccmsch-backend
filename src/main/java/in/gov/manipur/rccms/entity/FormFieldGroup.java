package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Form Field Group Entity
 * Master table for form field groups (admin controlled)
 * Groups are scoped per CaseType - each case type can have its own set of groups
 */
@Entity
@Table(name = "form_field_groups", indexes = {
    @Index(name = "idx_form_group_case_type", columnList = "case_type_id"),
    @Index(name = "idx_form_group_active", columnList = "is_active"),
    @Index(name = "idx_form_group_code", columnList = "case_type_id,group_code")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"case_type_id", "group_code"}, name = "uk_form_group_case_type_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormFieldGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_form_group_case_type"))
    private CaseType caseType;

    @Column(name = "case_type_id", insertable = false, updatable = false)
    private Long caseTypeId;

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode; // e.g., "deed_details", "applicant_info"

    @Column(name = "group_label", nullable = false, length = 200)
    private String groupLabel; // e.g., "Deed Details", "Applicant Information"

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

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
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
