package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseDocument;
import in.gov.manipur.rccms.entity.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseDocumentRepository extends JpaRepository<CaseDocument, Long> {

    List<CaseDocument> findByCaseIdAndModuleTypeOrderByUpdatedAtDesc(Long caseId, ModuleType moduleType);

    Optional<CaseDocument> findTopByCaseIdAndModuleTypeOrderByUpdatedAtDesc(Long caseId, ModuleType moduleType);
}

