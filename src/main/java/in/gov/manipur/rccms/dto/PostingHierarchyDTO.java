package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.AdminUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Unit Hierarchy (State → District → Sub-Division → Circle)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingHierarchyDTO {
    private Long unitId;
    private String unitCode;
    private String unitName;
    private AdminUnit.UnitLevel unitLevel;
    private Long lgdCode;
    
    // Hierarchy chain
    private PostingHierarchyDTO state;      // State level
    private PostingHierarchyDTO district;   // District level
    private PostingHierarchyDTO subDivision; // Sub-Division level
    private PostingHierarchyDTO circle;     // Circle level (current unit if circle)
}

