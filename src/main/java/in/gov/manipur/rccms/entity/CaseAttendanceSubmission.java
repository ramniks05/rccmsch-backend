package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Separate attendance submission entity (outside module form submissions).
 * Stores full attendance payload per case/date and supports attendance history.
 */
@Entity
@Table(name = "case_attendance_submissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_case_attendance_case_hearing_submission", columnNames = {"case_id", "hearing_submission_id"})
        },
        indexes = {
        @Index(name = "idx_case_attendance_case_id", columnList = "case_id"),
        @Index(name = "idx_case_attendance_hearing_submission_id", columnList = "hearing_submission_id"),
        @Index(name = "idx_case_attendance_date", columnList = "attendance_date"),
        @Index(name = "idx_case_attendance_submitted_at", columnList = "submitted_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseAttendanceSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, foreignKey = @ForeignKey(name = "fk_case_attendance_case"))
    private Case caseEntity;

    @Column(name = "case_id", insertable = false, updatable = false)
    private Long caseId;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Column(name = "hearing_submission_id")
    private Long hearingSubmissionId;

    @Column(name = "form_data", columnDefinition = "TEXT", nullable = false)
    private String formData;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "submitted_by_officer_id")
    private Long submittedByOfficerId;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (submittedAt == null) {
            submittedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

