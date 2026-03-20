package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseDocument;
import in.gov.manipur.rccms.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseDocumentRepository extends JpaRepository<CaseDocument, Long> {

    @Query("""
            SELECT d FROM CaseDocument d
            WHERE d.caseId = :caseId AND d.moduleType = :moduleType
            ORDER BY d.updatedAt DESC, d.id DESC
            """)
    List<CaseDocument> findByCaseIdAndModuleTypeOrderByUpdatedAtDesc(@Param("caseId") Long caseId,
                                                                      @Param("moduleType") String moduleType);

    @Query("""
            SELECT d FROM CaseDocument d
            WHERE d.caseId = :caseId AND d.moduleType = :moduleType
            ORDER BY d.updatedAt DESC, d.id DESC
            """)
    Optional<CaseDocument> findTopByCaseIdAndModuleTypeOrderByUpdatedAtDesc(@Param("caseId") Long caseId,
                                                                             @Param("moduleType") String moduleType);

    @Query("""
            SELECT d FROM CaseDocument d
            WHERE d.caseId = :caseId AND d.templateId = :templateId
            ORDER BY d.updatedAt DESC, d.id DESC
            """)
    Optional<CaseDocument> findTopByCaseIdAndTemplateIdOrderByUpdatedAtDesc(@Param("caseId") Long caseId,
                                                                             @Param("templateId") Long templateId);

    @Query("""
            SELECT d FROM CaseDocument d
            WHERE d.caseId = :caseId AND d.templateId = :templateId
            ORDER BY d.updatedAt DESC, d.id DESC
            """)
    List<CaseDocument> findByCaseIdAndTemplateIdOrderByUpdatedAtDesc(@Param("caseId") Long caseId,
                                                                      @Param("templateId") Long templateId);

    /**
     * Check if at least one document exists for this case with any of the given templateIds
     * and any of the given statuses (e.g. DRAFT, SIGNED).
     */
    boolean existsByCaseIdAndTemplateIdInAndStatusIn(Long caseId,
                                                     List<Long> templateIds,
                                                     List<DocumentStatus> statuses);
}
