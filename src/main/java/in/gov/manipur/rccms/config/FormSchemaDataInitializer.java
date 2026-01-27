package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.entity.FormFieldDefinition;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import in.gov.manipur.rccms.repository.FormFieldDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Form Schema Data Initializer
 * NOTE: This initializer is DISABLED after migration to CaseType-based form schemas.
 * Form fields are now associated with CaseType instead of CaseNature.
 * Admin should configure form fields per case type through admin APIs.
 * 
 * If you need to initialize default fields, update this to work with CaseType.
 * Each case type (NEW_FILE, APPEAL, etc.) can have different form fields.
 */
@Slf4j
// @Component  // DISABLED - Uncomment and update to use CaseType if needed
@RequiredArgsConstructor
@Order(4) // Initialize after workflows
public class FormSchemaDataInitializer implements CommandLineRunner {

    private final FormFieldDefinitionRepository fieldRepository;
    private final CaseTypeRepository caseTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========================================");
        log.info("Form Schema Data Initializer - DISABLED");
        log.info("Form fields are now managed per CaseType through admin APIs");
        log.info("Please configure form fields for each case type manually");
        log.info("========================================");
        
        // Initializer disabled - fields should be configured per case type through admin APIs
        // If you need default initialization, update methods below to use CaseType instead of CaseNature
    }


    /**
     * Helper method to create form field if it doesn't exist
     * NOTE: This method is kept for reference but disabled.
     * Update to use CaseType instead of CaseNature if you need to re-enable initialization.
     */
    @SuppressWarnings("unused")
    private void createFieldIfNotExists(CaseType caseType, String fieldName, String fieldLabel, 
            String fieldType, boolean isRequired, String validationRules, int displayOrder,
            String placeholder, String helpText) {
        createFieldIfNotExists(caseType, fieldName, fieldLabel, fieldType, isRequired, 
                validationRules, displayOrder, placeholder, helpText, null);
    }

    /**
     * Helper method to create form field if it doesn't exist (with field options)
     * NOTE: This method is kept for reference but disabled.
     * Update to use CaseType instead of CaseNature if you need to re-enable initialization.
     */
    @SuppressWarnings("unused")
    private void createFieldIfNotExists(CaseType caseType, String fieldName, String fieldLabel, 
            String fieldType, boolean isRequired, String validationRules, int displayOrder,
            String placeholder, String helpText, String fieldOptions) {
        if (fieldRepository.existsByCaseTypeIdAndFieldName(caseType.getId(), fieldName)) {
            log.debug("Field already exists, skipping: caseType={}, fieldName={}", 
                    caseType.getTypeCode(), fieldName);
            return;
        }

        FormFieldDefinition field = new FormFieldDefinition();
        field.setCaseType(caseType);
        field.setCaseTypeId(caseType.getId());
        field.setFieldName(fieldName);
        field.setFieldLabel(fieldLabel);
        field.setFieldType(fieldType);
        field.setIsRequired(isRequired);
        field.setValidationRules(validationRules);
        field.setDisplayOrder(displayOrder);
        field.setIsActive(true);
        field.setPlaceholder(placeholder);
        field.setHelpText(helpText);
        field.setFieldOptions(fieldOptions);
        // Note: fieldGroup should be set separately if needed (references FormFieldGroup.groupCode)

        fieldRepository.save(field);
        log.debug("Created form field: caseType={}, fieldName={}", caseType.getTypeCode(), fieldName);
    }
}

