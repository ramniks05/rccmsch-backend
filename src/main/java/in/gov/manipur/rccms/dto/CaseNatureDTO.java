package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.CourtLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for CaseNature entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseNatureDTO {
    private Long id;
    private Long caseTypeId;
    private String caseTypeName;
    private String caseTypeCode;
    private String natureCode;
    private String natureName;
    private CourtLevel courtLevel;
    private List<String> courtTypes; // Parsed from JSON string
    private CourtLevel fromLevel;
    private Boolean isAppeal;
    private Integer appealOrder;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
