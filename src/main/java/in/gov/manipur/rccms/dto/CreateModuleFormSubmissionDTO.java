package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModuleFormSubmissionDTO {
    @NotBlank
    private String formData; // JSON string
    private String remarks;
}

