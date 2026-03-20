package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.HearingRescheduleCaseDTO;
import in.gov.manipur.rccms.dto.HearingShiftExecuteRequestDTO;
import in.gov.manipur.rccms.dto.HearingShiftExecuteResponseDTO;
import in.gov.manipur.rccms.dto.HearingShiftPreviewRequestDTO;
import in.gov.manipur.rccms.dto.HearingShiftPreviewResponseDTO;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.entity.CaseHearingEvent;
import in.gov.manipur.rccms.entity.CaseModuleFormSubmission;
import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.repository.CaseHearingEventRepository;
import in.gov.manipur.rccms.repository.CaseModuleFormSubmissionRepository;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HearingShiftService {

    private final CaseRepository caseRepository;
    private final CaseHearingEventRepository caseHearingEventRepository;
    private final CaseModuleFormSubmissionRepository moduleFormSubmissionRepository;
    private final CourtRepository courtRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<HearingRescheduleCaseDTO> getCasesByHearingDate(LocalDate date, Long courtId) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        List<Case> cases = caseRepository.findActiveCasesByHearingDateAndOptionalCourt(date, courtId);
        return mapCasesToRescheduleDtos(cases, date);
    }

    @Transactional(readOnly = true)
    public HearingShiftPreviewResponseDTO previewShift(HearingShiftPreviewRequestDTO request) {
        validateShiftRequest(request.getFromDate(), request.getToDate(), request.getShiftAll(), request.getCaseIds());
        List<Case> eligible = caseRepository.findActiveCasesByHearingDateAndOptionalCourt(
                request.getFromDate(), request.getCourtId());
        List<Case> selected = filterSelectedCases(eligible, request.getShiftAll(), request.getCaseIds());
        List<HearingRescheduleCaseDTO> selectedDtos = mapCasesToRescheduleDtos(selected, request.getFromDate());

        return HearingShiftPreviewResponseDTO.builder()
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .shiftAll(Boolean.TRUE.equals(request.getShiftAll()))
                .totalEligibleCases(eligible.size())
                .totalSelectedCases(selected.size())
                .cases(selectedDtos)
                .build();
    }

    public HearingShiftExecuteResponseDTO executeShift(HearingShiftExecuteRequestDTO request, Long officerId) {
        validateShiftRequest(request.getFromDate(), request.getToDate(), request.getShiftAll(), request.getCaseIds());
        if (Objects.equals(request.getFromDate(), request.getToDate())) {
            throw new IllegalArgumentException("toDate must be different from fromDate");
        }

        List<Case> eligible = caseRepository.findActiveCasesByHearingDateAndOptionalCourt(
                request.getFromDate(), request.getCourtId());
        List<Case> selected = filterSelectedCases(eligible, request.getShiftAll(), request.getCaseIds());
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("No cases selected for shift");
        }

        String batchId = "SHIFT-" + UUID.randomUUID();
        List<Long> shiftedCaseIds = new ArrayList<>();

        for (Case caseEntity : selected) {
            shiftSingleCase(caseEntity, request.getToDate(), officerId, batchId, request.getReason(), request.getRemarks());
            shiftedCaseIds.add(caseEntity.getId());
        }

        return HearingShiftExecuteResponseDTO.builder()
                .batchId(batchId)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .shiftedCount(shiftedCaseIds.size())
                .shiftedCaseIds(shiftedCaseIds)
                .build();
    }

    private void shiftSingleCase(Case caseEntity, LocalDate toDate, Long officerId, String batchId, String reason, String remarks) {
        CaseHearingEvent current = caseHearingEventRepository.findByCaseIdAndIsCurrentTrue(caseEntity.getId())
                .orElseThrow(() -> new RuntimeException("Current hearing event not found for case: " + caseEntity.getId()));
        current.setIsCurrent(false);
        current.setStatus("SHIFTED_BULK");
        current.setBatchId(batchId);
        // Flush deactivation first so partial unique index (case_id where is_current=true) is not violated on next insert.
        caseHearingEventRepository.saveAndFlush(current);

        Long hearingSubmissionId = updateExistingHearingSubmissionAndReturnId(current, toDate, officerId, remarks);

        CaseHearingEvent newEvent = new CaseHearingEvent();
        newEvent.setCaseEntity(caseEntity);
        newEvent.setCaseId(caseEntity.getId());
        newEvent.setHearingSubmissionId(hearingSubmissionId);
        newEvent.setHearingNo((current.getHearingNo() == null ? 0 : current.getHearingNo()) + 1);
        newEvent.setHearingDate(toDate);
        newEvent.setStatus("SCHEDULED");
        newEvent.setIsCurrent(true);
        newEvent.setPreviousHearingEventId(current.getId());
        newEvent.setSource("BULK_SHIFT");
        newEvent.setBatchId(batchId);
        newEvent.setReason(reason);
        newEvent.setRemarks(remarks);
        newEvent.setActionByOfficerId(officerId);
        caseHearingEventRepository.save(newEvent);

        caseEntity.setHearingDate(toDate);
        caseEntity.setNextHearingDate(toDate);
        caseRepository.save(caseEntity);
    }

    private Long updateExistingHearingSubmissionAndReturnId(
            CaseHearingEvent previousEvent,
            LocalDate toDate,
            Long officerId,
            String remarks
    ) {
        Long hearingSubmissionId = previousEvent.getHearingSubmissionId();
        if (hearingSubmissionId == null) {
            return null;
        }

        CaseModuleFormSubmission hearingSubmission = moduleFormSubmissionRepository.findById(hearingSubmissionId)
                .orElseThrow(() -> new RuntimeException("Hearing submission not found: " + hearingSubmissionId));
        hearingSubmission.setFormData(buildShiftedHearingFormData(hearingSubmission.getFormData(), toDate));
        hearingSubmission.setHearingDateSnapshot(toDate);
        hearingSubmission.setSubmittedByOfficerId(officerId);
        if (remarks != null && !remarks.isBlank()) {
            hearingSubmission.setRemarks(remarks);
        }
        moduleFormSubmissionRepository.save(hearingSubmission);
        return hearingSubmissionId;
    }

    private String buildShiftedHearingFormData(String existingFormData, LocalDate toDate) {
        Map<String, Object> formData = new java.util.HashMap<>();
        if (existingFormData != null && !existingFormData.isBlank()) {
            try {
                Map<String, Object> prevMap = objectMapper.readValue(existingFormData,
                        new TypeReference<Map<String, Object>>() {});
                formData.putAll(prevMap);
            } catch (Exception ignored) {
                // Fall back to minimal hearing payload.
            }
        }
        formData.put("nextHearingDate", toDate.toString());
        formData.put("hearingDate", toDate.toString());
        formData.put("date", toDate.toString());
        formData.put("shiftedBySystem", true);
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize shifted hearing form data", e);
        }
    }

    private List<HearingRescheduleCaseDTO> mapCasesToRescheduleDtos(List<Case> cases, LocalDate fallbackDate) {
        if (cases.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> caseIds = cases.stream().map(Case::getId).toList();
        Map<Long, CaseHearingEvent> currentEvents = caseHearingEventRepository.findByCaseIdInAndIsCurrentTrue(caseIds)
                .stream()
                .collect(Collectors.toMap(CaseHearingEvent::getCaseId, Function.identity()));

        Set<Long> courtIds = cases.stream()
                .map(Case::getCourtId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> courtNames = courtRepository.findAllById(new ArrayList<>(courtIds)).stream()
                .collect(Collectors.toMap(Court::getId, Court::getCourtName));

        return cases.stream()
                .map(c -> {
                    CaseHearingEvent event = currentEvents.get(c.getId());
                    return HearingRescheduleCaseDTO.builder()
                            .caseId(c.getId())
                            .caseNumber(c.getCaseNumber())
                            .courtId(c.getCourtId())
                            .courtName(courtNames.get(c.getCourtId()))
                            .currentHearingDate(event != null ? event.getHearingDate() : fallbackDate)
                            .currentHearingNo(event != null ? event.getHearingNo() : null)
                            .build();
                })
                .toList();
    }

    private List<Case> filterSelectedCases(List<Case> eligibleCases, Boolean shiftAll, List<Long> caseIds) {
        if (Boolean.TRUE.equals(shiftAll)) {
            return eligibleCases;
        }
        if (caseIds == null || caseIds.isEmpty()) {
            throw new IllegalArgumentException("caseIds are required when shiftAll is false");
        }
        Set<Long> selectedIds = Set.copyOf(caseIds);
        return eligibleCases.stream()
                .filter(c -> selectedIds.contains(c.getId()))
                .toList();
    }

    private void validateShiftRequest(LocalDate fromDate, LocalDate toDate, Boolean shiftAll, List<Long> caseIds) {
        if (fromDate == null) {
            throw new IllegalArgumentException("fromDate is required");
        }
        if (toDate == null) {
            throw new IllegalArgumentException("toDate is required");
        }
        if (!Boolean.TRUE.equals(shiftAll) && (caseIds == null || caseIds.isEmpty())) {
            throw new IllegalArgumentException("caseIds are required when shiftAll is false");
        }
    }
}
