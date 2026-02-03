package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.Constants.Enums.EventType;
import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CalenderEventDTO;
import in.gov.manipur.rccms.entity.CalenderEvent;
import in.gov.manipur.rccms.service.CalenderEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calender")
public class CalenderEventController {

    private final CalenderEventService calenderEventService;


    @PostMapping("/create/calender-event")
    public ResponseEntity<ApiResponse<List<CalenderEventDTO>>> saveCalenderEvent(@RequestBody CalenderEventDTO calenderEventDTO) {

        return ResponseEntity.ok(ApiResponse.success
                ("Created and fetched successfully", calenderEventService.createCalenderEvents(calenderEventDTO)));
    }

    @PostMapping("/update/calender-event/{eventId}")
    public ResponseEntity<ApiResponse<CalenderEventDTO>> updateCalenderEvent
            (@PathVariable Long eventId, @RequestBody CalenderEventDTO calenderEventDTO) {

        return ResponseEntity.ok(ApiResponse.success
                ("Updated and fetched successfully", calenderEventService.updateCalenderEvents(eventId, calenderEventDTO)));
    }

    @GetMapping("/fetch/event-types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> fetchEventTypes() {

        List<Map<String, String>> eventList = Arrays.stream(EventType.values()).
                map(e -> Map.of("value", e.name(), "label", e.label)).toList();

        return ResponseEntity.ok(ApiResponse.success("Event types fetched successfully", eventList));

    }

    //Soft Deletion
    @PutMapping("/deactivate/calender-event/{eventId}")
    public ResponseEntity<ApiResponse<CalenderEventDTO>> deactivateCalenderEvent(@PathVariable Long eventId) {

        return ResponseEntity.ok(ApiResponse.success("Event deactivated successfully", calenderEventService.deactivateCalenderEvent(eventId)));

    }

    @GetMapping("fetch/calender-event-list")
    public ResponseEntity<ApiResponse<List<CalenderEventDTO>>> fetchCalenderEventList() {

        return ResponseEntity.ok(ApiResponse.success("Event List Fetched Successfully", calenderEventService.fetchCalenderEventList()));

    }


}
