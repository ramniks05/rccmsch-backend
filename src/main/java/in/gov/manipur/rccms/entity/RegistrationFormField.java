package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registration Form Field Entity
 * Defines dynamic registration form fields for Citizen/Lawyer
 */
@Entity
@Table(name = "registration_form_fields", indexes = {
    @Index(name = "idx_reg_form_type", columnList = "registration_type"),
    @Index(name = "idx_reg_form_active", columnList = "is_active"),
    @Index(name = "idx_reg_form_name", columnList = "field_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationFormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 20)
    private RegistrationType registrationType; // CITIZEN, LAWYER

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName; // e.g., "firstName", "mobileNumber"

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType; // TEXT, NUMBER, DATE, EMAIL, PHONE, PASSWORD, DROPDOWN

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules; // JSON string: {minLength, maxLength, pattern, min, max}

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "field_options", columnDefinition = "TEXT")
    private String fieldOptions; // JSON string for dropdown options

    @Column(name = "data_source", columnDefinition = "TEXT")
    private String dataSource; // JSON string: {type:"ADMIN_UNITS", level:"CIRCLE"}

    @Column(name = "placeholder", length = 200)
    private String placeholder;

    @Column(name = "help_text", length = 500)
    private String helpText;

    @Column(name = "field_group", length = 100)
    private String fieldGroup;

    @Column(name = "conditional_logic", columnDefinition = "TEXT")
    private String conditionalLogic; // JSON: {showIf:{field:"fieldName", value:"expected"}}

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

    public enum RegistrationType {
        CITIZEN, LAWYER
    }
}
