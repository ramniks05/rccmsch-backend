package in.gov.manipur.rccms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS Configuration
 * Enables Cross-Origin Resource Sharing for Angular frontend
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow Angular frontend origins (localhost ports 4200 and 4201)
        // Add more origins as needed for your frontend
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",  // Angular default dev port
                "http://localhost:4201",  // Alternative Angular dev port
                "http://localhost:3000",  // React default dev port
                "http://localhost:5173",  // Vite default dev port
                "http://127.0.0.1:4200",  // Alternative localhost format
                "http://127.0.0.1:3000"   // Alternative localhost format
        ));
        
        // Allow all HTTP methods (including OPTIONS for preflight)
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // Allow all headers (must specify explicitly when allowCredentials is true)
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "X-User-Id",  // Custom header for citizen user ID
                "x-user-id",  // Lowercase variant (browsers may send lowercase)
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Cache-Control",
                "Pragma"
        ));
        
        // Expose headers that frontend can access
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

