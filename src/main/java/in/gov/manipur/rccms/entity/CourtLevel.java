package in.gov.manipur.rccms.entity;

/**
 * Court Level Enum
 * Represents the administrative level at which a court operates
 * This can be made configurable in future by creating a master table
 */
public enum CourtLevel {
    CIRCLE,        // Circle level (SDC)
    SUB_DIVISION,  // Sub-Division level (SDO)
    DISTRICT,      // District level (DC, Revenue Court, Revenue Tribunal)
    STATE          // State level (if applicable)
}
