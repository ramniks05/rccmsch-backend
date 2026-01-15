package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.WorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Workflow History Repository
 */
@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {
    
    List<WorkflowHistory> findByCaseIdOrderByPerformedAtDesc(Long caseId);
    
    List<WorkflowHistory> findByInstanceIdOrderByPerformedAtDesc(Long instanceId);
    
    List<WorkflowHistory> findByPerformedByOfficerIdOrderByPerformedAtDesc(Long officerId);
    
    @Query("SELECT wh FROM WorkflowHistory wh WHERE wh.caseId = :caseId ORDER BY wh.performedAt DESC")
    List<WorkflowHistory> findCaseHistory(@Param("caseId") Long caseId);
    
    @Query("SELECT wh FROM WorkflowHistory wh WHERE wh.transitionId = :transitionId ORDER BY wh.performedAt DESC")
    List<WorkflowHistory> findByTransition(@Param("transitionId") Long transitionId);
}

