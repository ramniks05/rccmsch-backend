package in.gov.manipur.rccms.Constants.Enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EventType {

    HOLIDAY("Holiday"),
    HALF_DAY("Half Day"),
    WORKING_DAY("Working Day"),
    SPECIAL("Special"),;
    public final String label;
}
