package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseModuleFormFieldDefinition;
import in.gov.manipur.rccms.entity.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseModuleFormFieldRepository extends JpaRepository<CaseModuleFormFieldDefinition, Long> {

    /**
     * Distinct admin-created module forms: one row per (case_nature_id, case_type_id, module_type)
     * with id = MIN(field id) for that group. Used for permission-forms list.
     */
    @Query(value = "SELECT MIN(f.id) as id, f.case_nature_id as caseNatureId, f.case_type_id as caseTypeId, " +
            "f.module_type as moduleType, cn.name as natureName, ct.type_name as typeName " +
            "FROM case_module_form_fields f " +
            "JOIN case_natures cn ON cn.id = f.case_nature_id " +
            "LEFT JOIN case_types ct ON ct.id = f.case_type_id " +
            "WHERE f.is_active = true " +
            "GROUP BY f.case_nature_id, f.case_type_id, f.module_type, cn.name, ct.type_name " +
            "ORDER BY natureName, typeName, moduleType", nativeQuery = true)
    List<ModuleFormGroupProjection> findDistinctModuleFormGroups();

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

