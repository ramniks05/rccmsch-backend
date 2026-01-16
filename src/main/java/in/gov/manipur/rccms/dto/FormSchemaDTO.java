package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Form Schema
 * Contains all form fields for a case type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSchemaDTO {
    private Long caseTypeId;
    private String caseTypeName;
    private String caseTypeCode;
    private List<FormFieldDefinitionDTO> fields;
    private Integer totalFields;
}

