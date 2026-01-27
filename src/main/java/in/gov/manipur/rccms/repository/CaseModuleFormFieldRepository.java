package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseModuleFormFieldDefinition;
import in.gov.manipur.rccms.entity.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseModuleFormFieldRepository extends JpaRepository<CaseModuleFormFieldDefinition, Long> {

    @Query("SELECT f FROM CaseModuleFormFieldDefinition f " +
            "WHERE f.caseNatureId = :caseNatureId AND f.caseTypeId IS NULL AND f.moduleType = :moduleType AND f.isActive = true " +
            "ORDER BY f.displayOrder ASC")
    List<CaseModuleFormFieldDefinition> findActiveFields(Long caseNatureId, ModuleType moduleType);

    @Query("SELECT f FROM CaseModuleFormFieldDefinition f " +
            "WHERE f.caseNatureId = :caseNatureId AND f.caseTypeId = :caseTypeId AND f.moduleType = :moduleType AND f.isActive = true " +
            "ORDER BY f.displayOrder ASC")
    List<CaseModuleFormFieldDefinition> findActiveFieldsByCaseType(Long caseNatureId, Long caseTypeId, ModuleType moduleType);

    @Query("SELECT f FROM CaseModuleFormFieldDefinition f " +
            "WHERE f.caseNatureId = :caseNatureId AND f.caseTypeId IS NULL AND f.moduleType = :moduleType " +
            "ORDER BY f.displayOrder ASC")
    List<CaseModuleFormFieldDefinition> findAllFields(Long caseNatureId, ModuleType moduleType);

    @Query("SELECT f FROM CaseModuleFormFieldDefinition f " +
            "WHERE f.caseNatureId = :caseNatureId AND f.caseTypeId = :caseTypeId AND f.moduleType = :moduleType " +
            "ORDER BY f.displayOrder ASC")
    List<CaseModuleFormFieldDefinition> findAllFieldsByCaseType(Long caseNatureId, Long caseTypeId, ModuleType moduleType);

    boolean existsByCaseNatureIdAndCaseTypeIdAndModuleTypeAndFieldName(
            Long caseNatureId, Long caseTypeId, ModuleType moduleType, String fieldName);
}

