package ma.dev7hd.userservice.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    @Value("${user.username}")
    private String username;

    @Value("${user.pw}")
    private String password;

    @Value("${keycloak.realm}")
    public static String realm;


    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080/")       // Replace with your Keycloak server URL
                .realm(realm)                          // Use "master" for admin, or your custom realm
                .clientId(clientId)                    // "admin-cli" is typically used for admin access
                .clientSecret(clientSecret)
                .username(username)               // Admin username for Keycloak
                .password(password)               // Admin password for Keycloak
                .grantType(OAuth2Constants.PASSWORD)                    // Grant type, "password" for direct access
                .build();
    }

}
