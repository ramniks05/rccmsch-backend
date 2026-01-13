package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Posting (Officer DA History)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingDTO {

    private Long id;
    
    @NotNull(message = "Unit ID is required")
    private Long unitId;
    
    private String unitName;
    private String unitLgdCode;
    
    @NotNull(message = "Role code is required")
    private String roleCode;
    
    private String roleName;
    
    @NotNull(message = "Officer ID is required")
    private Long officerId;
    
    private String officerName;
    private String mobileNo;
    
    private String postingUserid; // Generated: ROLE@LGD (UserID format)
    
    @NotNull(message = "From date is required")
    private LocalDate fromDate;
    
    private LocalDate toDate;
    
    private Boolean isCurrent;
}

