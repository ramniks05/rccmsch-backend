package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseModuleFormSubmission;
import in.gov.manipur.rccms.entity.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseModuleFormSubmissionRepository extends JpaRepository<CaseModuleFormSubmission, Long> {

    List<CaseModuleFormSubmission> findByCaseIdAndModuleType(Long caseId, ModuleType moduleType);

    Optional<CaseModuleFormSubmission> findTopByCaseIdAndModuleTypeOrderBySubmittedAtDesc(
            Long caseId, ModuleType moduleType);
}

