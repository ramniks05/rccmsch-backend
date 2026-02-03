package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CalenderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalenderEventRepository extends JpaRepository<CalenderEvent, Long> {
}
