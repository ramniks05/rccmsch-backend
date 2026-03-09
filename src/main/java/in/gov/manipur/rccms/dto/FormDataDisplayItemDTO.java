package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One item of case form data with label and group for display.
 * Used in case response so frontend can show form data in an organized way.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormDataDisplayItemDTO {
    private String fieldName;
    private String fieldLabel;
    private String fieldGroup;
    private String groupLabel;
    private Object value;
    private Integer displayOrder;
    private Integer groupDisplayOrder;
}
