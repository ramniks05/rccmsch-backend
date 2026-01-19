package in.gov.manipur.rccms.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

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
                        .requestMatchers(HttpMethod.GET, "/api/admin/form-schemas/case-types/**").permitAll()
                        .requestMatchers("/api/admin/form-schemas/case-types/**").permitAll()  // Allow all methods for this path
                        .requestMatchers(HttpMethod.POST, "/api/admin/form-schemas/validate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/system-settings").permitAll()  // Public system settings
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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

