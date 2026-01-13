package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.Officer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Officer Repository
 * JPA repository for Officer entity (Government Employee)
 */
@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {

    /**
     * Find officer by mobile number
     */
    Optional<Officer> findByMobileNo(String mobileNo);

    /**
     * Find officer by email
     */
    Optional<Officer> findByEmail(String email);

    /**
     * Check if officer exists by mobile number
     */
    boolean existsByMobileNo(String mobileNo);

    /**
     * Check if officer exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Find all active officers
     */
    List<Officer> findByIsActiveTrueOrderByFullNameAsc();

    /**
     * Find all officers ordered by name
     */
    List<Officer> findAllByOrderByFullNameAsc();
}

