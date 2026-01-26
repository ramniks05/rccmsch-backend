package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Case Nature Request/Response (Previously CaseTypeDTO)
 * Represents case natures like MUTATION_GIFT_SALE, PARTITION, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseNatureDTO {

    private Long id;

    @NotBlank(message = "Case nature name is required")
    @Size(max = 200, message = "Case nature name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Case nature code is required")
    @Size(max = 50, message = "Case nature code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Case nature code must contain only uppercase letters, numbers, and underscores")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Long actId;
    private String actName;
    private String actCode;
    private Integer actYear;
    private String workflowCode;
    private Boolean isActive = true;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
