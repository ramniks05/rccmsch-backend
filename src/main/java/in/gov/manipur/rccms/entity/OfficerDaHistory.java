package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Officer DA History Entity (Posting History)
 * Core table that tracks assignments of persons to posts
 * Supports TWO types of postings:
 * 1. Court-based: Officer posted to a court (court_id NOT NULL)
 * 2. Unit-based: Field officer assigned to administrative unit (court_id NULL, unit_id NOT NULL)
 * 
 * UserID Format:
 * - Court-based: ROLE_CODE@COURT_CODE (e.g., TEHSILDAR@CHD_TEHSILDAR_COURT)
 * - Unit-based: ROLE_CODE@UNIT_LGD_CODE (e.g., PATWARI@400101)
 */
@Entity
@Table(name = "officer_da_history", 
       uniqueConstraints = {
           @UniqueConstraint(
               columnNames = {"court_id", "role_code", "is_current"},
               name = "uk_posting_court_role_current"
           ),
           @UniqueConstraint(
               columnNames = {"unit_id", "role_code", "is_current"},
               name = "uk_posting_unit_role_current"
           )
       },
       indexes = {
           @Index(name = "idx_court_role", columnList = "court_id,role_code"),
           @Index(name = "idx_unit_role", columnList = "unit_id,role_code"),
           @Index(name = "idx_officer_id", columnList = "officer_id"),
           @Index(name = "idx_userid", columnList = "userid"),
           @Index(name = "idx_is_current", columnList = "is_current"),
           @Index(name = "idx_posting_type", columnList = "court_id,unit_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficerDaHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = true, foreignKey = @ForeignKey(name = "fk_posting_court"))
    private Court court;

    @Column(name = "court_id", insertable = false, updatable = false)
    private Long courtId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = true, foreignKey = @ForeignKey(name = "fk_posting_unit"))
    private AdminUnit unit;

    @Column(name = "unit_id", insertable = false, updatable = false)
    private Long unitId;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode; // References role_master.role_code

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_posting_officer"))
    private Officer officer;

    @Column(name = "officer_id", insertable = false, updatable = false)
    private Long officerId;

    @Column(name = "userid", nullable = false, length = 100, unique = true)
    private String postingUserid; // Generated format: ROLE_CODE@COURT_CODE or ROLE_CODE@UNIT_LGD_CODE

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate; // NULL if current posting

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isCurrent == null) {
            isCurrent = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

