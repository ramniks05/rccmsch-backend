package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.Constants.Constant.Constant;
import in.gov.manipur.rccms.Projection.CalendarHearingProjection;
import in.gov.manipur.rccms.Projection.CauseListProjection;
import in.gov.manipur.rccms.Projection.OfficerCaseStatsProjection;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.repository.CaseWorkflowInstanceRepository;
import in.gov.manipur.rccms.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard service: case summary counts and hearing-by-date / hearing-by-court reports.
 * Used by public, admin, and officer dashboards.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private static final String HEARING_SCHEDULED_STATUS = "CASE_NUMBER_HEARING_DATE_GENERATED";
    private static final int NEXT_DAYS = 10;

    private final CaseRepository caseRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final CourtRepository courtRepository;

    /**
     * Case summary for dashboard: total, pending, disposed, hearing scheduled counts.
     */
    public CaseSummaryDTO getCaseSummary() {
        long totalCases = caseRepository.countByIsActiveTrue();
        long pendingCases = workflowInstanceRepository.countByCurrentState_IsFinalStateFalseAndCaseEntity_IsActiveTrue();
        long disposedCases = workflowInstanceRepository.countByCurrentState_IsFinalStateTrueAndCaseEntity_IsActiveTrue();
        long hearingScheduledCount = caseRepository.countByStatusAndIsActiveTrue(HEARING_SCHEDULED_STATUS);
        long totalCourts = courtRepository.count();
        return new CaseSummaryDTO(totalCases, pendingCases, disposedCases, hearingScheduledCount,totalCourts);
    }

    /**
     * Hearings on a given date: all courts with case numbers. When user selects a date.
     */
    public HearingsByDateResponseDTO getHearingsByDate(LocalDate date) {
        List<Case> cases = caseRepository.findByHearingDateAndIsActiveTrue(date);
        Map<Long, List<Case>> byCourt = cases.stream()
                .filter(c -> c.getCourtId() != null)
                .collect(Collectors.groupingBy(Case::getCourtId));
        List<CourtHearingSummaryDTO> courts = new ArrayList<>();
        for (Map.Entry<Long, List<Case>> e : byCourt.entrySet()) {
            Long courtId = e.getKey();
            List<Case> courtCases = e.getValue();
            String courtCode = null;
            String courtName = null;
            Optional<Court> courtOpt = courtRepository.findById(courtId);
            if (courtOpt.isPresent()) {
                courtCode = courtOpt.get().getCourtCode();
                courtName = courtOpt.get().getCourtName();
            }
            List<CourtHearingSummaryDTO.CaseNumberItemDTO> items = courtCases.stream()
                    .map(c -> new CourtHearingSummaryDTO.CaseNumberItemDTO(c.getId(), c.getCaseNumber()))
                    .toList();
            courts.add(CourtHearingSummaryDTO.builder()
                    .courtId(courtId)
                    .courtCode(courtCode != null ? courtCode : "")
                    .courtName(courtName != null ? courtName : "Court " + courtId)
                    .caseCount(items.size())
                    .cases(items)
                    .build());
        }
        courts.sort(Comparator.comparing(CourtHearingSummaryDTO::getCourtName));
        return HearingsByDateResponseDTO.builder()
                .date(date)
                .courts(courts)
                .build();
    }

    /**
     * For a selected court: next 10 days (from fromDate) with date-wise hearing count and case numbers.
     */
    public HearingsByCourtResponseDTO getHearingsByCourt(Long courtId, LocalDate fromDate) {
        if (courtId == null) {
            throw new IllegalArgumentException("courtId is required");
        }
        LocalDate endDate = fromDate.plusDays(NEXT_DAYS - 1);
        List<Case> cases = caseRepository.findByCourtIdAndHearingDateBetweenAndIsActiveTrue(courtId, fromDate, endDate);

        String courtCode = null;
        String courtName = null;
        Optional<Court> courtOpt = courtRepository.findById(courtId);
        if (courtOpt.isPresent()) {
            courtCode = courtOpt.get().getCourtCode();
            courtName = courtOpt.get().getCourtName();
        }

        Map<LocalDate, List<Case>> byDate = cases.stream().collect(Collectors.groupingBy(Case::getHearingDate));
        List<DayWiseHearingDTO> days = new ArrayList<>();
        for (int i = 0; i < NEXT_DAYS; i++) {
            LocalDate d = fromDate.plusDays(i);
            List<Case> dayCases = byDate.getOrDefault(d, List.of());
            List<String> caseNumbers = dayCases.stream().map(Case::getCaseNumber).toList();
            days.add(DayWiseHearingDTO.builder()
                    .date(d)
                    .caseCount(caseNumbers.size())
                    .caseNumbers(caseNumbers)
                    .build());
        }

        return HearingsByCourtResponseDTO.builder()
                .courtId(courtId)
                .courtCode(courtCode != null ? courtCode : "")
                .courtName(courtName != null ? courtName : "Court " + courtId)
                .fromDate(fromDate)
                .days(days)
                .build();
    }


    public List<CalendarHearingDTO> getMonthlyHearings(Integer year, Integer month, Long courtId) {

        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarHearingProjection> results = caseRepository
                .findMonthlyHearings(startDate, endDate, courtId);


        return results.stream()
                .map(CalendarHearingDTO::new)
                .toList();

    }

    public List<CauseListDTO> getCauseList(Long courtId) {

        List<CauseListProjection> results =
                caseRepository.getCauseList(courtId);

        return results.stream()
                .map(CauseListDTO::new)
                .toList();
    }

    public List<OfficerCaseStatsProjection> getOfficerCaseStatistics() {

        return caseRepository.getOfficerCaseStats();
    }

}
