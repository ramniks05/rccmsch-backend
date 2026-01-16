package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Workflow Transition Repository
 */
@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    
    List<WorkflowTransition> findByWorkflowIdAndIsActiveTrue(Long workflowId);
    
    List<WorkflowTransition> findByFromStateIdAndIsActiveTrue(Long fromStateId);
    
    Optional<WorkflowTransition> findByWorkflowIdAndTransitionCode(Long workflowId, String transitionCode);
    
    @Query("SELECT wt FROM WorkflowTransition wt WHERE wt.fromStateId = :fromStateId AND wt.isActive = true")
    List<WorkflowTransition> findAvailableTransitionsFromState(@Param("fromStateId") Long fromStateId);
    
    @Query("SELECT wt FROM WorkflowTransition wt WHERE wt.workflowId = :workflowId AND wt.fromStateId = :fromStateId AND wt.isActive = true")
    List<WorkflowTransition> findTransitionsFromState(@Param("workflowId") Long workflowId, @Param("fromStateId") Long fromStateId);
}

