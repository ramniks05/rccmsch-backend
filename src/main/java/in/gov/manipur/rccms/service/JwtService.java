package in.gov.manipur.rccms.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service for token generation and validation
 * Handles JWT token creation, parsing, and validation
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret:MySecretKeyForJWTTokenGeneration12345678901234567890}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:3600000}") // 1 hour in milliseconds
    private Long jwtExpiration;

    @Value("${app.jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private Long refreshTokenExpiration;

    /**
     * Generate JWT access token for a user
     * @param userId the user ID
     * @param username the username (mobile/email)
     * @param userType the user type
     * @return JWT token string
     */
    public String generateToken(Long userId, String username, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", userType);
        claims.put("type", "access");
        return createToken(claims, username, jwtExpiration);
    }

    /**
     * Generate refresh token
     * @param userId the user ID
     * @param username the username
     * @return Refresh token string
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return createToken(claims, username, refreshTokenExpiration);
    }

    /**
     * Generate JWT token for post-based authentication
     * @param userid the UserID (ROLE@LGD format)
     * @param roleCode the role code
     * @param roleId the role_master id (acting role)
     * @param unitId the unit ID
     * @param unitLevel the unit level
     * @param userId the user (person) ID
     * @return JWT token string
     */
    public String generatePostBasedToken(String userid, String roleCode, Long roleId, Long unitId, String unitLevel, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userid", userid);
        claims.put("roleCode", roleCode);
        claims.put("roleId", roleId != null ? roleId : 0L);
        claims.put("unitId", unitId);
        claims.put("unitLevel", unitLevel);
        claims.put("userId", userId);
        claims.put("type", "access");
        claims.put("authType", "POST_BASED");
        return createToken(claims, userid, jwtExpiration);
    }

    /**
     * Generate JWT token for admin authentication
     * @param username the admin username
     * @param role the admin role (e.g., SUPER_ADMIN)
     * @return JWT token string
     */
    public String generateAdminToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 0L); // Admin user ID
        claims.put("role", role);
        claims.put("type", "access");
        claims.put("authType", "ADMIN");
        return createToken(claims, username, jwtExpiration);
    }

    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username (mobile number) from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extract user type from token
     */
    public String extractUserType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userType", String.class);
    }

    /**
     * Extract token type (access or refresh)
     */
    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("type", String.class);
    }

    /**
     * Validate refresh token
     */
    public Boolean validateRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String type = claims.get("type", String.class);
            return "refresh".equals(type) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token
     * @param token the JWT token
     * @param username the username (mobile number or email) to validate against
     * @return true if token is valid
     */
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 32 bytes for HS256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

