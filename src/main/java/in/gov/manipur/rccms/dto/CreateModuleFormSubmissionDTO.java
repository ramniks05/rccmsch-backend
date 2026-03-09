package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModuleFormSubmissionDTO {
    @NotBlank
    @JsonDeserialize(using = JsonStringOrObjectDeserializer.class)
    private String formData; // JSON string (accepts string or object from client)
    private String remarks;
}

