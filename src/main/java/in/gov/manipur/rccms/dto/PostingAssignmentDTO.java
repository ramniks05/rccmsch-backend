package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Posting Assignment Request
 * Supports TWO types of postings:
 * 1. Court-based: Provide courtId (unitId will be derived from court)
 * 2. Unit-based: Provide unitId only (courtId should be null)
 * 
 * Business Rules:
 * - Either courtId OR unitId must be provided (not both, not neither)
 * - For court-based: courtId is required, unitId is optional (derived from court)
 * - For unit-based: unitId is required, courtId must be null
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingAssignmentDTO {

    /**
     * Court ID for court-based posting
     * Required for court-based postings (TEHSILDAR, READER, etc.)
     * Must be NULL for unit-based postings (PATWARI, KANUNGO, etc.)
     */
    private Long courtId;

    /**
     * Unit ID for unit-based posting
     * Required for unit-based postings (PATWARI, KANUNGO, etc.)
     * Optional for court-based postings (will be derived from court)
     */
    private Long unitId;

    /**
     * Role from role_master (id). Resolved by roleId when present, else by roleCode.
     */
    private Long roleId;

    /**
     * Role code (from role_master). Used when roleId is not provided.
     */
    private String roleCode;

    @NotNull(message = "Officer ID is required")
    private Long officerId;
}

