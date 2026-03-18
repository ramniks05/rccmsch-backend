package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseAttendanceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseAttendanceSubmissionRepository extends JpaRepository<CaseAttendanceSubmission, Long> {

    Optional<CaseAttendanceSubmission> findTopByCaseIdOrderBySubmittedAtDesc(Long caseId);

    Optional<CaseAttendanceSubmission> findByCaseIdAndHearingSubmissionId(Long caseId, Long hearingSubmissionId);

    List<CaseAttendanceSubmission> findByCaseIdOrderBySubmittedAtDesc(Long caseId);

    boolean existsByCaseIdAndHearingSubmissionId(Long caseId, Long hearingSubmissionId);
}

