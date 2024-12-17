package ma.dev7hd.userservice.services;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ma.dev7hd.userservice.config.KeycloakConfig;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.User;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
public class KeycloakUserService implements IClientService {

    private final RealmResource realmResource;

    private final String DEFAULT_PASSWORD = "123456";

    public KeycloakUserService(Keycloak keycloak) {
        this.realmResource = keycloak.realm(KeycloakConfig.realm);
    }

    /**
     * Registers a user in Keycloak with roles based on their type.
     *
     * @param user the User object to be registered in Keycloak
     * @return response from Keycloak
     */
    @Override
    @Transactional
    public Response registerUserWithKeycloak(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        // Prepare Keycloak user representation
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setUsername(user.getId());
        keycloakUser.setEmail(user.getEmail());
        keycloakUser.setFirstName(user.getFirstName());
        keycloakUser.setLastName(user.getLastName());
        keycloakUser.setEnabled(true);
        keycloakUser.setEmailVerified(true);

        // Set default password
        keycloakUser.setCredentials(Collections.singletonList(createPasswordCredentials(DEFAULT_PASSWORD)));

        try {
            // Create user in Keycloak
            Response response = realmResource.users().create(keycloakUser);

            if (response.getStatus() != 201) {
                System.out.println("KC User Error: " + keycloakUser.getEmail());
                //throw new RuntimeException("Failed to create Keycloak user: " + response.getStatusInfo().toString());
            }

            // Retrieve the created user's ID
            String userId = realmResource.users().search(user.getId(), true).get(0).getId();

            // Assign roles based on user type
            List<RoleRepresentation> roles = new ArrayList<>();
            if (user instanceof Admin) {
                roles.add(realmResource.roles().get("ADMIN").toRepresentation());
            }

            realmResource.users().get(userId).roles().realmLevel().add(roles);


            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error registering user in Keycloak: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteKCUser(String userId) {
        System.out.println("Start deleting user from Keycloak...");

        try {
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> userRepresentations = usersResource.searchByUsername(userId.toLowerCase(), true);

            if (userRepresentations.isEmpty()) {
                System.err.println("No such Keycloak user with username: " + userId);
                return 0;
            }

            UserRepresentation user = userRepresentations.get(0);

            // Delete the user and revoke sessions in one operation
            Response deleteResponse = usersResource.delete(user.getId());
            if (deleteResponse.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                System.out.printf("User successfully deleted from Keycloak: Email:%s\n", user.getEmail());
                return deleteResponse.getStatus();
            } else {
                System.err.println("Failed to delete user: " + deleteResponse.getStatusInfo().getReasonPhrase());
                return deleteResponse.getStatus();
            }

        } catch (Exception e) {
            System.err.println("Error while deleting Keycloak user: " + e.getMessage());
            return 0;
        }
    }


    @Override
    public void updateKCUser(User user) {
        System.out.println("Start updating Keycloak user...");
        UsersResource usersResource = realmResource.users();

        try {
            UserRepresentation kcUser = usersResource.searchByUsername(user.getId().toLowerCase(), true).stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found in Keycloak"));

            kcUser.setFirstName(user.getFirstName());
            kcUser.setLastName(user.getLastName());
            kcUser.setEmail(user.getEmail());

            usersResource.get(kcUser.getId()).update(kcUser);
            System.out.println("User updated successfully.");
        } catch (Exception e) {
            System.err.println("Updating User information failed: " + e.getMessage());
            throw new RuntimeException("Error updating Keycloak user: " + e.getMessage(), e);
        }
    }


    @Override
    public boolean toggleKCUserAccount(String email) {
        UsersResource usersResource = realmResource.users();
        UserRepresentation kcUser = usersResource.searchByEmail(email, true).getFirst();
        kcUser.setEnabled(!kcUser.isEnabled());
        try {
            realmResource
                    .users()
                    .get(kcUser.getId())
                    .update(kcUser);
        } catch (Exception e) {
            System.err.println("Toggling user account failed: \n" + e.getMessage());
        }
        return kcUser.isEnabled();
    }

    private CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

    // RESET PASSWORD TO THE DEFAULT ONE
    @Override
    public void changeUserPassword(String userId) {
        changeKeycloakUserPassword(userId, DEFAULT_PASSWORD);
    }

    //CHANGE PASSWORD TO NEW ONE
    @Override
    public void changeUserPassword(String userEmail, String newPassword) {
        changeKeycloakUserPassword(userEmail, newPassword);
    }

    private void changeKeycloakUserPassword(String userId, String defaultPassword) {
        List<UserRepresentation> userRepresentations = realmResource.users().searchByUsername(userId, true);

        if (!userRepresentations.isEmpty()) {
            UserRepresentation userRepresentation = userRepresentations.getFirst();
            String userRepresentationId = userRepresentation.getId();
            realmResource.users()
                    .get(userRepresentationId) // User ID to reset password for
                    .resetPassword(createPasswordCredentials(defaultPassword));
        } else {
            throw new RuntimeException("Finding keycloak user failed");
        }
    }

    @Override
    public boolean verifyPassword(String email, String password) {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(KeycloakConfig.kcServerUrl)
                    .realm(KeycloakConfig.realm)
                    .clientId(KeycloakConfig.clientId)
                    .clientSecret(KeycloakConfig.clientSecret)
                    .grantType(OAuth2Constants.PASSWORD)
                    .username(email)
                    .password(password)
                    .build();

            // Attempt to get the access token to verify credentials
            AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();
            return tokenResponse != null;
        } catch (Exception e) {
            // Authentication failed
            return false;
        }
    }
}