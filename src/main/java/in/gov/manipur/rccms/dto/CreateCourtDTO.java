package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.CourtLevel;
import in.gov.manipur.rccms.entity.CourtType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating Court
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourtDTO {
    
    @NotBlank(message = "Court code is required")
    @Size(max = 50, message = "Court code must not exceed 50 characters")
    private String courtCode;
    
    @NotBlank(message = "Court name is required")
    @Size(max = 200, message = "Court name must not exceed 200 characters")
    private String courtName;
    
    @NotNull(message = "Court level is required")
    private CourtLevel courtLevel;
    
    @NotNull(message = "Court type is required")
    private CourtType courtType;
    
    @NotNull(message = "Unit ID is required")
    private Long unitId;
    
    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
    
    @Size(max = 20, message = "Contact number must not exceed 20 characters")
    private String contactNumber;
    
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    private Boolean isActive = true;
}
