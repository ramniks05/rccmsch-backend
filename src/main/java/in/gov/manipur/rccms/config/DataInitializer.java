package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.repository.AdminUnitRepository;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Initializer
 * Automatically inserts master data when application starts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CaseTypeRepository caseTypeRepository;
    private final AdminUnitRepository adminUnitRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("Starting data initialization...");
        log.info("========================================");
        
        initializeCaseTypes();
        initializeAdminUnits();
        
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

    /**
     * Initialize Administrative Units master data
     */
    private void initializeAdminUnits() {
        log.info("Initializing Administrative Units...");
        
        Map<String, AdminUnit> unitMap = new HashMap<>();
        long lgdCodeCounter = 140001L;
        
        // 1. Insert STATE
        AdminUnit state = createAdminUnit("MANIPUR", "Manipur", AdminUnit.UnitLevel.STATE, lgdCodeCounter++, null);
        state = saveIfNotExists(state, "MANIPUR");
        unitMap.put("MANIPUR", state);
        log.info("Inserted State: Manipur");
        
        // 2. Insert DISTRICTS
        String[] districts = {
            "Imphal West", "Imphal East", "Thoubal", "Bishnupur", "Jiribam", "Kakching"
        };
        
        for (String districtName : districts) {
            String code = generateCode(districtName);
            AdminUnit district = createAdminUnit(code, districtName, AdminUnit.UnitLevel.DISTRICT, lgdCodeCounter++, state);
            district = saveIfNotExists(district, code);
            unitMap.put(code, district);
            log.info("Inserted District: {}", districtName);
        }
        
        // 3. Insert SUB-DIVISIONS
        // Imphal West
        insertSubDivision("Patsoi", unitMap.get("IMPHAL_WEST"), unitMap, lgdCodeCounter++);
        insertSubDivision("Wanggoi", unitMap.get("IMPHAL_WEST"), unitMap, lgdCodeCounter++);
        insertSubDivision("Lamsang", unitMap.get("IMPHAL_WEST"), unitMap, lgdCodeCounter++);
        insertSubDivision("Lamphel", unitMap.get("IMPHAL_WEST"), unitMap, lgdCodeCounter++);
        
        // Imphal East
        insertSubDivision("Sawombung", unitMap.get("IMPHAL_EAST"), unitMap, lgdCodeCounter++);
        insertSubDivision("Porompat", unitMap.get("IMPHAL_EAST"), unitMap, lgdCodeCounter++);
        insertSubDivision("Keirao Bitra", unitMap.get("IMPHAL_EAST"), unitMap, lgdCodeCounter++);
        
        // Thoubal
        insertSubDivision("Thoubal", unitMap.get("THOUBAL"), unitMap, lgdCodeCounter++, "THOUBAL_SD");
        insertSubDivision("Lilong", unitMap.get("THOUBAL"), unitMap, lgdCodeCounter++);
        
        // Bishnupur
        insertSubDivision("Bishnupur", unitMap.get("BISHNUPUR"), unitMap, lgdCodeCounter++, "BISHNUPUR_SD");
        insertSubDivision("Nambol", unitMap.get("BISHNUPUR"), unitMap, lgdCodeCounter++);
        insertSubDivision("Ningthoukhong", unitMap.get("BISHNUPUR"), unitMap, lgdCodeCounter++);
        
        // Kakching
        insertSubDivision("Kakching", unitMap.get("KAKCHING"), unitMap, lgdCodeCounter++, "KAKCHING_SD");
        insertSubDivision("Waikhong", unitMap.get("KAKCHING"), unitMap, lgdCodeCounter++);
        
        // Jiribam
        insertSubDivision("Jiribam", unitMap.get("JIRIBAM"), unitMap, lgdCodeCounter++, "JIRIBAM_SD");
        insertSubDivision("Borobekra", unitMap.get("JIRIBAM"), unitMap, lgdCodeCounter++);
        
        // 4. Insert CIRCLES
        // Patsoi
        insertCircle("Patsoi", unitMap.get("PATSOI"), unitMap, lgdCodeCounter++);
        insertCircle("Konthoujam", unitMap.get("PATSOI"), unitMap, lgdCodeCounter++);
        
        // Wanggoi
        insertCircle("Wanggoi", unitMap.get("WANGGOI"), unitMap, lgdCodeCounter++);
        insertCircle("Hiyangthang", unitMap.get("WANGGOI"), unitMap, lgdCodeCounter++);
        insertCircle("Mayang Imphal", unitMap.get("WANGGOI"), unitMap, lgdCodeCounter++);
        insertCircle("Lilong Chajing", unitMap.get("WANGGOI"), unitMap, lgdCodeCounter++);
        
        // Lamsang
        insertCircle("Lamsang", unitMap.get("LAMSANG"), unitMap, lgdCodeCounter++);
        insertCircle("Sekmai", unitMap.get("LAMSANG"), unitMap, lgdCodeCounter++);
        insertCircle("Salam", unitMap.get("LAMSANG"), unitMap, lgdCodeCounter++);
        
        // Lamphel
        insertCircle("Lamphel Central I", unitMap.get("LAMPHEL"), unitMap, lgdCodeCounter++);
        insertCircle("Lamphel Central II", unitMap.get("LAMPHEL"), unitMap, lgdCodeCounter++);
        
        // Sawombung
        insertCircle("Sawombung", unitMap.get("SAWOMBUNG"), unitMap, lgdCodeCounter++);
        insertCircle("Sagolmang", unitMap.get("SAWOMBUNG"), unitMap, lgdCodeCounter++);
        
        // Porompat
        insertCircle("Heingang", unitMap.get("POROMPAT"), unitMap, lgdCodeCounter++);
        insertCircle("Porompat", unitMap.get("POROMPAT"), unitMap, lgdCodeCounter++);
        
        // Keirao Bitra
        insertCircle("Tulihal", unitMap.get("KEIRAO_BITRA"), unitMap, lgdCodeCounter++);
        insertCircle("Keirao Bitra", unitMap.get("KEIRAO_BITRA"), unitMap, lgdCodeCounter++);
        
        // Thoubal (Sub-Division)
        insertCircle("Thoubal", unitMap.get("THOUBAL_SD"), unitMap, lgdCodeCounter++);
        insertCircle("Khongjom", unitMap.get("THOUBAL_SD"), unitMap, lgdCodeCounter++);
        insertCircle("Heirok", unitMap.get("THOUBAL_SD"), unitMap, lgdCodeCounter++);
        insertCircle("Yairipok", unitMap.get("THOUBAL_SD"), unitMap, lgdCodeCounter++);
        
        // Lilong (Sub-Division)
        insertCircle("Lilong", unitMap.get("LILONG"), unitMap, lgdCodeCounter++);
        insertCircle("Irong Chesaba", unitMap.get("LILONG"), unitMap, lgdCodeCounter++);
        
        // Bishnupur (Sub-Division)
        insertCircle("Bishnupur", unitMap.get("BISHNUPUR_SD"), unitMap, lgdCodeCounter++);
        insertCircle("Ningthoukhong", unitMap.get("BISHNUPUR_SD"), unitMap, lgdCodeCounter++);
        
        // Nambol
        insertCircle("Nambol", unitMap.get("NAMBOL"), unitMap, lgdCodeCounter++);
        insertCircle("Oinam", unitMap.get("NAMBOL"), unitMap, lgdCodeCounter++);
        
        // Moirang (Note: This is a Circle under Ningthoukhong Sub-Division, but we'll add it)
        AdminUnit moirangSubDiv = unitMap.get("NINGTHOUKHONG");
        if (moirangSubDiv != null) {
            insertCircle("Moirang", moirangSubDiv, unitMap, lgdCodeCounter++);
            insertCircle("Kumbi", moirangSubDiv, unitMap, lgdCodeCounter++);
        }
        
        // Kakching (Sub-Division)
        insertCircle("Kakching", unitMap.get("KAKCHING_SD"), unitMap, lgdCodeCounter++);
        insertCircle("Hiyanglam", unitMap.get("KAKCHING_SD"), unitMap, lgdCodeCounter++);
        
        // Waikhong
        insertCircle("Waikhong", unitMap.get("WAIKHONG"), unitMap, lgdCodeCounter++);
        insertCircle("Sugnu", unitMap.get("WAIKHONG"), unitMap, lgdCodeCounter++);
        
        // Jiribam (Sub-Division)
        insertCircle("Jiribam", unitMap.get("JIRIBAM_SD"), unitMap, lgdCodeCounter++);
        
        // Borobekra
        insertCircle("Borobekra", unitMap.get("BOROBEKRA"), unitMap, lgdCodeCounter++);
        
        log.info("Administrative Units initialization completed");
    }

    /**
     * Insert Sub-Division
     */
    private void insertSubDivision(String name, AdminUnit parent, Map<String, AdminUnit> unitMap, long lgdCode) {
        insertSubDivision(name, parent, unitMap, lgdCode, null);
    }

    /**
     * Insert Sub-Division with custom code
     */
    private void insertSubDivision(String name, AdminUnit parent, Map<String, AdminUnit> unitMap, long lgdCode, String customCode) {
        String code = customCode != null ? customCode : generateCode(name);
        AdminUnit subDiv = createAdminUnit(code, name, AdminUnit.UnitLevel.SUB_DIVISION, lgdCode, parent);
        subDiv = saveIfNotExists(subDiv, code);
        unitMap.put(code, subDiv);
        log.info("Inserted Sub-Division: {} (Parent: {})", name, parent.getUnitName());
    }

    /**
     * Insert Circle
     */
    private void insertCircle(String name, AdminUnit parent, Map<String, AdminUnit> unitMap, long lgdCode) {
        String code = generateCode(name);
        AdminUnit circle = createAdminUnit(code, name, AdminUnit.UnitLevel.CIRCLE, lgdCode, parent);
        circle = saveIfNotExists(circle, code);
        unitMap.put(code, circle);
        log.info("Inserted Circle: {} (Parent: {})", name, parent.getUnitName());
    }

    /**
     * Create an AdminUnit entity
     */
    private AdminUnit createAdminUnit(String code, String name, AdminUnit.UnitLevel level, long lgdCode, AdminUnit parent) {
        AdminUnit adminUnit = new AdminUnit();
        adminUnit.setUnitCode(code);
        adminUnit.setUnitName(name);
        adminUnit.setUnitLevel(level);
        adminUnit.setLgdCode(lgdCode);
        adminUnit.setParentUnit(parent);
        adminUnit.setIsActive(true);
        adminUnit.setCreatedAt(LocalDateTime.now());
        adminUnit.setUpdatedAt(LocalDateTime.now());
        return adminUnit;
    }

    /**
     * Save admin unit if it doesn't exist
     */
    private AdminUnit saveIfNotExists(AdminUnit adminUnit, String code) {
        if (!adminUnitRepository.existsByUnitCode(code)) {
            return adminUnitRepository.save(adminUnit);
        } else {
            log.debug("Admin unit already exists, skipping: {}", code);
            return adminUnitRepository.findByUnitCode(code)
                    .orElseGet(() -> adminUnit);
        }
    }

    /**
     * Generate unique code from name
     */
    private String generateCode(String name) {
        return name.toUpperCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Z0-9_]", "")
                .replaceAll("_+", "_");
    }
}

