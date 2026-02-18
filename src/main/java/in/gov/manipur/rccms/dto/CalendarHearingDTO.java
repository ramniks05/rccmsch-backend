package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.Constants.Constant.Constant;
import in.gov.manipur.rccms.Projection.CalendarHearingProjection;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class CalendarHearingDTO {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(Constant.HEARING_DATE_FORMAT);

    private Integer date;
    private Boolean isHearing;
    private String tooltip;


    public CalendarHearingDTO(CalendarHearingProjection r) {

        this.date = r.getHearingDate().getDayOfMonth();
        this.tooltip = r.getCourtName() + "-" + r.getTotalCases();
        this.isHearing = true;

    }
}

