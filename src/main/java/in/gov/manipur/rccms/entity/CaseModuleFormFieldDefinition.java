package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Form field definition for case modules (hearing, notice, ordersheet, judgement)
 * Fields are configured per case nature and module type.
 */
@Entity
@Table(name = "case_module_form_fields", indexes = {
        @Index(name = "idx_module_form_case_nature", columnList = "case_nature_id"),
        @Index(name = "idx_module_form_case_type", columnList = "case_type_id"),
        @Index(name = "idx_module_form_module_type", columnList = "module_type"),
        @Index(name = "idx_module_form_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseModuleFormFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_nature_id", nullable = false, foreignKey = @ForeignKey(name = "fk_module_form_case_nature"))
    private CaseNature caseNature;

    @Column(name = "case_nature_id", insertable = false, updatable = false)
    private Long caseNatureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", foreignKey = @ForeignKey(name = "fk_module_form_case_type"))
    private CaseType caseType;

    @Column(name = "case_type_id", insertable = false, updatable = false)
    private Long caseTypeId; // optional override per case type

    @Column(name = "module_type", nullable = false, length = 30)
    private String moduleType;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType; // TEXT, TEXTAREA, RICH_TEXT, NUMBER, DATE, DATETIME, SELECT, MULTISELECT, CHECKBOX, RADIO, FILE

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules; // JSON string

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "field_options", columnDefinition = "TEXT")
    private String fieldOptions; // JSON string for SELECT/RADIO

    @Column(name = "item_schema", columnDefinition = "TEXT")
    private String itemSchema; // JSON string for REPEATABLE_SECTION item fields

    @Column(name = "placeholder", length = 200)
    private String placeholder;

    @Column(name = "help_text", length = 500)
    private String helpText;

    @Column(name = "data_source", columnDefinition = "TEXT")
    private String dataSource; // JSON for dynamic dropdowns

    @Column(name = "depends_on_field", length = 100)
    private String dependsOnField;

    @Column(name = "dependency_condition", columnDefinition = "TEXT")
    private String dependencyCondition;

    @Column(name = "conditional_logic", columnDefinition = "TEXT")
    private String conditionalLogic;

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

