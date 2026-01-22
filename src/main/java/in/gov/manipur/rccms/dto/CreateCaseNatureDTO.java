package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.CourtLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating/updating CaseNature
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseNatureDTO {
    
    @NotNull(message = "Case type ID is required")
    private Long caseTypeId;
    
    @NotBlank(message = "Nature code is required")
    @Size(max = 50, message = "Nature code must not exceed 50 characters")
    private String natureCode;
    
    @NotBlank(message = "Nature name is required")
    @Size(max = 200, message = "Nature name must not exceed 200 characters")
    private String natureName;
    
    @NotNull(message = "Court level is required")
    private CourtLevel courtLevel;
    
    @NotNull(message = "Court types are required")
    private List<String> courtTypes; // Will be converted to JSON array
    
    private CourtLevel fromLevel; // For appeals - original order level
    
    private Boolean isAppeal = false;
    
    private Integer appealOrder = 0; // 1 for first appeal, 2 for second appeal
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private Boolean isActive = true;
    
    private Integer displayOrder = 0;
}
