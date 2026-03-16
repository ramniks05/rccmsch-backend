package in.gov.manipur.rccms.constant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All possible workflow state codes used for dashboard/reporting with display labels.
 * Use for workflow state create/edit UI: user can choose from this list or type a new code.
 * Labels are for display (e.g. "Case Filled", "Hearing Scheduled", "Disposed").
 */
public final class ReportingStateLabel {

    private static final Map<String, String> STATE_CODE_TO_LABEL = new LinkedHashMap<>();

    static {
        // Case lifecycle – common reporting terms
        put("INITIATE_CASE", "Case Filled"); 
        put("DA_ENTRY", "DA Entry");
        put("CASE_ADMITTED", "Case Accepted");
        put("HEARING_SCHEDULED", "Hearing Scheduled");

        // Notice & hearing
        put("NOTICE_DRAFT", "Notice Draft");
        put("NOTICE_GENERATED", "Notice Generated");
        put("NOTICE_PUBLISHED", "Notice Published");
       
        put("HEARING_COMPLETED", "Hearing Completed");
        put("OBJECTION_PERIOD", "Objection Period");

        // Decision / judgement
        put("DECISION_PENDING", "Decision Pending");
        put("PENDING_JUDGEMENT", "Pending Judgement");
        put("JUDGEMENT_PENDING", "Judgement Pending");     
        // Final / disposed
        put("DISPOSED", "Disposed");
        put("REJECTED", "Rejected");     
        // Correction
        put("RETURNED_FOR_CORRECTION", "Returned for Correction");

        // Mutation / partition / other workflows
        put("FEES_PAID", "Fees Paid");
        // Court order
        put("COURT_ORDER_RECEIVED", "Court Order Received");
        // Notice sent (actions required)
        put("NOTICE_SENT_TO_PARTY", "Notice Sent to Party");
        put("NOTICE_SENT", "Notice Sent");
    }

    private static void put(String code, String label) {
        if (!STATE_CODE_TO_LABEL.containsKey(code)) {
            STATE_CODE_TO_LABEL.put(code, label);
        }
    }

    private ReportingStateLabel() {}

    /**
     * All possible state codes with display labels for reporting/dashboard.
     * Use in workflow state UI: choose from list or type new.
     */
    public static Map<String, String> allReportingStatesWithLabels() {
        return new LinkedHashMap<>(STATE_CODE_TO_LABEL);
    }

    /**
     * List of { stateCode, stateName } for API (dropdown/autocomplete).
     */
    public static List<Map<String, String>> allReportingStatesAsList() {
        return STATE_CODE_TO_LABEL.entrySet().stream()
                .map(e -> Map.<String, String>of("stateCode", e.getKey(), "stateName", e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Display label for a state code; falls back to code with underscores as spaces.
     */
    public static String getLabel(String stateCode) {
        if (stateCode == null) return "";
        return STATE_CODE_TO_LABEL.getOrDefault(stateCode, stateCode.replace("_", " "));
    }
}
