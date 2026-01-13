package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.AdminUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Administrative Unit Request/Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUnitDTO {

    private Long unitId;

    @NotBlank(message = "Unit code is required")
    @Size(max = 50, message = "Unit code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Unit code must contain only uppercase letters, numbers, and underscores")
    private String unitCode;

    @NotBlank(message = "Unit name is required")
    @Size(max = 200, message = "Unit name must not exceed 200 characters")
    private String unitName;

    @NotNull(message = "Unit level is required")
    private AdminUnit.UnitLevel unitLevel;

    @NotNull(message = "LGD code is required")
    private Long lgdCode;

    private Long parentUnitId;

    private String parentUnitName; // For response only

    private Boolean isActive = true;
}

