package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CauseListDTO {

    private String courtName;
    private String courtAddress;
    private Long totalCases;
    private String hearingDate;
}
