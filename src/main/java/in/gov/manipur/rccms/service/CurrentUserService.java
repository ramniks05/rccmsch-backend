package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.entity.Officer;
import in.gov.manipur.rccms.entity.OfficerDaHistory;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.repository.OfficerDaHistoryRepository;
import in.gov.manipur.rccms.repository.OfficerRepository;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Current User Service
 * Extracts current user information from JWT token or SecurityContext
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final JwtService jwtService;
    private final OfficerRepository officerRepository;
    private final OfficerDaHistoryRepository postingRepository;
    private final RoleMasterRepository roleMasterRepository;

    /**
     * Extract JWT token from request
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Get current officer ID from JWT token
     */
    public Long getCurrentOfficerId(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Claims claims = jwtService.extractAllClaims(token);
                String authType = claims.get("authType", String.class);
                
                if ("POST_BASED".equals(authType)) {
                    return claims.get("userId", Long.class);
                } else if ("ADMIN".equals(authType)) {
                    // Admin doesn't have officer ID
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Error extracting officer ID from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get current role ID (role_master id) from JWT or resolve from role code.
     * Used by workflow for permission checks (acting role is role_id).
     */
    public Long getCurrentRoleId(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Claims claims = jwtService.extractAllClaims(token);
                String authType = claims.get("authType", String.class);
                if ("POST_BASED".equals(authType)) {
                    Long roleId = claims.get("roleId", Long.class);
                    if (roleId != null && roleId != 0L) return roleId;
                }
                String roleCode = getCurrentRoleCode(request);
                if (roleCode != null) {
                    return roleMasterRepository.findByRoleCode(roleCode)
                            .map(RoleMaster::getId)
                            .orElse(null);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting role ID from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get current role code from JWT token
     */
    public String getCurrentRoleCode(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Claims claims = jwtService.extractAllClaims(token);
                String authType = claims.get("authType", String.class);
                
                if ("POST_BASED".equals(authType)) {
                    return claims.get("roleCode", String.class);
                } else if ("ADMIN".equals(authType)) {
                    return claims.get("role", String.class);
                } else {
                    // Citizen/Lawyer authentication - check userType
                    String userType = claims.get("userType", String.class);
                    if ("LAWYER".equalsIgnoreCase(userType)) {
                        return "LAWYER";
                    } else if ("OPERATOR".equalsIgnoreCase(userType)) {
                        return "OPERATOR";
                    } else {
                        return "CITIZEN";
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting role code from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get current unit ID from JWT token
     */
    public Long getCurrentUnitId(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Claims claims = jwtService.extractAllClaims(token);
                String authType = claims.get("authType", String.class);
                
                if ("POST_BASED".equals(authType)) {
                    return claims.get("unitId", Long.class);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting unit ID from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get current officer from JWT token
     */
    public Officer getCurrentOfficer(HttpServletRequest request) {
        Long officerId = getCurrentOfficerId(request);
        if (officerId != null) {
            return officerRepository.findById(officerId).orElse(null);
        }
        return null;
    }

    /**
     * Get current posting from JWT token
     */
    public OfficerDaHistory getCurrentPosting(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Claims claims = jwtService.extractAllClaims(token);
                String authType = claims.get("authType", String.class);
                
                if ("POST_BASED".equals(authType)) {
                    String userid = claims.get("userid", String.class);
                    if (userid != null) {
                        return postingRepository.findByPostingUseridAndIsCurrentTrue(userid).orElse(null);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting posting from token: {}", e.getMessage());
        }
        return null;
    }
}

