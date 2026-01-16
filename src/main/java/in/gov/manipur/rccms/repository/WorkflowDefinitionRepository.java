package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Workflow Definition Repository
 */
@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    
    Optional<WorkflowDefinition> findByWorkflowCode(String workflowCode);
    
    boolean existsByWorkflowCode(String workflowCode);
    
    List<WorkflowDefinition> findByIsActiveTrueOrderByWorkflowNameAsc();
    
    List<WorkflowDefinition> findAllByOrderByWorkflowNameAsc();
}

