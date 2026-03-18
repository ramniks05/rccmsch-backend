package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.entity.CaseWorkflowInstance;
import in.gov.manipur.rccms.entity.ModuleType;
import in.gov.manipur.rccms.entity.WorkflowState;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.repository.CaseWorkflowInstanceRepository;
import in.gov.manipur.rccms.repository.WorkflowHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for "Actions required" dashboards (citizen and officer) and case detail on demand.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionsRequiredService {

    private static final String ACTION_RESUBMIT = "RESUBMIT_AFTER_CORRECTION";
    private static final String ACTION_ACK_NOTICE = "ACKNOWLEDGE_NOTICE";
    private static final String STATE_RETURNED_FOR_CORRECTION = "RETURNED_FOR_CORRECTION";
    private static final String NOTICE_ACCEPTED_METADATA = "NOTICE_ACCEPTED";

    private final CaseRepository caseRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowHistoryRepository historyRepository;
    private final WorkflowEngineService workflowEngineService;
    private final CaseService caseService;
    private final CaseDocumentService documentService;

    /**
     * Citizen dashboard: count and optional list of cases needing citizen action.
     * Actions: acknowledge notice (when notice sent and not yet accepted), resubmit after correction.
     */
    public CitizenActionsRequiredDTO getCitizenActionsRequired(Long applicantId, Integer limit) {
        if (applicantId == null) {
            return CitizenActionsRequiredDTO.builder().totalCount(0).items(List.of()).build();
        }
        List<Case> applicantCases = caseRepository.findActiveCasesByApplicant(applicantId);
        List<CitizenActionItemDTO> actionItems = new ArrayList<>();
        for (Case c : applicantCases) {
            Optional<CaseWorkflowInstance> opt = workflowInstanceRepository.findByCaseId(c.getId());
            if (opt.isEmpty()) continue;
            WorkflowState state = opt.get().getCurrentState();
            if (state == null) continue;
            String stateCode = state.getStateCode();
            if (stateCode == null) continue;

            if (STATE_RETURNED_FOR_CORRECTION.equals(stateCode)) {
                actionItems.add(CitizenActionItemDTO.builder()
                        .caseId(c.getId())
                        .caseNumber(c.getCaseNumber())
                        .subject(c.getSubject())
                        .actionCode(ACTION_RESUBMIT)
                        .actionLabel("Resubmit after correction")
                        .build());
                continue;
            }
            if (stateCode.startsWith("NOTICE_SENT") || "NOTICE_SENT_TO_PARTY".equals(stateCode)) {
                boolean alreadyAccepted = historyRepository.countByCaseIdAndMetadataContaining(c.getId(), NOTICE_ACCEPTED_METADATA) > 0;
                if (!alreadyAccepted) {
                    actionItems.add(CitizenActionItemDTO.builder()
                            .caseId(c.getId())
                            .caseNumber(c.getCaseNumber())
                            .subject(c.getSubject())
                            .actionCode(ACTION_ACK_NOTICE)
                            .actionLabel("Acknowledge notice")
                            .build());
                }
            }
        }
        long total = actionItems.size();
        List<CitizenActionItemDTO> items = limit != null && limit > 0
                ? actionItems.stream().limit(limit).toList()
                : actionItems;
        return CitizenActionsRequiredDTO.builder().totalCount(total).items(items).build();
    }

    /**
     * Officer dashboard: count and optional list of assigned cases that have at least one available transition.
     */
    public OfficerActionsRequiredDTO getOfficerActionsRequired(Long officerId, String roleCode, Long unitId, Integer limit) {
        if (officerId == null || roleCode == null || unitId == null) {
            return OfficerActionsRequiredDTO.builder().totalCount(0).items(List.of()).build();
        }
        List<CaseWorkflowInstance> assigned = workflowInstanceRepository.findByAssignedToOfficerId(officerId);
        List<OfficerActionItemDTO> actionItems = new ArrayList<>();
        for (CaseWorkflowInstance inst : assigned) {
            Case c = inst.getCaseEntity();
            if (c == null || !Boolean.TRUE.equals(c.getIsActive())) continue;
            List<WorkflowTransitionDTO> transitions = workflowEngineService.getAvailableTransitions(
                    c.getId(), officerId, null, roleCode, unitId);
            if (transitions.isEmpty()) continue;
            List<TransitionSummaryDTO> summaries = transitions.stream()
                    .map(t -> TransitionSummaryDTO.builder()
                            .code(t.getTransitionCode())
                            .label(t.getTransitionName() != null ? t.getTransitionName() : t.getTransitionCode())
                            .build())
                    .toList();
            WorkflowState state = inst.getCurrentState();
            actionItems.add(OfficerActionItemDTO.builder()
                    .caseId(c.getId())
                    .caseNumber(c.getCaseNumber())
                    .subject(c.getSubject())
                    .currentStateCode(state != null ? state.getStateCode() : null)
                    .currentStateName(state != null ? state.getStateName() : null)
                    .availableTransitions(summaries)
                    .build());
        }
        long total = actionItems.size();
        List<OfficerActionItemDTO> items = limit != null && limit > 0
                ? actionItems.stream().limit(limit).toList()
                : actionItems;
        return OfficerActionsRequiredDTO.builder().totalCount(total).items(items).build();
    }

    /**
     * Distinct action types (transition codes + labels) that exist in the officer's current caseload.
     * Used to build the "filter by action" dropdown.
     */
    public List<TransitionSummaryDTO> getOfficerCaseloadActionTypes(Long officerId, String roleCode, Long unitId) {
        if (officerId == null || roleCode == null || unitId == null) {
            return List.of();
        }
        List<CaseWorkflowInstance> assigned = workflowInstanceRepository.findByAssignedToOfficerId(officerId);
        Set<String> seenCodes = new HashSet<>();
        List<TransitionSummaryDTO> result = new ArrayList<>();
        for (CaseWorkflowInstance inst : assigned) {
            Case c = inst.getCaseEntity();
            if (c == null || !Boolean.TRUE.equals(c.getIsActive())) continue;
            List<WorkflowTransitionDTO> transitions = workflowEngineService.getAvailableTransitions(
                    c.getId(), officerId, null, roleCode, unitId);
            for (WorkflowTransitionDTO t : transitions) {
                if (t.getTransitionCode() != null && seenCodes.add(t.getTransitionCode())) {
                    result.add(TransitionSummaryDTO.builder()
                            .code(t.getTransitionCode())
                            .label(t.getTransitionName() != null ? t.getTransitionName() : t.getTransitionCode())
                            .build());
                }
            }
        }
        result.sort(Comparator.comparing(TransitionSummaryDTO::getLabel, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    /**
     * My cases filtered by transition code: only cases where the given transition is currently available.
     */
    public List<CaseDTO> getOfficerCasesFilteredByTransition(Long officerId, String roleCode, Long unitId, String transitionCode) {
        if (officerId == null) {
            return List.of();
        }
        List<CaseWorkflowInstance> assigned = workflowInstanceRepository.findByAssignedToOfficerId(officerId);
        if (transitionCode == null || transitionCode.isBlank()) {
            return assigned.stream()
                    .map(inst -> inst.getCaseEntity())
                    .filter(Objects::nonNull)
                    .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                    .map(c -> caseService.getCaseById(c.getId()))
                    .toList();
        }
        List<CaseDTO> filtered = new ArrayList<>();
        for (CaseWorkflowInstance inst : assigned) {
            Case c = inst.getCaseEntity();
            if (c == null || !Boolean.TRUE.equals(c.getIsActive())) continue;
            List<WorkflowTransitionDTO> transitions = workflowEngineService.getAvailableTransitions(
                    c.getId(), officerId, null, roleCode, unitId);
            boolean hasTransition = transitions.stream()
                    .anyMatch(t -> transitionCode.equals(t.getTransitionCode()));
            if (hasTransition) {
                filtered.add(caseService.getCaseById(c.getId()));
            }
        }
        return filtered;
    }

    /**
     * Full case detail on demand: case info, workflow history, and documents summary.
     * Only document-capable module types (NOTICE, ORDERSHEET, JUDGEMENT) are queried.
     * Other workflow modules like ATTENDANCE, REQUEST_FIELD_REPORT, or SUBMIT_FIELD_REPORT
     * may legitimately have no documents and are not treated as errors.
     */
    public CaseDetailDTO getCaseDetail(Long caseId) {
        CaseDTO caseInfo = caseService.getCaseById(caseId);
        List<WorkflowHistoryDTO> history = workflowEngineService.getWorkflowHistoryDTOs(caseId);
        List<CaseDocumentSummaryDTO> documents = new ArrayList<>();

        // Only query modules that support documents, to avoid invalid module type errors.
        List<ModuleType> documentModules = List.of(
                ModuleType.NOTICE,
                ModuleType.ORDERSHEET,
                ModuleType.JUDGEMENT
        );

        for (ModuleType mt : documentModules) {
            try {
                var doc = documentService.getLatestDocument(caseId, mt);
                if (doc != null) {
                    documents.add(CaseDocumentSummaryDTO.builder()
                            .documentId(doc.getId())
                            .moduleType(mt)
                            .moduleTypeLabel(mt.name().replace("_", " "))
                            .status(doc.getStatus())
                            .createdAt(doc.getCreatedAt())
                            .signedAt(doc.getSignedAt())
                            .hasContent(doc.getContentHtml() != null && !doc.getContentHtml().isEmpty())
                            .build());
                }
            } catch (Exception e) {
                log.debug("No document for case {} module {}: {}", caseId, mt, e.getMessage());
            }
        }
        return CaseDetailDTO.builder()
                .caseInfo(caseInfo)
                .history(history)
                .documents(documents)
                .build();
    }
}
