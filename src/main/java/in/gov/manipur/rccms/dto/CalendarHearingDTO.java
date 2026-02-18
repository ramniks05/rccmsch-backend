package in.gov.manipur.rccms.dto;

import lombok.Data;

@Data
public class CalendarHearingDTO {
    private int date;
    private Boolean isHearing;
    private String tooltip;
}

