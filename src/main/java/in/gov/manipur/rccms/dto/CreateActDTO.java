package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating Act
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateActDTO {
    
    @NotBlank(message = "Act code is required")
    @Size(max = 50, message = "Act code must not exceed 50 characters")
    private String actCode;
    
    @NotBlank(message = "Act name is required")
    @Size(max = 300, message = "Act name must not exceed 300 characters")
    private String actName;
    
    @NotNull(message = "Act year is required")
    private Integer actYear;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private String sections; // JSON string for relevant sections
    
    private Boolean isActive = true;
}
