package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for party attendance information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyAttendanceDTO {
    /**
     * Party identifier (e.g., "petitioner", "respondent", or custom party name)
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
     * Whether this party is present (checked)
     */
    private Boolean isPresent;
    
    /**
     * Whether this party is represented by proxy
     */
    private Boolean isProxy;
    
    /**
     * Proxy name (if isProxy = true)
     */
    private String proxyName;
    
    /**
     * Additional remarks for this party's attendance
     */
    private String remarks;
}
