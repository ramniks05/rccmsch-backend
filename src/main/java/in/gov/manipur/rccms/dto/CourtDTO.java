package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.CourtLevel;
import in.gov.manipur.rccms.entity.CourtType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Court entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtDTO {
    private Long id;
    private String courtCode;
    private String courtName;
    private CourtLevel courtLevel;
    private CourtType courtType;
    private Long unitId;
    private String unitName;
    private String unitCode;
    private String designation;
    private String address;
    private String contactNumber;
    private String email;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
