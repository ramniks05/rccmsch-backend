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
        this.courtName = r.getCourtName() != null ? r.getCourtName() : "NA";
        this.courtAddress = r.getCourtAddress() != null ? r.getCourtAddress() : "NA";
        this.totalCases = r.getTotalCases() != null ? r.getTotalCases() : 0L;
        this.hearingDate = r.getHearingDate() != null ? r.getHearingDate().format(FORMATTER) : "NA";
    }
}
