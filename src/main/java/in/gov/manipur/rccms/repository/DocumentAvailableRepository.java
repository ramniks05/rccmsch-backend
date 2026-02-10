package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.DocumentsAvailable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentAvailableRepository extends JpaRepository<DocumentsAvailable, Long> {
}
