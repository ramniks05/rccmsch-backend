package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseWorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Case Workflow Instance Repository
 */
@Repository
public interface CaseWorkflowInstanceRepository extends JpaRepository<CaseWorkflowInstance, Long> {
    
    Optional<CaseWorkflowInstance> findByCaseId(Long caseId);
    
    List<CaseWorkflowInstance> findByCurrentStateId(Long currentStateId);
    
    List<CaseWorkflowInstance> findByAssignedToOfficerId(Long officerId);
    
    List<CaseWorkflowInstance> findByAssignedToUnitId(Long unitId);
    
    List<CaseWorkflowInstance> findByAssignedToRole(String roleCode);
    
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi WHERE cwi.assignedToOfficerId = :officerId " +
           "AND cwi.assignedToRole = :roleCode ORDER BY cwi.updatedAt DESC")
    List<CaseWorkflowInstance> findByAssignedOfficerAndRole(
            @Param("officerId") Long officerId, 
            @Param("roleCode") String roleCode);
    
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi WHERE cwi.assignedToUnitId = :unitId " +
           "AND cwi.currentStateId = :stateId")
    List<CaseWorkflowInstance> findByUnitAndState(@Param("unitId") Long unitId, @Param("stateId") Long stateId);
}

