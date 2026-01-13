package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Administrative Unit Entity
 * Master table for hierarchical administrative units (State, District, Sub-Division, Circle)
 */
@Entity
@Table(name = "admin_unit", uniqueConstraints = {
    @UniqueConstraint(columnNames = "unit_code", name = "uk_admin_unit_code"),
    @UniqueConstraint(columnNames = "lgd_code", name = "uk_admin_unit_lgd_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "unit_code", nullable = false, length = 50, unique = true)
    private String unitCode;

    @Column(name = "unit_name", nullable = false, length = 200)
    private String unitName;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_level", nullable = false, length = 20)
    private UnitLevel unitLevel;

    @Column(name = "lgd_code", nullable = false, unique = true)
    private Long lgdCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id", foreignKey = @ForeignKey(name = "fk_admin_unit_parent"))
    private AdminUnit parentUnit;

    @Column(name = "parent_unit_id", insertable = false, updatable = false)
    private Long parentUnitId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Unit Level Enum
     */
    public enum UnitLevel {
        STATE,
        DISTRICT,
        SUB_DIVISION,
        CIRCLE
    }

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

