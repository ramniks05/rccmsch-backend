package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Posting (Officer DA History)
 * Supports both court-based and unit-based postings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingDTO {

    private Long id;
    
    /**
     * Court ID (for court-based postings)
     * NULL for unit-based postings
     */
    private Long courtId;
    
    private String courtName;
    private String courtCode;
    private String courtLevel;
    private String courtType;
    
    /**
     * Unit information
     * For court-based: derived from court
     * For unit-based: direct unit assignment
     */
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
    
    /**
     * UserID format:
     * - Court-based: ROLE_CODE@COURT_CODE
     * - Unit-based: ROLE_CODE@UNIT_LGD_CODE
     */
    private String postingUserid;
    
    /**
     * Posting type: "COURT_BASED" or "UNIT_BASED"
     */
    private String postingType;
    
    @NotNull(message = "From date is required")
    private LocalDate fromDate;
    
    private LocalDate toDate;
    
    private Boolean isCurrent;
}

