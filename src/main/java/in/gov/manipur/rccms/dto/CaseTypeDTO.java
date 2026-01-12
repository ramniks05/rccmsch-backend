package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Case Type Request/Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseTypeDTO {

    private Long id;

    @NotBlank(message = "Case type name is required")
    @Size(max = 200, message = "Case type name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Case type code is required")
    @Size(max = 50, message = "Case type code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Case type code must contain only uppercase letters, numbers, and underscores")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isActive = true;
}

