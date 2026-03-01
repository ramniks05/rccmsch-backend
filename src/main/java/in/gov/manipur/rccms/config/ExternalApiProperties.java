package in.gov.manipur.rccms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for external APIs used by form data sources (e.g. CHD Revenue API).
 * Each API has a key (e.g. chd-revenue) and config: baseUrl, loginPath, credentials, token cache.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.external-apis")
public class ExternalApiProperties {

    /** When false, SSL certificate verification is disabled for external API calls (use only when server cert is not in Java truststore). */
    private boolean sslVerify = true;

    /**
     * Map of API config key -> API settings (baseUrl, loginPath, username, password, etc.)
     */
    private Map<String, ExternalApiConfig> apis = new HashMap<>();

    @Data
    public static class ExternalApiConfig {
        private String baseUrl;
        private String loginPath = "/rccmsapi/UserLogin";
        private String username;
        private String password;
        private int tokenCacheTtlMinutes = 50;
        /** JSON path to token in login response, e.g. "token" or "data.accessToken" */
        private String tokenResponsePath = "token";
    }
}
