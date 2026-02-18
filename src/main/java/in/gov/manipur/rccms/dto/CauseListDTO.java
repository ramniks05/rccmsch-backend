package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.Constants.Constant.Constant;
import in.gov.manipur.rccms.Projection.CauseListProjection;
import lombok.Data;


import java.time.format.DateTimeFormatter;



@Data
public class CauseListDTO {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(Constant.HEARING_DATE_FORMAT);


    private String courtName;
    private String courtAddress;
    private Long totalCases;
    private String hearingDate;


    public CauseListDTO(CauseListProjection r) {
        this.courtName = r.getCourtName();
        this.courtAddress = r.getCourtAddress();
        this.totalCases = r.getTotalCases();
        this.hearingDate = r.getHearingDate().format(FORMATTER);
    }
}
