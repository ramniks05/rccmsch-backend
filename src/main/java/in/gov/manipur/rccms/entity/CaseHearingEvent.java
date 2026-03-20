package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Normalized hearing timeline per case.
 * Each new hearing scheduling/rescheduling action appends one row.
 */
@Entity
@Table(name = "case_hearing_events", indexes = {
        @Index(name = "idx_case_hearing_event_case_id", columnList = "case_id"),
        @Index(name = "idx_case_hearing_event_hearing_date", columnList = "hearing_date"),
        @Index(name = "idx_case_hearing_event_case_current", columnList = "case_id,is_current")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseHearingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, foreignKey = @ForeignKey(name = "fk_case_hearing_event_case"))
    private Case caseEntity;

    @Column(name = "case_id", insertable = false, updatable = false)
    private Long caseId;

    @Column(name = "hearing_submission_id")
    private Long hearingSubmissionId;

    @Column(name = "hearing_no", nullable = false)
    private Integer hearingNo;

    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "previous_hearing_event_id")
    private Long previousHearingEventId;

    @Column(name = "source", nullable = false, length = 30)
    private String source = "HEARING_FORM";

    @Column(name = "batch_id", length = 80)
    private String batchId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "action_by_officer_id")
    private Long actionByOfficerId;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (actionAt == null) {
            actionAt = now;
        }
        if (isCurrent == null) {
            isCurrent = true;
        }
        if (source == null || source.isBlank()) {
            source = "HEARING_FORM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
