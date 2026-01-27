package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Act Entity
 * Master table for legal acts that case types reference
 */
@Entity
@Table(name = "acts", uniqueConstraints = {
    @UniqueConstraint(columnNames = "act_code", name = "uk_act_code"),
    @UniqueConstraint(columnNames = {"act_name", "act_year"}, name = "uk_act_name_year")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Act {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "act_code", nullable = false, length = 50)
    private String actCode; // Unique code for the act

    @Column(name = "act_name", nullable = false, length = 300)
    private String actName; // e.g., "Manipur Land Revenue and Land Reforms Act"

    @Column(name = "act_year")
    private Integer actYear; // e.g., 1960

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sections", columnDefinition = "TEXT")
    private String sections; // JSON string for relevant sections

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
