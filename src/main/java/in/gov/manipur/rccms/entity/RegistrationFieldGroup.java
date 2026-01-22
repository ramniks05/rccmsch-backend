package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registration Field Group Entity
 * Manages group options for registration fields (admin controlled)
 */
@Entity
@Table(name = "registration_field_groups", indexes = {
    @Index(name = "idx_reg_group_type", columnList = "registration_type"),
    @Index(name = "idx_reg_group_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationFieldGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 20)
    private RegistrationFormField.RegistrationType registrationType;

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode; // e.g., "basic", "location"

    @Column(name = "group_label", nullable = false, length = 100)
    private String groupLabel; // e.g., "Basic Info"

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
