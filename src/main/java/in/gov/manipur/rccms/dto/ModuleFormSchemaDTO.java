package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.ModuleType;
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
    private ModuleType moduleType;
    private List<ModuleFormFieldDTO> fields;
    private Integer totalFields;
}

