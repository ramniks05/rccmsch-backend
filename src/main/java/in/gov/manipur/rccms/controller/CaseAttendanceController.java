package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.service.CaseAttendanceService;
import in.gov.manipur.rccms.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Separate attendance APIs (outside module form submissions).
 * Attendance save also sets workflow flag ATTENDANCE_SUBMITTED=true.
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Case Attendance", description = "APIs for separate attendance submissions and history")
public class CaseAttendanceController {

    private final CaseAttendanceService attendanceService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Submit attendance", description = "Submit attendance in separate storage and set ATTENDANCE_SUBMITTED workflow flag")
    @PostMapping("/{caseId}/attendance/submit")
    public ResponseEntity<ApiResponse<CaseAttendanceSubmissionDTO>> submitAttendance(
            @PathVariable Long caseId,
            @Valid @RequestBody SubmitCaseAttendanceDTO dto,
            HttpServletRequest request) {
        Long officerId = currentUserService.getCurrentOfficerId(request);
        if (officerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found"));
        }
        CaseAttendanceSubmissionDTO saved = attendanceService.submitAttendance(caseId, officerId, dto);
        return ResponseEntity.ok(ApiResponse.success("Attendance submitted successfully", saved));
    }

    @Operation(summary = "Get latest attendance", description = "Get latest attendance submission for a case")
    @GetMapping("/{caseId}/attendance/latest")
    public ResponseEntity<ApiResponse<CaseAttendanceSubmissionDTO>> getLatestAttendance(@PathVariable Long caseId) {
        CaseAttendanceSubmissionDTO latest = attendanceService.getLatestAttendance(caseId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Latest attendance retrieved", latest));
    }

    @Operation(summary = "Get attendance history", description = "Get attendance history for a case")
    @GetMapping("/{caseId}/attendance/history")
    public ResponseEntity<ApiResponse<List<CaseAttendanceSubmissionDTO>>> getAttendanceHistory(@PathVariable Long caseId) {
        List<CaseAttendanceSubmissionDTO> history = attendanceService.getAttendanceHistory(caseId);
        return ResponseEntity.ok(ApiResponse.success("Attendance history retrieved", history));
    }
}

