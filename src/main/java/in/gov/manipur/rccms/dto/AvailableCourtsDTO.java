package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for available courts response
 * Used when user selects case type and system returns available courts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCourtsDTO {
    private CaseTypeDTO caseType;
    private List<CourtDTO> courts;
    private String message;
}
