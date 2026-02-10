package in.gov.manipur.rccms.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.gov.manipur.rccms.Constants.Constant.Constant;
import in.gov.manipur.rccms.Constants.Enums.EventType;
import in.gov.manipur.rccms.entity.CalenderEvent;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


@Data
@NoArgsConstructor
public class CalenderEventDTO {
    private Long eventId;
    private String title;
    private EventType eventType;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)  // derived from date, not accepted in request
    private String year;
    @JsonFormat(pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = LocalDateTimeDateOnlyDeserializer.class)
    private LocalDateTime date;
    private String description;
    private Boolean isActive;

    public CalenderEventDTO(CalenderEvent calenderEvent) {


        if (calenderEvent != null) {
            this.eventId = calenderEvent.getEventId();
            this.title = calenderEvent.getTitle() == null ? "NA" : calenderEvent.getTitle();
            this.eventType = calenderEvent.getEventType() == null ? eventType : calenderEvent.getEventType();
            this.year = calenderEvent.getYear() == null ? "NA" : calenderEvent.getYear();
            this.date = calenderEvent.getDate() == null ? null : calenderEvent.getDate();
//            this.date = calenderEvent.getDate() == null ? "NA" : calenderEvent.getDate().format(DateTimeFormatter.ofPattern(Constant.EVENT_DATE_FORMAT));
            this.description = calenderEvent.getDescription() == null ? "NA" : calenderEvent.getDescription();
            this.isActive = calenderEvent.getIsActive();

        }
    }

}
