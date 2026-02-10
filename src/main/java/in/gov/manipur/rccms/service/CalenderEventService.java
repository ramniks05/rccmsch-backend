package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.Constants.Constant.Constant;
import in.gov.manipur.rccms.dto.CalenderEventDTO;
import in.gov.manipur.rccms.entity.CalenderEvent;
import in.gov.manipur.rccms.repository.CalenderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalenderEventService {


    private final CalenderEventRepository calenderEventRepository;


    public List<CalenderEventDTO> createCalenderEvents(CalenderEventDTO calenderEventDTO) {

        CalenderEvent calenderEvent = new CalenderEvent();
//        LocalDate localDate = LocalDate.parse(
//                calenderEventDTO.getDate(),
//                DateTimeFormatter.ofPattern(Constant.EVENT_DATE_FORMAT)
//        );

        LocalDateTime localDateTime = calenderEventDTO.getDate();
        calenderEvent.setDate(localDateTime);
        calenderEvent.setTitle(calenderEventDTO.getTitle());
        calenderEvent.setDescription(calenderEventDTO.getDescription());
        calenderEvent.setEventType(calenderEventDTO.getEventType());
        String yearVal = String.valueOf(localDateTime.getYear());
        calenderEvent.setYear(yearVal);
        calenderEvent.setCreatedDate(LocalDateTime.now());
        calenderEvent.setUpdatedDate(LocalDateTime.now());

        calenderEventRepository.save(calenderEvent);

        List<CalenderEvent> calenderEventList = calenderEventRepository.findAll();

        return calenderEventList.stream().map(CalenderEventDTO::new).toList();


    }

    public CalenderEventDTO updateCalenderEvents(Long eventId, CalenderEventDTO calenderEventDTO) {

        CalenderEvent existingCalenderEventData = calenderEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with event Id :" + " " + eventId));

        existingCalenderEventData.setTitle(calenderEventDTO.getTitle());
        existingCalenderEventData.setEventType(calenderEventDTO.getEventType());
        existingCalenderEventData.setDescription(calenderEventDTO.getDescription());
        existingCalenderEventData.setUpdatedDate(LocalDateTime.now());

        CalenderEvent updatedCalenderEventData = calenderEventRepository.save(existingCalenderEventData);
        return new CalenderEventDTO(updatedCalenderEventData);


    }

    public CalenderEventDTO deactivateCalenderEvent(Long eventId) {

        CalenderEvent existingCalenderEventData = calenderEventRepository.findById(eventId).
                orElseThrow(() -> new RuntimeException("Event not found with event Id :" + " " + eventId));

        existingCalenderEventData.setIsActive(false);
        calenderEventRepository.save(existingCalenderEventData);
        return new CalenderEventDTO(existingCalenderEventData);

    }

    public List<CalenderEventDTO> fetchCalenderEventList() {
        List<CalenderEvent> eventList = calenderEventRepository.findAll();
        return eventList.stream().map(CalenderEventDTO::new).toList();
    }
}
