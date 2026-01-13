package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.OfficerDaHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Officer DA History Repository
 * JPA repository for OfficerDaHistory entity (Posting History)
 */
@Repository
public interface OfficerDaHistoryRepository extends JpaRepository<OfficerDaHistory, Long> {

    /**
     * Find posting by UserID (generated format: ROLE@LGD)
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.postingUserid = :userid")
    Optional<OfficerDaHistory> findByPostingUserid(@Param("userid") String userid);

    /**
     * Find active posting by UserID
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.postingUserid = :userid AND p.isCurrent = true")
    Optional<OfficerDaHistory> findByPostingUseridAndIsCurrentTrue(@Param("userid") String userid);

    /**
     * Find active posting by unit and role
     */
    Optional<OfficerDaHistory> findByUnitIdAndRoleCodeAndIsCurrentTrue(Long unitId, String roleCode);

    /**
     * Find all active postings by unit
     */
    List<OfficerDaHistory> findByUnitIdAndIsCurrentTrue(Long unitId);

    /**
     * Find all postings by officer (person)
     */
    List<OfficerDaHistory> findByOfficerIdOrderByFromDateDesc(Long officerId);

    /**
     * Find all active postings by officer
     */
    List<OfficerDaHistory> findByOfficerIdAndIsCurrentTrue(Long officerId);

    /**
     * Find all postings by unit
     */
    List<OfficerDaHistory> findByUnitIdOrderByFromDateDesc(Long unitId);

    /**
     * Find all postings by role code
     */
    List<OfficerDaHistory> findByRoleCodeAndIsCurrentTrueOrderByFromDateDesc(String roleCode);

    /**
     * Check if active posting exists for unit and role
     */
    boolean existsByUnitIdAndRoleCodeAndIsCurrentTrue(Long unitId, String roleCode);

    /**
     * Find postings that need to be closed (for transfer logic)
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.unitId = :unitId AND p.roleCode = :roleCode AND p.isCurrent = true")
    List<OfficerDaHistory> findActivePostingsByUnitAndRole(@Param("unitId") Long unitId, @Param("roleCode") String roleCode);

    /**
     * Find all active postings
     */
    List<OfficerDaHistory> findByIsCurrentTrueOrderByFromDateDesc();
}

