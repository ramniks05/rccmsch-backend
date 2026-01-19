package in.gov.manipur.rccms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk updating multiple form fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateFieldsDTO {
    
    @NotEmpty(message = "Fields list cannot be empty")
    @Valid
    private List<BulkUpdateFieldItemDTO> fields;
}

