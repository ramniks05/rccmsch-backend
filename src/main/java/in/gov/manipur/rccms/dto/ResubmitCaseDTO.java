package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for resubmitting a case after correction
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResubmitCaseDTO {

    @NotBlank(message = "Case data is required")
    private String caseData; // JSON string for case-specific data

    private String remarks;
}
