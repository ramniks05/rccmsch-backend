package in.gov.manipur.rccms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk creating multiple form fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateFieldsDTO {
    
    @NotNull(message = "Case type ID is required")
    private Long caseTypeId;
    
    @NotEmpty(message = "Fields list cannot be empty")
    @Valid
    private List<CreateFormFieldDTO> fields;
}

