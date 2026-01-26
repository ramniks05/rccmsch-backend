package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Form Field Group
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormFieldGroupDTO {
    private Long id;
    private Long caseTypeId;
    private String caseTypeName;
    private String caseTypeCode;
    private String groupCode;
    private String groupLabel;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
