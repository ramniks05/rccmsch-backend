package in.gov.manipur.rccms.Projection;

import java.time.LocalDate;

public interface CauseListProjection {


    String getCourtName();
    String getCourtAddress();
    Long getTotalCases();
    LocalDate getHearingDate();
}
