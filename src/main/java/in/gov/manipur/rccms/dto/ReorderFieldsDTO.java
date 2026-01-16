package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for reordering form fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderFieldsDTO {
    
    @NotEmpty(message = "Field IDs list cannot be empty")
    private List<Long> fieldIds; // Ordered list of field IDs
}

