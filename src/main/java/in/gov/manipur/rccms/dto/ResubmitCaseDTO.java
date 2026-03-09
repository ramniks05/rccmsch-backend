package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

    @JsonDeserialize(using = JsonStringOrObjectDeserializer.class)
    @NotBlank(message = "Case data is required")
    private String caseData; // JSON string for case-specific data (accepts string or object from client)

    private String remarks;
}
