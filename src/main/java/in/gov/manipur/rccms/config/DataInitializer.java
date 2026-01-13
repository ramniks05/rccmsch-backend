package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Initializer
 * Automatically inserts master data when application starts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CaseTypeRepository caseTypeRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("Starting data initialization...");
        log.info("========================================");
        
        initializeCaseTypes();
        
        log.info("========================================");
        log.info("Data initialization completed!");
        log.info("========================================");
    }

    /**
     * Initialize Case Types master data
     */
    private void initializeCaseTypes() {
        log.info("Initializing Case Types...");
        
        List<CaseType> caseTypesToInsert = new ArrayList<>();
        
        // Define all case types
        caseTypesToInsert.add(createCaseType(
            "MUTATION_GIFT_SALE",
            "Mutation (after Gift/Sale Deeds)",
            "Mutation after Gift or Sale Deeds"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "MUTATION_DEATH",
            "Mutation (after death of landowner)",
            "Mutation after death of landowner"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "PARTITION",
            "Partition (division of land parcel)",
            "Partition or division of land parcel"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "CLASSIFICATION_CHANGE_BEFORE_2014",
            "Change in Classification of Land (before 2014)",
            "Change in classification of land before 2014"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "CLASSIFICATION_CHANGE_AFTER_2014",
            "Change in Classification of Land (after 2014)",
            "Change in classification of land after 2014"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "HIGHER_COURT_ORDER",
            "Implementation of order passed by a Higher Court",
            "Implementation of order passed by a Higher Court"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "ALLOTMENT",
            "Allotment of Land",
            "Allotment of Land"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "LAND_ACQUISITION_RFCTLARR_NHA",
            "Land Acquisition (under RFCTLARR Act, 2013 or National Highways Act, 1956)",
            "Land Acquisition under RFCTLARR Act, 2013 or National Highways Act, 1956"
        ));
        
        caseTypesToInsert.add(createCaseType(
            "LAND_ACQUISITION_DIRECT_PURCHASE",
            "Land Acquisition (under Direct Purchase)",
            "Land Acquisition under Direct Purchase"
        ));
        
        // Insert case types if they don't exist
        int insertedCount = 0;
        int skippedCount = 0;
        
        for (CaseType caseType : caseTypesToInsert) {
            if (!caseTypeRepository.existsByCode(caseType.getCode())) {
                caseTypeRepository.save(caseType);
                insertedCount++;
                log.info("Inserted case type: {} - {}", caseType.getCode(), caseType.getName());
            } else {
                skippedCount++;
                log.debug("Case type already exists, skipping: {}", caseType.getCode());
            }
        }
        
        log.info("Case Types initialization completed: {} inserted, {} skipped (already exist)", 
                insertedCount, skippedCount);
    }

    /**
     * Create a CaseType entity
     */
    private CaseType createCaseType(String code, String name, String description) {
        CaseType caseType = new CaseType();
        caseType.setCode(code);
        caseType.setName(name);
        caseType.setDescription(description);
        caseType.setIsActive(true);
        caseType.setCreatedAt(LocalDateTime.now());
        caseType.setUpdatedAt(LocalDateTime.now());
        return caseType;
    }
}

