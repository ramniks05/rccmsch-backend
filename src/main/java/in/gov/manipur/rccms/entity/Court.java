package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Court Entity
 * Represents courts at different administrative levels
 * Supports multiple court types at the same level (e.g., DC_COURT and REVENUE_TRIBUNAL at District)
 */
@Entity
@Table(name = "courts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"unit_id", "court_type", "court_code"}, 
                     name = "uk_court_unit_type_code"),
    @UniqueConstraint(columnNames = "court_code", name = "uk_court_code")
}, indexes = {
    @Index(name = "idx_court_level", columnList = "court_level"),
    @Index(name = "idx_court_type", columnList = "court_type"),
    @Index(name = "idx_court_unit", columnList = "unit_id"),
    @Index(name = "idx_court_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "court_code", nullable = false, length = 50, unique = true)
    private String courtCode; // Unique code for the court

    @Column(name = "court_name", nullable = false, length = 200)
    private String courtName;

    @Enumerated(EnumType.STRING)
    @Column(name = "court_level", nullable = false, length = 20)
    private CourtLevel courtLevel; // CIRCLE, SUB_DIVISION, DISTRICT, STATE

    @Enumerated(EnumType.STRING)
    @Column(name = "court_type", nullable = false, length = 50)
    private CourtType courtType; // SDC_COURT, SDO_COURT, DC_COURT, REVENUE_TRIBUNAL, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_court_unit"))
    private AdminUnit unit;

    @Column(name = "unit_id", insertable = false, updatable = false)
    private Long unitId;

    @Column(name = "designation", length = 100)
    private String designation; // "SDC", "SDO", "DC", "Revenue Tribunal"

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "email", length = 100)
    private String email;

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
