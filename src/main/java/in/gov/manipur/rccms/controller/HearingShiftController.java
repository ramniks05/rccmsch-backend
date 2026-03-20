package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.HearingRescheduleCaseDTO;
import in.gov.manipur.rccms.dto.HearingShiftExecuteRequestDTO;
import in.gov.manipur.rccms.dto.HearingShiftExecuteResponseDTO;
import in.gov.manipur.rccms.dto.HearingShiftPreviewRequestDTO;
import in.gov.manipur.rccms.dto.HearingShiftPreviewResponseDTO;
import in.gov.manipur.rccms.service.CurrentUserService;
import in.gov.manipur.rccms.service.HearingShiftService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/hearing-shift")
@RequiredArgsConstructor
public class HearingShiftController {

    private final HearingShiftService hearingShiftService;
    private final CurrentUserService currentUserService;

    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<List<HearingRescheduleCaseDTO>>> getCasesForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long courtId) {
        return ResponseEntity.ok(ApiResponse.success("Cases fetched for hearing date",
                hearingShiftService.getCasesByHearingDate(date, courtId)));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<HearingShiftPreviewResponseDTO>> previewShift(
            @RequestBody HearingShiftPreviewRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Shift preview generated",
                hearingShiftService.previewShift(request)));
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<HearingShiftExecuteResponseDTO>> executeShift(
            @RequestBody HearingShiftExecuteRequestDTO request,
            HttpServletRequest servletRequest) {
        Long officerId = currentUserService.getCurrentOfficerId(servletRequest);
        if (officerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Hearing shift executed",
                hearingShiftService.executeShift(request, officerId)));
    }
}
