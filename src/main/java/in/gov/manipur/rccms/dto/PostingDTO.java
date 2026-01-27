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
    
    @NotNull(message = "Court ID is required")
    private Long courtId;
    
    private String courtName;
    private String courtCode;
    private String courtLevel;
    private String courtType;
    
    // Unit information (derived from court)
    private Long unitId;
    private String unitName;
    private String unitCode;
    private String unitLgdCode;
    
    @NotNull(message = "Role code is required")
    private String roleCode;
    
    private String roleName;
    
    @NotNull(message = "Officer ID is required")
    private Long officerId;
    
    private String officerName;
    private String mobileNo;
    
    private String postingUserid; // Generated: ROLE_CODE@COURT_CODE
    
    @NotNull(message = "From date is required")
    private LocalDate fromDate;
    
    private LocalDate toDate;
    
    private Boolean isCurrent;
}

