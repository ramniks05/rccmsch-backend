package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Act entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActDTO {
    private Long id;
    private String actCode;
    private String actName;
    private Integer actYear;
    private String description;
    private String sections; // JSON string
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
