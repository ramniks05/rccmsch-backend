package in.gov.manipur.rccms.Projection;

import java.time.LocalDate;

public interface CalendarHearingProjection {


    LocalDate getHearingDate();
    String getCourtName();
    Long getTotalCases();
}
