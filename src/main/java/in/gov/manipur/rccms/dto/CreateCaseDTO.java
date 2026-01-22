package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new case
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseDTO {
    
    @NotNull(message = "Case type ID is required")
    private Long caseTypeId;
    
    @NotNull(message = "Applicant ID is required")
    private Long applicantId;
    
    @NotNull(message = "Unit ID is required")
    private Long unitId;
    
    private Long caseNatureId; // Case nature (New File, Appeal, etc.)
    
    private Long courtId; // Court where petition is filed
    
    private String originalOrderLevel; // For appeals - level of original order (CIRCLE, SUB_DIVISION, DISTRICT)
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String description;
    
    private String priority; // "LOW", "MEDIUM", "HIGH", "URGENT"
    
    private LocalDate applicationDate;
    
    private String remarks;
    
    private String caseData; // JSON string for case-specific data
}

