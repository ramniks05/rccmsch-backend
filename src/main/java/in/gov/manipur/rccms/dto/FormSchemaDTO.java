package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Form Schema
 * Contains all form fields for a case type, organized by groups
 * Simplified response for easy frontend binding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormSchemaDTO {
    private Long caseTypeId;
    private String caseTypeName;
    private String caseTypeCode;
    
    @Builder.Default
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<FormFieldDefinitionDTO> fields = new ArrayList<>(); // Flat list (for backward compatibility)
    
    @Builder.Default
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<FormGroupWithFieldsDTO> groups = new ArrayList<>(); // Groups with their fields (structured)
    
    @Builder.Default
    private Integer totalFields = 0;
}

