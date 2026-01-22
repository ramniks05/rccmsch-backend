package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.Act;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Act Repository
 * JPA repository for Act entity
 */
@Repository
public interface ActRepository extends JpaRepository<Act, Long> {

    /**
     * Find act by code
     */
    Optional<Act> findByActCode(String actCode);

    /**
     * Check if act exists by code
     */
    boolean existsByActCode(String actCode);

    /**
     * Find all active acts
     */
    List<Act> findByIsActiveTrueOrderByActNameAsc();
}
