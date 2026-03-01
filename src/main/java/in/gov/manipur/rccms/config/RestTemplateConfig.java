package in.gov.manipur.rccms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;

/**
 * RestTemplate configuration for outbound HTTP calls (e.g. external API integration).
 * Provides a dedicated RestTemplate for external APIs that can disable SSL verification when the
 * server certificate is not in Java's truststore (e.g. self-signed or internal CA).
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        return new RestTemplate(factory);
    }

    /**
     * RestTemplate for external API (CHD Revenue etc.). When app.external-apis.ssl-verify is false,
     * skips SSL certificate verification so calls to servers with self-signed or internal CA certs succeed.
     */
    @Bean(name = "externalApiRestTemplate")
    public RestTemplate externalApiRestTemplate(ExternalApiProperties externalApiProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        if (!externalApiProperties.isSslVerify()) {
            log.warn("External API SSL verification is DISABLED (app.external-apis.ssl-verify=false). Use only for dev/internal.");
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }}, new java.security.SecureRandom());
                javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                SimpleClientHttpRequestFactory customFactory = new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                        super.prepareConnection(connection, httpMethod);
                        if (connection instanceof HttpsURLConnection) {
                            HttpsURLConnection https = (HttpsURLConnection) connection;
                            https.setSSLSocketFactory(sslSocketFactory);
                            https.setHostnameVerifier((hostname, session) -> true);
                        }
                    }
                };
                customFactory.setConnectTimeout(10000);
                customFactory.setReadTimeout(30000);
                return new RestTemplate(customFactory);
            } catch (Exception e) {
                throw new IllegalStateException("Could not create SSL-relaxed RestTemplate for external API", e);
            }
        }
        return new RestTemplate(factory);
    }
}
