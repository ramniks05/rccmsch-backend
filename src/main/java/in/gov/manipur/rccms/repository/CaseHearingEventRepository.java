package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseHearingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseHearingEventRepository extends JpaRepository<CaseHearingEvent, Long> {

    Optional<CaseHearingEvent> findByCaseIdAndIsCurrentTrue(Long caseId);

    List<CaseHearingEvent> findByCaseIdOrderByHearingNoDesc(Long caseId);

    Optional<CaseHearingEvent> findTopByCaseIdOrderByHearingNoDesc(Long caseId);

    List<CaseHearingEvent> findByCaseIdInAndIsCurrentTrue(List<Long> caseIds);
}
