package in.gov.manipur.rccms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security Configuration
 * 
 * Configuration:
 * - JWT authentication filter for protected endpoints
 * - Public endpoints for authentication APIs
 * - Disables CSRF (stateless JWT authentication)
 * - Enables CORS for Angular frontend
 * - Stateless session management for JWT
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        // Allow all OPTIONS requests (CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints (authentication)
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/admin/auth/officer-login",
                                "/api/admin/auth/reset-password",
                                "/api/admin/auth/verify-mobile",
                                "/api/admin/auth/login",
                                "/api/health",
                                "/api/captcha/**"
                        ).permitAll()
                        // Public read-only endpoints (for frontend) - GET requests
                        .requestMatchers(HttpMethod.GET, "/api/case-types/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/case-types/**").permitAll()  // Admin read-only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/case-natures/**").permitAll()  // Public case natures endpoints
                        .requestMatchers(HttpMethod.GET, "/api/admin/case-natures/**").permitAll()  // Admin read-only case natures
                        .requestMatchers(HttpMethod.GET, "/api/public/case-types/**").permitAll()  // Public case types
                        .requestMatchers(HttpMethod.GET, "/api/public/courts/**").permitAll()  // Public courts
                        .requestMatchers(HttpMethod.GET, "/api/public/form-schemas/**").permitAll()  // Public form schemas for citizens
                        .requestMatchers(HttpMethod.GET, "/api/admin/form-schemas/case-types/**").permitAll()  // Form schemas for case types (backward compatibility)
                        .requestMatchers(HttpMethod.GET, "/api/public/form-data-sources/**").permitAll()  // Form data source endpoints
                        .requestMatchers("/api/admin/form-schemas/case-types/**").permitAll()  // Allow all methods for this path (backward compatibility)
                        .requestMatchers(HttpMethod.POST, "/api/admin/form-schemas/validate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin-units/root").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin-units/parent/**").permitAll()
                        .requestMatchers("/api/admin/system-settings").permitAll()  // System settings - GET and PUT (for frontend display and admin update)
                        .requestMatchers(HttpMethod.GET, "/api/public/registration-forms/**").permitAll()
                        // Calendar: allow all (fetch, create, update, deactivate) without auth
                        .requestMatchers("/api/calender/**").permitAll()
                        // Swagger/OpenAPI endpoints
                        .requestMatchers(
                                "/swagger-ui/**", 
                                "/swagger-ui.html", 
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**", 
                                "/swagger-resources/**", 
                                "/webjars/**"
                        ).permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Handle authentication and authorization errors
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    /**
     * Handle 401 Unauthorized (no token or invalid token)
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            errorResponse.put("error", "Unauthorized");
            errorResponse.put("message", "Authentication required. Please provide a valid JWT token.");
            errorResponse.put("path", request.getRequestURI());
            
            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
        };
    }

    /**
     * Handle 403 Forbidden (token valid but insufficient permissions)
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", HttpServletResponse.SC_FORBIDDEN);
            errorResponse.put("error", "Forbidden");
            errorResponse.put("message", "Access denied. Admin authority required. Please ensure you are logged in as admin and your token has ADMIN authority.");
            errorResponse.put("path", request.getRequestURI());
            
            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
        };
    }

    /**
     * Password Encoder Bean
     * Uses BCrypt for password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

