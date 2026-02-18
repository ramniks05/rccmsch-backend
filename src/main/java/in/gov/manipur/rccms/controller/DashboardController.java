package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard APIs for case summary and hearing reports.
 * Accessible by public, admin, and officer (no auth required for dashboard view).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard APIs: case summary, hearings by date, hearings by court")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/case-summary")
    @Operation(summary = "Case summary", description = "Total, pending, disposed, and hearing-scheduled case counts for dashboard")
    public ResponseEntity<ApiResponse<CaseSummaryDTO>> getCaseSummary() {
        CaseSummaryDTO summary = dashboardService.getCaseSummary();
        return ResponseEntity.ok(ApiResponse.success("Case summary", summary));
    }

    @GetMapping("/hearings-by-date")
    @Operation(summary = "Hearings by date", description = "For a selected date, returns all courts with case numbers scheduled for that date")
    public ResponseEntity<ApiResponse<HearingsByDateResponseDTO>> getHearingsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        HearingsByDateResponseDTO data = dashboardService.getHearingsByDate(date);
        return ResponseEntity.ok(ApiResponse.success("Hearings by date", data));
    }

    @GetMapping("/hearings-by-court")
    @Operation(summary = "Hearings by court (next 10 days)", description = "For a selected court, returns date-wise hearing count and case numbers for the next 10 days from fromDate")
    public ResponseEntity<ApiResponse<HearingsByCourtResponseDTO>> getHearingsByCourt(
            @RequestParam Long courtId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        LocalDate start = fromDate != null ? fromDate : LocalDate.now();
        HearingsByCourtResponseDTO data = dashboardService.getHearingsByCourt(courtId, start);
        return ResponseEntity.ok(ApiResponse.success("Hearings by court", data));
    }

    @GetMapping("/hearings/calendar")
    @Operation(summary = "Monthly Hearing Dates with and without court filtering ", description = "All monthly active hearings with dates and tooltip ")

    public ResponseEntity<List<CalendarHearingDTO>> getCalendarHearings(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Long courtId) {

        return ResponseEntity.ok(
                dashboardService.getMonthlyHearings(year, month, courtId)
        );
    }


    @GetMapping("/cause-list/{courtId}")
    @Operation(summary = "Cause List", description = "Fetching cause list for dashboard ")
    public ResponseEntity<List<CauseListDTO>> getCauseList(
            @PathVariable(required = false) Long courtId) {

        return ResponseEntity.ok(
                dashboardService.getCauseList(courtId)
        );
    }


}
