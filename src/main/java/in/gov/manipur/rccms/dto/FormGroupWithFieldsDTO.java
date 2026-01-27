package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Form Field Group with its associated fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormGroupWithFieldsDTO {
    private Long groupId;
    private String groupCode;
    private String groupLabel;
    private String description;
    private Integer displayOrder;
    
    @Builder.Default
    private List<FormFieldDefinitionDTO> fields = new ArrayList<>();
    
    private Integer fieldCount;
}
