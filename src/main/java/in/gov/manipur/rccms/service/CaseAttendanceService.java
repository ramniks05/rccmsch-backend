package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.CaseAttendanceSubmissionDTO;
import in.gov.manipur.rccms.dto.SubmitCaseAttendanceDTO;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.entity.CaseAttendanceSubmission;
import in.gov.manipur.rccms.entity.CaseModuleFormSubmission;
import in.gov.manipur.rccms.repository.CaseAttendanceSubmissionRepository;
import in.gov.manipur.rccms.repository.CaseModuleFormSubmissionRepository;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.repository.CaseWorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseAttendanceService {

    private static final String ATTENDANCE_SUBMITTED_FLAG = "ATTENDANCE_SUBMITTED";

    private final CaseAttendanceSubmissionRepository attendanceRepository;
    private final CaseModuleFormSubmissionRepository moduleFormSubmissionRepository;
    private final CaseRepository caseRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final ObjectMapper objectMapper;

    public CaseAttendanceSubmissionDTO submitAttendance(Long caseId, Long officerId, SubmitCaseAttendanceDTO dto) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (officerId == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("SubmitCaseAttendanceDTO cannot be null");
        }
        if (dto.getFormData() == null || dto.getFormData().isBlank()) {
            throw new IllegalArgumentException("Form data cannot be empty");
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        Long hearingSubmissionId = java.util.Objects.requireNonNull(
                dto.getLatestHearingSubmissionId(),
                "latestHearingSubmissionId is required to submit attendance"
        );
        CaseModuleFormSubmission hearingSubmission = moduleFormSubmissionRepository.findById(hearingSubmissionId)
                .orElseThrow(() -> new RuntimeException("Hearing submission not found: " + hearingSubmissionId));
        if (!caseId.equals(hearingSubmission.getCaseId())) {
            throw new IllegalArgumentException("Hearing submission does not belong to case: " + caseId);
        }
        if (!"HEARING".equalsIgnoreCase(hearingSubmission.getModuleType())) {
            throw new IllegalArgumentException("Provided hearing submission id is not a HEARING module submission");
        }
        // Idempotent behavior: if attendance for same hearing already exists, update it.
        Optional<CaseAttendanceSubmission> existing = attendanceRepository
                .findByCaseIdAndHearingSubmissionId(caseId, hearingSubmissionId);

        CaseAttendanceSubmission attendance = existing.orElseGet(CaseAttendanceSubmission::new);
        attendance.setCaseEntity(caseEntity);
        attendance.setAttendanceDate(extractAttendanceDate(dto.getFormData()));
        attendance.setHearingSubmissionId(hearingSubmissionId);
        attendance.setFormData(dto.getFormData());
        attendance.setRemarks(dto.getRemarks());
        attendance.setSubmittedByOfficerId(officerId);

        CaseAttendanceSubmission saved = attendanceRepository.save(attendance);

        // Critical requirement: always set workflow flag after separate attendance save.
        updateWorkflowFlag(caseId, ATTENDANCE_SUBMITTED_FLAG, true);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<CaseAttendanceSubmissionDTO> getLatestAttendance(Long caseId) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        return attendanceRepository.findTopByCaseIdOrderBySubmittedAtDesc(caseId)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CaseAttendanceSubmissionDTO> getAttendanceHistory(Long caseId) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        return attendanceRepository.findByCaseIdOrderBySubmittedAtDesc(caseId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private LocalDate extractAttendanceDate(String formDataJson) {
        try {
            Map<String, Object> map = objectMapper.readValue(formDataJson, new TypeReference<Map<String, Object>>() {});
            Object dateValue = map.get("date");
            if (dateValue != null) {
                String dateStr = dateValue.toString().trim();
                if (!dateStr.isEmpty()) {
                    return LocalDate.parse(dateStr);
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract attendance date from formData JSON: {}", e.getMessage());
        }
        return null;
    }

    private void updateWorkflowFlag(Long caseId, String key, boolean value) {
        workflowInstanceRepository.findByCaseId(caseId).ifPresent(instance -> {
            Map<String, Object> data = parseJsonMap(instance.getWorkflowData());
            data.put(key, value);
            try {
                instance.setWorkflowData(objectMapper.writeValueAsString(data));
                workflowInstanceRepository.save(instance);
            } catch (Exception e) {
                log.error("Failed to update workflow data for case {}: {}", caseId, e.getMessage());
            }
        });
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Invalid workflow data JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private CaseAttendanceSubmissionDTO toDto(CaseAttendanceSubmission attendance) {
        CaseAttendanceSubmissionDTO dto = new CaseAttendanceSubmissionDTO();
        dto.setId(attendance.getId());
        dto.setCaseId(attendance.getCaseId());
        dto.setAttendanceDate(attendance.getAttendanceDate());
        dto.setHearingSubmissionId(attendance.getHearingSubmissionId());
        dto.setFormData(attendance.getFormData());
        dto.setRemarks(attendance.getRemarks());
        dto.setSubmittedByOfficerId(attendance.getSubmittedByOfficerId());
        dto.setSubmittedAt(attendance.getSubmittedAt());
        dto.setCreatedAt(attendance.getCreatedAt());
        dto.setUpdatedAt(attendance.getUpdatedAt());
        return dto;
    }
}

