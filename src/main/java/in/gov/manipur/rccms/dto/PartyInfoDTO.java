package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for party information (used for displaying parties in attendance form)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyInfoDTO {
    /**
     * Party identifier (e.g., "petitioner", "respondent")
     */
    private String partyId;
    
    /**
     * Party name (e.g., "Petitioner Name", "Respondent Name")
     */
    private String partyName;
    
    /**
     * Party type: "PETITIONER", "RESPONDENT", or custom type
     */
    private String partyType;
    
    /**
     * Party label for display (e.g., "Petitioner", "Respondent")
     */
    private String partyLabel;
}
