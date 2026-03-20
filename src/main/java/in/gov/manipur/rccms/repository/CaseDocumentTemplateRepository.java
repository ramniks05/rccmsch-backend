package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseDocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseDocumentTemplateRepository extends JpaRepository<CaseDocumentTemplate, Long> {

    /** All active templates for permission-documents list (admin-created). */
    List<CaseDocumentTemplate> findByIsActiveTrueOrderByTemplateNameAsc();

    @Query("SELECT t FROM CaseDocumentTemplate t " +
            "WHERE t.caseNatureId = :caseNatureId AND t.caseTypeId IS NULL AND t.moduleType = :moduleType AND t.isActive = true " +
            "ORDER BY t.version DESC")
    List<CaseDocumentTemplate> findActiveTemplates(Long caseNatureId, String moduleType);

    @Query("SELECT t FROM CaseDocumentTemplate t " +
            "WHERE t.caseNatureId = :caseNatureId AND t.caseTypeId = :caseTypeId AND t.moduleType = :moduleType AND t.isActive = true " +
            "ORDER BY t.version DESC")
    List<CaseDocumentTemplate> findActiveTemplatesByCaseType(Long caseNatureId, Long caseTypeId, String moduleType);

    List<CaseDocumentTemplate> findByCaseNatureIdAndCaseTypeIdAndModuleTypeOrderByVersionDesc(
            Long caseNatureId, Long caseTypeId, String moduleType);

    List<CaseDocumentTemplate> findByCaseNatureIdAndModuleTypeOrderByVersionDesc(Long caseNatureId, String moduleType);

    Optional<CaseDocumentTemplate> findTopByCaseNatureIdAndModuleTypeAndIsActiveTrueOrderByVersionDesc(
            Long caseNatureId, String moduleType);

    Optional<CaseDocumentTemplate> findTopByCaseNatureIdAndCaseTypeIdAndModuleTypeAndIsActiveTrueOrderByVersionDesc(
            Long caseNatureId, Long caseTypeId, String moduleType);
}

