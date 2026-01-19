package in.gov.manipur.rccms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single field update item in bulk update
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateFieldItemDTO {
    
    @NotNull(message = "Field ID is required")
    private Long fieldId;
    
    @NotNull(message = "Update data is required")
    @Valid
    private UpdateFormFieldDTO updateData;
}

