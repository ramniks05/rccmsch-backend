package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OTP Repository interface
 * Provides data access operations for OTP entities
 */
@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    /**
     * Find valid OTP by mobile number, OTP code, and citizen type
     */
    @Query("SELECT o FROM Otp o WHERE o.mobileNumber = :mobileNumber " +
           "AND o.otpCode = :otpCode AND o.citizenType = :citizenType " +
           "AND o.isUsed = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<Otp> findValidOtpByMobileNumber(
            @Param("mobileNumber") String mobileNumber,
            @Param("otpCode") String otpCode,
            @Param("citizenType") Otp.CitizenType citizenType,
            @Param("now") LocalDateTime now);

    /**
     * Count OTP requests in last 15 minutes for rate limiting
     */
    @Query("SELECT COUNT(o) FROM Otp o WHERE o.mobileNumber = :mobileNumber " +
           "AND o.citizenType = :citizenType AND o.createdAt > :since")
    long countRecentOtpRequests(
            @Param("mobileNumber") String mobileNumber,
            @Param("citizenType") Otp.CitizenType citizenType,
            @Param("since") LocalDateTime since);

    /**
     * Mark OTP as used
     */
    @Modifying
    @Query("UPDATE Otp o SET o.isUsed = true WHERE o.id = :id")
    void markAsUsed(@Param("id") Long id);

    /**
     * Delete expired OTPs
     */
    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
}

