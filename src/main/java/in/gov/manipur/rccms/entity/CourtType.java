package in.gov.manipur.rccms.entity;

/**
 * Court Type Enum
 * Represents different types of courts at various levels
 * This can be made configurable in future by creating a master table
 */
public enum CourtType {
    SDC_COURT,           // Sub-Divisional Circle Officer Court (Circle level)
    SDO_COURT,           // Sub-Divisional Officer Court (Sub-Division level)
    DC_COURT,            // Deputy Commissioner Court (District level)
    REVENUE_COURT,       // Revenue Court (District level)
    REVENUE_TRIBUNAL,    // Revenue Tribunal (District level)
    STATE_TRIBUNAL       // State level Tribunal (if exists)
}
