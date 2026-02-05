package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.WhatsNew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsNewRepository extends JpaRepository<WhatsNew, Long> {
}
