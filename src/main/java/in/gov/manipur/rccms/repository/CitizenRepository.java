package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Citizen Repository interface
 * Provides data access operations for Citizen entities
 */
@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {

    /**
     * Find citizen by email
     */
    Optional<Citizen> findByEmail(String email);

    /**
     * Find citizen by mobile number
     */
    Optional<Citizen> findByMobileNumber(String mobileNumber);

    /**
     * Find citizen by Aadhar number
     */
    /**
     * Find citizen by email or mobile number
     */
    Optional<Citizen> findByEmailOrMobileNumber(String email, String mobileNumber);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if mobile number exists
     */
    boolean existsByMobileNumber(String mobileNumber);

    /**
     * Check if Aadhar number exists
     */

    /**
     * Find active citizen by email or mobile number
     */
    Optional<Citizen> findByEmailOrMobileNumberAndIsActiveTrue(String email, String mobileNumber);
}

