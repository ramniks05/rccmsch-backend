package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Posting Details (used in Auth Response)
 * Includes role, unit, hierarchy, and officer information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingDetailsDTO {
    // Posting Information
    private Long postingId;
    private String postingUserid; // ROLE@LGD format
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean isCurrent;
    
    // Role Information
    private String roleCode;
    private String roleName;
    
    // Court Information
    private Long courtId;
    private String courtCode;
    private String courtName;
    private String courtLevel;
    private String courtType;
    
    // Unit Information (derived from court)
    private Long unitId;
    private String unitCode;
    private String unitName;
    private Long unitLgdCode;
    
    // Full Hierarchy (State → District → Sub-Division → Circle)
    private PostingHierarchyDTO hierarchy;
    
    // Officer Information
    private Long officerId;
    private String officerName;
    private String officerEmail;
    private String officerMobile;
}

