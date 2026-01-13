package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Posting Assignment Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingAssignmentDTO {

    @NotNull(message = "Unit ID is required")
    private Long unitId;

    @NotNull(message = "Role code is required")
    private String roleCode;

    @NotNull(message = "Officer ID is required")
    private Long officerId;
}

