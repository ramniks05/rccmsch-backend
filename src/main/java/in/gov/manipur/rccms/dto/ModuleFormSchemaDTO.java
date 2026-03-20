package in.gov.manipur.rccms.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ModuleFormSchemaDTO {
    private Long caseNatureId;
    private String caseNatureCode;
    private String caseNatureName;
    private Long caseTypeId;
    private String caseTypeCode;
    private String caseTypeName;
    private String moduleType;
    private List<ModuleFormFieldDTO> fields;
    private Integer totalFields;
}

