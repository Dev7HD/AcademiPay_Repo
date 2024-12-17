package ma.dev7hd.userservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.keycloak.admin.client.Keycloak;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    private final Keycloak keycloak;

    public FeignClientConfig(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @Bean
    public RequestInterceptor keycloakAuthInterceptor() {
        return template -> {
            // Retrieve the access token from Keycloak's token manager
            String accessToken = keycloak.tokenManager().getAccessTokenString();

            // Add the Authorization header with Bearer token
            template.header("Authorization", "Bearer " + accessToken);
        };
    }
}
