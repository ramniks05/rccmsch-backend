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

import java.util.Optional;

/**
 * Form Schema Data Initializer
 * Automatically initializes basic form fields for all 9 case types
 * Admin can add/modify fields later through admin APIs
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(4) // Initialize after workflows
public class FormSchemaDataInitializer implements CommandLineRunner {

    private final FormFieldDefinitionRepository fieldRepository;
    private final CaseTypeRepository caseTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========================================");
        log.info("Initializing Form Schema Data...");
        log.info("========================================");

        try {
            initializeMutationGiftSaleFields();
            initializeMutationDeathFields();
            initializePartitionFields();
            initializeClassificationBefore2014Fields();
            initializeClassificationAfter2014Fields();
            initializeCourtOrderFields();
            initializeAllotmentFields();
            initializeAcquisitionRFCTLARRFields();
            initializeAcquisitionDirectPurchaseFields();

            log.info("========================================");
            log.info("Form Schema Data initialization completed!");
            log.info("========================================");
        } catch (Exception e) {
            log.error("Error initializing form schema data: {}", e.getMessage(), e);
        }
    }

    /**
     * I. Mutation (after Gift/Sale Deeds) - Basic Fields
     */
    private void initializeMutationGiftSaleFields() {
        String caseTypeCode = "MUTATION_GIFT_SALE";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "registeredDeedNumber", "Registered Deed Number", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 50, \"pattern\": \"^[A-Z0-9/-]+$\"}", order++, 
                "Enter the registered deed number", "Deed number as per registration certificate");
        
        createFieldIfNotExists(caseType, "deedRegistrationDate", "Deed Registration Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select registration date", "Date when the deed was registered");
        
        createFieldIfNotExists(caseType, "deedType", "Deed Type", "SELECT", true, null, order++,
                "Select deed type", null, 
                "[{\"value\": \"SALE\", \"label\": \"Sale Deed\"}, {\"value\": \"GIFT\", \"label\": \"Gift Deed\"}]");
        
        createFieldIfNotExists(caseType, "sellerName", "Seller/Grantor Name", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 100}", order++, 
                "Enter seller/grantor full name", "Name of the person who sold/gifted the land");
        
        createFieldIfNotExists(caseType, "buyerName", "Buyer/Grantee Name", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 100}", order++, 
                "Enter buyer/grantee full name", "Name of the person who purchased/received the land");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details (Patta No., Dag No., etc.)", "Complete land identification details");
        
        createFieldIfNotExists(caseType, "subRegistrarOffice", "Sub-Registrar Office", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 100}", order++, 
                "Enter sub-registrar office name", "Office where the deed was registered");
        
        createFieldIfNotExists(caseType, "deedCopy", "Deed Copy", "FILE", true, null, order++,
                "Upload registered deed copy", "Upload scanned copy of registered deed (PDF/Image)");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * II. Mutation (after death of landowner) - Basic Fields
     */
    private void initializeMutationDeathFields() {
        String caseTypeCode = "MUTATION_DEATH";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "deceasedName", "Deceased Landowner Name", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 100}", order++, 
                "Enter deceased landowner name", "Full name of the deceased landowner");
        
        createFieldIfNotExists(caseType, "deathDate", "Date of Death", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select date of death", "Date when the landowner passed away");
        
        createFieldIfNotExists(caseType, "deathCertificateNumber", "Death Certificate Number", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 50}", order++, 
                "Enter death certificate number", "Certificate number issued by authorities");
        
        createFieldIfNotExists(caseType, "deathCertificateDate", "Death Certificate Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select certificate issue date", "Date when death certificate was issued");
        
        createFieldIfNotExists(caseType, "heirDetails", "Heir Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter details of legal heirs", "Names and relationships of all legal heirs");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details (Patta No., Dag No., etc.)", "Complete land identification details");
        
        createFieldIfNotExists(caseType, "deathCertificateCopy", "Death Certificate Copy", "FILE", true, null, order++,
                "Upload death certificate copy", "Upload scanned copy of death certificate (PDF/Image)");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * III. Partition (division of land parcel) - Basic Fields
     */
    private void initializePartitionFields() {
        String caseTypeCode = "PARTITION";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "originalPattaNumber", "Original Patta Number", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 50}", order++, 
                "Enter original patta number", "Patta number of the land to be partitioned");
        
        createFieldIfNotExists(caseType, "dagNumbers", "Dag Numbers", "TEXT", true, 
                "{\"minLength\": 1, \"maxLength\": 100}", order++, 
                "Enter dag numbers (comma separated)", "All dag numbers under the patta");
        
        createFieldIfNotExists(caseType, "partitionType", "Partition Type", "SELECT", true, null, order++,
                "Select partition type", null, 
                "[{\"value\": \"PATTA_2_DAG\", \"label\": \"Partition of 1 Patta with 2 Dag Nos.\"}, " +
                "{\"value\": \"PATTA_1_DAG\", \"label\": \"Partition of 1 Patta with 1 Dag No.\"}]");
        
        createFieldIfNotExists(caseType, "coOwners", "Co-Owners Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter co-owners names and shares", "Names of all co-owners and their respective shares");
        
        createFieldIfNotExists(caseType, "partitionDetails", "Partition Details", "TEXTAREA", true, 
                "{\"minLength\": 20, \"maxLength\": 1000}", order++, 
                "Enter detailed partition plan", "Detailed description of how land will be divided");
        
        createFieldIfNotExists(caseType, "landArea", "Total Land Area (in acres)", "NUMBER", true, 
                "{\"min\": 0.01, \"max\": 10000}", order++, 
                "Enter total land area", "Total area of land to be partitioned");
        
        createFieldIfNotExists(caseType, "supportingDocuments", "Supporting Documents", "FILE", false, null, order++,
                "Upload supporting documents", "Any additional documents related to partition");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * IV. Change in Classification (before 2014) - Basic Fields
     */
    private void initializeClassificationBefore2014Fields() {
        String caseTypeCode = "CLASSIFICATION_CHANGE_BEFORE_2014";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "currentClassification", "Current Classification", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 50}", order++, 
                "Enter current land classification", "Current classification of the land");
        
        createFieldIfNotExists(caseType, "proposedClassification", "Proposed Classification", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 50}", order++, 
                "Enter proposed classification", "New classification requested");
        
        createFieldIfNotExists(caseType, "revenueDepartmentApproval", "Revenue Department Approval", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 100}", order++, 
                "Enter approval reference number", "Reference number of Revenue Department approval");
        
        createFieldIfNotExists(caseType, "approvalDate", "Approval Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select approval date", "Date when Revenue Department approved the change");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details", "Complete land identification details");
        
        createFieldIfNotExists(caseType, "feesPaid", "Fees Paid", "NUMBER", true, 
                "{\"min\": 0}", order++, 
                "Enter fees paid amount", "Amount of fees paid for classification change");
        
        createFieldIfNotExists(caseType, "approvalDocument", "Approval Document", "FILE", true, null, order++,
                "Upload approval document", "Upload Revenue Department approval document");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * V. Change in Classification (after 2014) - Basic Fields
     */
    private void initializeClassificationAfter2014Fields() {
        String caseTypeCode = "CLASSIFICATION_CHANGE_AFTER_2014";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "currentClassification", "Current Classification", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 50}", order++, 
                "Enter current land classification", "Current classification of the land");
        
        createFieldIfNotExists(caseType, "proposedClassification", "Proposed Classification", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 50}", order++, 
                "Enter proposed classification", "New classification requested");
        
        createFieldIfNotExists(caseType, "revenueDepartmentApproval", "Revenue Department Approval", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 100}", order++, 
                "Enter approval reference number", "Reference number of Revenue Department approval under Conservation of Paddy Land and Wetland Act, 2014");
        
        createFieldIfNotExists(caseType, "approvalDate", "Approval Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select approval date", "Date when Revenue Department approved the change");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details", "Complete land identification details");
        
        createFieldIfNotExists(caseType, "feesPaid", "Fees Paid", "NUMBER", true, 
                "{\"min\": 0}", order++, 
                "Enter fees paid amount", "Amount of fees paid for classification change");
        
        createFieldIfNotExists(caseType, "approvalDocument", "Approval Document", "FILE", true, null, order++,
                "Upload approval document", "Upload Revenue Department approval document");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * VI. Implementation of Higher Court Order - Basic Fields
     */
    private void initializeCourtOrderFields() {
        String caseTypeCode = "HIGHER_COURT_ORDER";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "courtName", "Court Name", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 200}", order++, 
                "Enter court name", "Name of the Higher Court that passed the order");
        
        createFieldIfNotExists(caseType, "caseNumber", "Court Case Number", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 50}", order++, 
                "Enter court case number", "Case number in the court");
        
        createFieldIfNotExists(caseType, "orderDate", "Order Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select order date", "Date when the court order was passed");
        
        createFieldIfNotExists(caseType, "orderDetails", "Order Details", "TEXTAREA", true, 
                "{\"minLength\": 20, \"maxLength\": 1000}", order++, 
                "Enter order details", "Summary of the court order");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details", "Complete land identification details as per court order");
        
        createFieldIfNotExists(caseType, "courtOrderCopy", "Court Order Copy", "FILE", true, null, order++,
                "Upload court order copy", "Upload scanned copy of court order (PDF)");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * VII. Allotment of Land - Basic Fields
     */
    private void initializeAllotmentFields() {
        String caseTypeCode = "ALLOTMENT";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "governmentApprovalNumber", "Government Approval Number", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 100}", order++, 
                "Enter government approval number", "Reference number of government approval for allotment");
        
        createFieldIfNotExists(caseType, "approvalDate", "Approval Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select approval date", "Date when government approved the allotment");
        
        createFieldIfNotExists(caseType, "allotteeName", "Allottee Name", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 100}", order++, 
                "Enter allottee name", "Name of the person to whom land is allotted");
        
        createFieldIfNotExists(caseType, "premiumPaid", "Premium Paid", "NUMBER", true, 
                "{\"min\": 0}", order++, 
                "Enter premium amount paid", "Amount of premium paid for allotment");
        
        createFieldIfNotExists(caseType, "premiumPaymentDate", "Premium Payment Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select payment date", "Date when premium was paid");
        
        createFieldIfNotExists(caseType, "deedRegistrationNumber", "Deed Registration Number", "TEXT", false, 
                "{\"minLength\": 5, \"maxLength\": 50}", order++, 
                "Enter deed registration number", "Registration number if deed is already registered");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details", "Complete land identification details");
        
        createFieldIfNotExists(caseType, "allotmentDocuments", "Allotment Documents", "FILE", true, null, order++,
                "Upload allotment documents", "Upload government approval and deed documents");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * VIII. Land Acquisition (RFCTLARR/NHA) - Basic Fields
     */
    private void initializeAcquisitionRFCTLARRFields() {
        String caseTypeCode = "LAND_ACQUISITION_RFCTLARR_NHA";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "acquisitionAct", "Acquisition Act", "SELECT", true, null, order++,
                "Select acquisition act", null, 
                "[{\"value\": \"RFCTLARR\", \"label\": \"RFCTLARR Act, 2013\"}, " +
                "{\"value\": \"NHA\", \"label\": \"National Highways Act, 1956\"}]");
        
        createFieldIfNotExists(caseType, "acquiringAgency", "Acquiring Agency", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 200}", order++, 
                "Enter acquiring agency name", "Name of the agency acquiring the land");
        
        createFieldIfNotExists(caseType, "awardOrderNumber", "Award Order Number", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 100}", order++, 
                "Enter award order number", "DC's order number for compensation award");
        
        createFieldIfNotExists(caseType, "awardDate", "Award Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select award date", "Date when compensation award was passed");
        
        createFieldIfNotExists(caseType, "compensationAmount", "Compensation Amount", "NUMBER", true, 
                "{\"min\": 0}", order++, 
                "Enter compensation amount", "Amount of compensation awarded");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details", "Complete land identification details");
        
        createFieldIfNotExists(caseType, "acquisitionDocuments", "Acquisition Documents", "FILE", true, null, order++,
                "Upload acquisition documents", "Upload award order and related documents");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * IX. Land Acquisition (Direct Purchase) - Basic Fields
     */
    private void initializeAcquisitionDirectPurchaseFields() {
        String caseTypeCode = "LAND_ACQUISITION_DIRECT_PURCHASE";
        Optional<CaseType> caseTypeOpt = caseTypeRepository.findByCode(caseTypeCode);
        if (caseTypeOpt.isEmpty()) {
            log.warn("Case type not found: {}. Skipping form field initialization.", caseTypeCode);
            return;
        }
        CaseType caseType = caseTypeOpt.get();

        int order = 1;
        createFieldIfNotExists(caseType, "governmentApprovalNumber", "Government Approval Number", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 100}", order++, 
                "Enter government approval number", "Reference number of government approval for direct purchase");
        
        createFieldIfNotExists(caseType, "approvalDate", "Approval Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select approval date", "Date when government approved direct purchase");
        
        createFieldIfNotExists(caseType, "acquiringAgency", "Acquiring Agency", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 200}", order++, 
                "Enter acquiring agency name", "Name of the agency purchasing the land");
        
        createFieldIfNotExists(caseType, "sellerName", "Seller Name", "TEXT", true, 
                "{\"minLength\": 3, \"maxLength\": 100}", order++, 
                "Enter seller name", "Name of the original landowner");
        
        createFieldIfNotExists(caseType, "saleDeedNumber", "Sale Deed Number", "TEXT", true, 
                "{\"minLength\": 5, \"maxLength\": 50}", order++, 
                "Enter sale deed registration number", "Registration number of sale deed");
        
        createFieldIfNotExists(caseType, "deedRegistrationDate", "Deed Registration Date", "DATE", true, 
                "{\"maxDate\": \"today\"}", order++, 
                "Select registration date", "Date when sale deed was registered");
        
        createFieldIfNotExists(caseType, "purchaseAmount", "Purchase Amount", "NUMBER", true, 
                "{\"min\": 0}", order++, 
                "Enter purchase amount", "Amount paid for direct purchase");
        
        createFieldIfNotExists(caseType, "landDetails", "Land Details", "TEXTAREA", true, 
                "{\"minLength\": 10, \"maxLength\": 500}", order++, 
                "Enter land details", "Complete land identification details");
        
        createFieldIfNotExists(caseType, "purchaseDocuments", "Purchase Documents", "FILE", true, null, order++,
                "Upload purchase documents", "Upload government approval and sale deed documents");

        log.info("Initialized form fields for: {}", caseTypeCode);
    }

    /**
     * Helper method to create form field if it doesn't exist
     */
    private void createFieldIfNotExists(CaseType caseType, String fieldName, String fieldLabel, 
            String fieldType, boolean isRequired, String validationRules, int displayOrder,
            String placeholder, String helpText) {
        createFieldIfNotExists(caseType, fieldName, fieldLabel, fieldType, isRequired, 
                validationRules, displayOrder, placeholder, helpText, null);
    }

    /**
     * Helper method to create form field if it doesn't exist (with field options)
     */
    private void createFieldIfNotExists(CaseType caseType, String fieldName, String fieldLabel, 
            String fieldType, boolean isRequired, String validationRules, int displayOrder,
            String placeholder, String helpText, String fieldOptions) {
        if (fieldRepository.existsByCaseTypeIdAndFieldName(caseType.getId(), fieldName)) {
            log.debug("Field already exists, skipping: caseType={}, fieldName={}", 
                    caseType.getCode(), fieldName);
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

        fieldRepository.save(field);
        log.debug("Created form field: caseType={}, fieldName={}", caseType.getCode(), fieldName);
    }
}

