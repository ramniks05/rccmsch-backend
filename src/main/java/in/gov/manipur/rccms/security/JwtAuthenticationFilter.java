package in.gov.manipur.rccms.security;

import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import in.gov.manipur.rccms.repository.OfficerDaHistoryRepository;
import in.gov.manipur.rccms.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT Authentication Filter
 * Validates JWT tokens and sets authentication context
 * Also validates that post-based tokens have active postings
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final OfficerDaHistoryRepository postingRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip JWT filter for public endpoints
        if (shouldSkipFilter(requestPath, request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                log.debug("JWT token found for path: {}", requestPath);
                String username = extractUsernameFromToken(token);
                
                if (jwtService.validateToken(token, username)) {
                    // Extract auth type from token
                    String authType = jwtService.extractAllClaims(token).get("authType", String.class);
                    log.debug("Token validated. AuthType: {}, Username: {}", authType, username);
                    
                    // For post-based authentication, validate posting is still active
                    if ("POST_BASED".equals(authType)) {
                        validatePostingIsActive(token);
                    }
                    
                    // Set authentication context
                    setAuthenticationContext(token, request);
                    log.debug("Authentication context set successfully");
                } else {
                    log.warn("JWT token validation failed for path: {}", requestPath);
                }
            } else {
                log.debug("No JWT token found in request for path: {}", requestPath);
            }
        } catch (InvalidCredentialsException e) {
            log.warn("JWT authentication failed - posting validation: {}", e.getMessage());
            // Clear security context
            SecurityContextHolder.clearContext();
            // Return 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + e.getMessage() + "\"}");
            return;
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            // Clear security context on any error
            SecurityContextHolder.clearContext();
            // Continue filter chain - let endpoint handle unauthorized access
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Check if filter should be skipped for public endpoints
     */
    private boolean shouldSkipFilter(String path, String method) {
        // Public authentication endpoints
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.startsWith("/api/admin/auth/")) {
            return true;
        }
        if (path.equals("/api/health")) {
            return true;
        }
        if (path.startsWith("/api/captcha/")) {
            return true;
        }
        
        // Public GET endpoints
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/case-types")) {
                return true;
            }
            if (path.startsWith("/api/admin/case-types")) {
                return true;
            }
            if (path.startsWith("/api/admin/form-schemas/case-types/")) {
                return true;
            }
            if (path.equals("/api/admin/system-settings")) {
                return true;
            }
        }
        
        // Public POST endpoints
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/admin/form-schemas/validate")) {
            return true;
        }
        
        // Swagger endpoints
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || 
            path.startsWith("/swagger-resources") || path.startsWith("/webjars")) {
            return true;
        }
        
        // OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        return false;
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Extract username from token (subject)
     */
    private String extractUsernameFromToken(String token) {
        return jwtService.extractUsername(token);
    }

    /**
     * Validate that posting is still active for post-based tokens
     */
    private void validatePostingIsActive(String token) {
        try {
            var claims = jwtService.extractAllClaims(token);
            String userid = claims.get("userid", String.class);
            
            if (userid != null) {
                // Check if posting is still active
                boolean isActive = postingRepository.findByPostingUseridAndIsCurrentTrue(userid).isPresent();
                
                if (!isActive) {
                    log.warn("Posting validation failed: UserID {} is no longer active", userid);
                    throw new InvalidCredentialsException("Your posting has been transferred. Please login again.");
                }
            }
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating posting: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid posting information");
        }
    }

    /**
     * Set Spring Security authentication context
     */
    private void setAuthenticationContext(String token, HttpServletRequest request) {
        try {
            var claims = jwtService.extractAllClaims(token);
            String username = jwtService.extractUsername(token);
            String authType = claims.get("authType", String.class);
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            
            // Add authorities based on auth type
            if ("POST_BASED".equals(authType)) {
                String roleCode = claims.get("roleCode", String.class);
                if (roleCode != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
                }
            } else if ("ADMIN".equals(authType)) {
                // For admin, add both ADMIN authority and role-based authority
                authorities.add(new SimpleGrantedAuthority("ADMIN"));
                String role = claims.get("role", String.class);
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            } else {
                // Citizen/Lawyer authentication
                String userType = claims.get("userType", String.class);
                if ("LAWYER".equalsIgnoreCase(userType)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_LAWYER"));
                } else if ("OPERATOR".equalsIgnoreCase(userType)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_CITIZEN"));
                }
            }
            
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("Error setting authentication context: {}", e.getMessage());
        }
    }
}

