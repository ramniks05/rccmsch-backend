package in.gov.manipur.rccms.dto;


import in.gov.manipur.rccms.Constants.Constant.Constant;
import in.gov.manipur.rccms.Constants.Enums.EventType;
import in.gov.manipur.rccms.entity.CalenderEvent;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;


@Data
@NoArgsConstructor
public class CalenderEventDTO {
    private Long eventId;
    private String title;
    private EventType eventType;
    private String financialYear;
    private String date;
    private String description;
    private Boolean isActive;

    public CalenderEventDTO(CalenderEvent calenderEvent) {


        if (calenderEvent != null) {
            this.eventId = calenderEvent.getEventId();
            this.title = calenderEvent.getTitle() == null ? "NA" : calenderEvent.getTitle();
            this.eventType = calenderEvent.getEventType() == null ? eventType : calenderEvent.getEventType();
            this.financialYear = calenderEvent.getTitle() == null ? String.valueOf(financialYear) : calenderEvent.getFinancialYear();
            this.date = calenderEvent.getDate() == null ? "NA" : calenderEvent.getDate().format(DateTimeFormatter.ofPattern(Constant.EVENT_DATE_FORMAT));
            this.description = calenderEvent.getDescription() == null ? "NA" : calenderEvent.getDescription();
            this.financialYear = calenderEvent.getFinancialYear();
            this.isActive = calenderEvent.getIsActive();

        }
    }

}
