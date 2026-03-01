package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.config.ExternalApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;

/**
 * Service to call external APIs that require token-based authentication.
 * Performs login (username/password) to obtain a token, caches it, and uses it for data API calls.
 * Used by form data sources when dataSource.type = "API" and apiConfigKey points to a configured API (e.g. chd-revenue).
 */
@Slf4j
@Service
public class ExternalApiClientService {

    private final RestTemplate restTemplate;
    private final ExternalApiProperties externalApiProperties;
    private final ObjectMapper objectMapper;

    public ExternalApiClientService(
            @Qualifier("externalApiRestTemplate") RestTemplate restTemplate,
            ExternalApiProperties externalApiProperties,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.externalApiProperties = externalApiProperties;
        this.objectMapper = objectMapper;
    }

    /** Cache: apiConfigKey -> (token, expiresAt) */
    private final Map<String, CachedToken> tokenCache = new HashMap<>();

    /**
     * Fetch data from an external API. Ensures we have a valid token (login if needed), then GET the data endpoint.
     *
     * @param apiConfigKey   key from app.external-apis.apis (e.g. "chd-revenue")
     * @param dataPath       path relative to baseUrl (e.g. "/rccmsapi/SomeEndpoint")
     * @param queryParams    optional query params
     * @return list of option maps (e.g. [{id, code, name}, ...]); if API returns array, that is returned; if wrapper object with "data" or "result", array inside is extracted
     */
    public List<Map<String, Object>> fetchData(String apiConfigKey, String dataPath, Map<String, String> queryParams) {
        ExternalApiProperties.ExternalApiConfig config = getConfig(apiConfigKey);
        String token = getOrRefreshToken(apiConfigKey, config);
        String url = buildUrl(config.getBaseUrl(), dataPath, queryParams);

        log.info("External API call: GET {} (apiConfigKey={}, queryParams={})", url, apiConfigKey, queryParams);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
        } catch (Exception e) {
            log.error("External API request failed: GET {} - {}", url, e.getMessage());
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("External API {} returned {} for {} | body: {}", apiConfigKey, response.getStatusCode(), dataPath,
                    response.getBody() != null ? response.getBody().substring(0, Math.min(500, response.getBody().length())) : "null");
            return Collections.emptyList();
        }

        String responseBody = response.getBody();
        if (log.isDebugEnabled()) {
            log.debug("External API response (first 500 chars): {}", responseBody == null ? "null" : responseBody.substring(0, Math.min(500, responseBody.length())));
        }
        List<Map<String, Object>> result = parseDataResponse(responseBody);
        log.info("External API returned {} items for {}", result != null ? result.size() : 0, url);
        return result;
    }

    /**
     * Get or refresh token for the given API config. Caches token and refreshes when expired.
     */
    public String getOrRefreshToken(String apiConfigKey, ExternalApiProperties.ExternalApiConfig config) {
        CachedToken cached = tokenCache.get(apiConfigKey);
        if (cached != null && cached.expiresAt.isAfter(Instant.now())) {
            return cached.token;
        }

        String token = login(config);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Failed to obtain token from external API: " + apiConfigKey);
        }

        long ttlSeconds = config.getTokenCacheTtlMinutes() * 60L;
        tokenCache.put(apiConfigKey, new CachedToken(token, Instant.now().plusSeconds(ttlSeconds)));
        log.info("Cached token for external API: {}", apiConfigKey);
        return token;
    }

    /**
     * Perform login and return the token from the response.
     */
    public String login(ExternalApiProperties.ExternalApiConfig config) {
        String url = config.getBaseUrl().replaceAll("/$", "") + config.getLoginPath();
        Map<String, String> body = new HashMap<>();
        body.put("username", config.getUsername());
        body.put("password", config.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            log.info("External API login: POST {} (apiConfigKey from config)", url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("External API login failed for {}: {} | body: {}", url, response.getStatusCode(),
                        response.getBody() != null ? response.getBody().substring(0, Math.min(300, response.getBody().length())) : "null");
                return null;
            }

            String token = extractToken(response.getBody(), config.getTokenResponsePath());
            log.info("External API login successful for {}", url);
            return token;
        } catch (Exception e) {
            log.error("External API login request failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    private ExternalApiProperties.ExternalApiConfig getConfig(String apiConfigKey) {
        Map<String, ExternalApiProperties.ExternalApiConfig> apis = externalApiProperties.getApis();
        ExternalApiProperties.ExternalApiConfig config = apis != null ? apis.get(apiConfigKey) : null;
        if (config == null || config.getBaseUrl() == null || config.getUsername() == null) {
            throw new IllegalArgumentException("Unknown or incomplete external API config: " + apiConfigKey);
        }
        return config;
    }

    private String buildUrl(String baseUrl, String path, Map<String, String> queryParams) {
        String full = baseUrl.replaceAll("/$", "") + (path.startsWith("/") ? path : "/" + path);
        if (queryParams != null && !queryParams.isEmpty()) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(full);
            queryParams.forEach(builder::queryParam);
            full = builder.toUriString();
        }
        return full;
    }

    /**
     * Extract token from login response JSON using a path like "token" or "data.accessToken".
     */
    private String extractToken(String json, String path) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            if (path == null || path.isBlank()) {
                return getString(root, "token") != null ? getString(root, "token") : getString(root, "accessToken");
            }
            String[] parts = path.split("\\.");
            JsonNode node = root;
            for (String part : parts) {
                if (node == null) return null;
                node = node.get(part);
            }
            return node != null && !node.isNull() ? node.asText() : null;
        } catch (Exception e) {
            log.debug("Token extraction failed, trying common keys: {}", e.getMessage());
            try {
                JsonNode root = objectMapper.readTree(json);
                return getString(root, "token") != null ? getString(root, "token")
                        : getString(root, "accessToken") != null ? getString(root, "accessToken")
                        : getNestedString(root, "data", "token") != null ? getNestedString(root, "data", "token")
                        : getNestedString(root, "data", "accessToken") != null ? getNestedString(root, "data", "accessToken")
                        : null;
            } catch (Exception e2) {
                log.warn("Could not parse login response: {}", e2.getMessage());
                return null;
            }
        }
    }

    private String getString(JsonNode node, String key) {
        JsonNode n = node.get(key);
        return n != null && !n.isNull() ? n.asText() : null;
    }

    private String getNestedString(JsonNode node, String key1, String key2) {
        JsonNode n = node.get(key1);
        return n != null ? getString(n, key2) : null;
    }

    /**
     * Parse API response into list of option maps. Handles:
     * - Array at root or wrapped in "data"/"result"/"items" (dropdown options).
     * - Single object at root or in "data" (on-change detail); returns one-element list so frontend can use data[0].
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseDataResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode array = root.isArray() ? root : root.get("data");
            if (array == null) array = root.get("result");
            if (array == null) array = root.get("items");
            if (array != null && array.isArray()) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode item : array) {
                    list.add(objectMapper.convertValue(item, Map.class));
                }
                return list;
            }
            // Single object (e.g. on-change detail: GetOwnerDetailsByMustKhas) – return as one-element list
            JsonNode dataNode = root.get("data");
            JsonNode single = (dataNode != null && dataNode.isObject()) ? dataNode : (root.isObject() ? root : null);
            if (single != null && single.isObject()) {
                Map<String, Object> map = objectMapper.convertValue(single, Map.class);
                return Collections.singletonList(map);
            }
            log.warn("External API response was not array or single object at root/data");
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to parse external API data response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static class CachedToken {
        final String token;
        final Instant expiresAt;

        CachedToken(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }
    }
}
