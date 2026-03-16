package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseDocument;
import in.gov.manipur.rccms.entity.DocumentStatus;
import in.gov.manipur.rccms.entity.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseDocumentRepository extends JpaRepository<CaseDocument, Long> {

    List<CaseDocument> findByCaseIdAndModuleTypeOrderByUpdatedAtDesc(Long caseId, ModuleType moduleType);

    Optional<CaseDocument> findTopByCaseIdAndModuleTypeOrderByUpdatedAtDesc(Long caseId, ModuleType moduleType);

    Optional<CaseDocument> findTopByCaseIdAndTemplateIdOrderByUpdatedAtDesc(Long caseId, Long templateId);

    List<CaseDocument> findByCaseIdAndTemplateIdOrderByUpdatedAtDesc(Long caseId, Long templateId);

    /**
     * Check if at least one document exists for this case with any of the given templateIds
     * and any of the given statuses (e.g. DRAFT, FINAL, SIGNED).
     */
    boolean existsByCaseIdAndTemplateIdInAndStatusIn(Long caseId,
                                                     List<Long> templateIds,
                                                     List<DocumentStatus> statuses);
}
