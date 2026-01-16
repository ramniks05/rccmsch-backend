package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Form Field Definition Entity
 * Defines form fields dynamically for each case type
 * Allows admin to configure form structure without code changes
 */
@Entity
@Table(name = "form_field_definitions", indexes = {
    @Index(name = "idx_form_field_case_type", columnList = "case_type_id"),
    @Index(name = "idx_form_field_active", columnList = "is_active"),
    @Index(name = "idx_form_field_name", columnList = "field_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_form_field_case_type"))
    private CaseType caseType;

    @Column(name = "case_type_id", insertable = false, updatable = false)
    private Long caseTypeId;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName; // e.g., "registeredDeedNumber"

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel; // e.g., "Registered Deed Number"

    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType; // TEXT, NUMBER, DATE, DATETIME, EMAIL, PHONE, TEXTAREA, SELECT, RADIO, CHECKBOX, FILE

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules; // JSON string: {minLength, maxLength, min, max, pattern, minDate, maxDate, etc.}

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "field_options", columnDefinition = "TEXT")
    private String fieldOptions; // JSON string: [{value: "option1", label: "Option 1"}, ...] for SELECT/RADIO

    @Column(name = "placeholder", length = 200)
    private String placeholder;

    @Column(name = "help_text", length = 500)
    private String helpText;

    @Column(name = "field_group", length = 100)
    private String fieldGroup; // Group fields together (e.g., "deed_details", "applicant_info")

    @Column(name = "conditional_logic", columnDefinition = "TEXT")
    private String conditionalLogic; // JSON: {showIf: {field: "fieldName", value: "expectedValue"}}

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
        if (isRequired == null) {
            isRequired = false;
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

