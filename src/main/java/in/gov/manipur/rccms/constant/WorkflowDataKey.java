package in.gov.manipur.rccms.constant;

import in.gov.manipur.rccms.entity.ModuleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for workflow_data keys stored in case_workflow_instance.workflow_data.
 * Use these exact keys when:
 * - Writing to workflow_data (CaseDocumentService, CaseModuleFormService, WorkflowEngineService)
 * - Setting permission conditions (workflowDataFieldsRequired in workflow_permission.conditions)
 * This avoids mismatches (e.g. requiring "DRAFT" when the app only writes NOTICE_DRAFT_CREATED).
 */
public final class WorkflowDataKey {

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        // Form submission flags: MODULE_SUBMITTED (from CaseModuleFormService)
        for (ModuleType mt : ModuleType.values()) {
            LABELS.put(mt.name() + "_SUBMITTED", mt.name().charAt(0) + mt.name().substring(1).toLowerCase() + " form submitted");
        }
        // Document flags: two stages only - DRAFT (save) and SIGNED (save and sign). No READY stage.
        for (ModuleType mt : new ModuleType[]{ModuleType.NOTICE, ModuleType.ORDERSHEET, ModuleType.JUDGEMENT}) {
            String name = mt.name().charAt(0) + mt.name().substring(1).toLowerCase();
            LABELS.put(mt.name() + "_DRAFT_CREATED", "Draft " + name + " created");
            LABELS.put(mt.name() + "_SIGNED", name + " saved and signed");
        }
        // Special
        LABELS.put("NOTICE_ACCEPTED_BY_APPLICANT", "Notice accepted by applicant");
    }

    /** Unmodifiable set of all valid workflow_data keys. Use for validation and API. */
    private static final Set<String> VALID_KEYS = Collections.unmodifiableSet(LABELS.keySet());

    private WorkflowDataKey() {}

    /**
     * Returns the set of all valid workflow data keys. Use when saving permission conditions
     * (workflowDataFieldsRequired must only contain keys from this set).
     */
    public static Set<String> validKeys() {
        return VALID_KEYS;
    }

    /**
     * Returns true if the key is one of the canonical workflow_data keys the app writes.
     */
    public static boolean isValid(String key) {
        return key != null && VALID_KEYS.contains(key);
    }

    /**
     * Display label for UI/checklist. Falls back to key with underscores as spaces if unknown.
     */
    public static String getDisplayLabel(String key) {
        if (key == null) return "";
        return LABELS.getOrDefault(key, key.replace("_", " "));
    }

    /**
     * All valid keys with their display labels (for admin API dropdown).
     */
    public static Map<String, String> keysWithLabels() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(LABELS));
    }

    /** Kind of workflow key: form submission, document lifecycle, or special. */
    public static final String KIND_FORM = "FORM";
    public static final String KIND_DOCUMENT = "DOCUMENT";
    public static final String KIND_SPECIAL = "SPECIAL";

    /**
     * Returns each key with label, moduleType, and kind so the frontend can bind to forms/documents.
     * - FORM: key is set when a module form is submitted → bind to GET/POST .../module-forms/{moduleType}
     * - DOCUMENT: key is set when a document is created/updated → bind to .../documents/{moduleType}
     * - SPECIAL: key is set by other actions (e.g. NOTICE_ACCEPTED_BY_APPLICANT).
     * Form schema is dynamic (fields per case nature/type) but is always for a fixed moduleType (HEARING, NOTICE, etc.).
     */
    public static List<Map<String, String>> keysWithBinding() {
        List<Map<String, String>> list = new ArrayList<>();
        for (ModuleType mt : ModuleType.values()) {
            String key = mt.name() + "_SUBMITTED";
            if (LABELS.containsKey(key)) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("key", key);
                entry.put("label", LABELS.get(key));
                entry.put("moduleType", mt.name());
                entry.put("kind", KIND_FORM);
                list.add(entry);
            }
        }
        for (ModuleType mt : new ModuleType[]{ModuleType.NOTICE, ModuleType.ORDERSHEET, ModuleType.JUDGEMENT}) {
            for (String suffix : new String[]{"_DRAFT_CREATED", "_SIGNED"}) {
                String key = mt.name() + suffix;
                if (LABELS.containsKey(key)) {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("key", key);
                    entry.put("label", LABELS.get(key));
                    entry.put("moduleType", mt.name());
                    entry.put("kind", KIND_DOCUMENT);
                    list.add(entry);
                }
            }
        }
        Map<String, String> special = new LinkedHashMap<>();
        special.put("key", "NOTICE_ACCEPTED_BY_APPLICANT");
        special.put("label", LABELS.get("NOTICE_ACCEPTED_BY_APPLICANT"));
        special.put("moduleType", "NOTICE");
        special.put("kind", KIND_SPECIAL);
        list.add(special);
        return Collections.unmodifiableList(list);
    }
}
