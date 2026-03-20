package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseModuleFormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseModuleFormSubmissionRepository extends JpaRepository<CaseModuleFormSubmission, Long> {

    List<CaseModuleFormSubmission> findByCaseIdAndModuleType(Long caseId, String moduleType);

    List<CaseModuleFormSubmission> findByCaseIdAndHearingSubmissionIdOrderBySubmittedAtDesc(Long caseId, Long hearingSubmissionId);

    @Query("""
            SELECT s FROM CaseModuleFormSubmission s
            WHERE s.caseId = :caseId AND s.moduleType = :moduleType
            ORDER BY s.submittedAt DESC, s.id DESC
            """)
    Optional<CaseModuleFormSubmission> findTopByCaseIdAndModuleTypeOrderBySubmittedAtDesc(
            @Param("caseId") Long caseId, @Param("moduleType") String moduleType);
}

