package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Workflow State Repository
 */
@Repository
public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {
    
    List<WorkflowState> findByWorkflowIdOrderByStateOrderAsc(Long workflowId);
    
    Optional<WorkflowState> findByWorkflowIdAndStateCode(Long workflowId, String stateCode);
    
    Optional<WorkflowState> findByWorkflowIdAndIsInitialStateTrue(Long workflowId);
    
    List<WorkflowState> findByWorkflowIdAndIsFinalStateTrue(Long workflowId);
    
    @Query("SELECT ws FROM WorkflowState ws WHERE ws.workflowId = :workflowId ORDER BY ws.stateOrder ASC")
    List<WorkflowState> findStatesByWorkflowOrdered(@Param("workflowId") Long workflowId);
}

