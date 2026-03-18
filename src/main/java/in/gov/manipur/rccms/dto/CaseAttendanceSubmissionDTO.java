package in.gov.manipur.rccms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CaseAttendanceSubmissionDTO {
    private Long id;
    private Long caseId;
    private LocalDate attendanceDate;
    private Long hearingSubmissionId;
    private String formData;
    private String remarks;
    private Long submittedByOfficerId;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

