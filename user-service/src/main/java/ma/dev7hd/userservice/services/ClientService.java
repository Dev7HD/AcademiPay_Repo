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

import java.util.Collections;
import java.util.List;


@Service
public class ClientService implements IClientService {

    private final RealmResource realmResource;

    private final String DEFAULT_PASSWORD = "123456";

    public ClientService(Keycloak keycloak) {
        this.realmResource = keycloak.realm(KeycloakConfig.realm);
    }

    @Override
    @Transactional
    public void registerUserWithKeycloak(User savedUser) {

        // Register client as Keycloak user
        UserRepresentation keycloakUser = new UserRepresentation();
        assert savedUser != null;
        keycloakUser.setUsername(savedUser.getId());
        keycloakUser.setEmailVerified(true);
        keycloakUser.setEmail(savedUser.getEmail());
        keycloakUser.setFirstName(savedUser.getFirstName());
        keycloakUser.setLastName(savedUser.getLastName());
        keycloakUser.setEnabled(true);

        RoleRepresentation adminRole = realmResource.roles().get("ADMIN").toRepresentation();
        RoleRepresentation studentRole = realmResource.roles().get("STUDENT").toRepresentation();

        // Password and other attributes can be set here
        CredentialRepresentation passwordCredentials = createPasswordCredentials(DEFAULT_PASSWORD);
        keycloakUser.setCredentials(Collections.singletonList(passwordCredentials));


        // Add user to Keycloak
        Response response = null;

        try {
            response = realmResource.users().create(keycloakUser);

            UsersResource usersResource = realmResource.users();
            UserRepresentation userRepresentation = usersResource.searchByEmail(savedUser.getEmail(), true).getFirst(); // Replace with appropriate search
            String userId = userRepresentation.getId();

            if (savedUser instanceof Admin) {
                usersResource.get(userId).roles().realmLevel().add(List.of(adminRole, studentRole));
            } else {
                usersResource.get(userId).roles().realmLevel().add(List.of(studentRole));
                //List<RoleRepresentation> assignedRoles = usersResource.get(userId).roles().realmLevel().listAll();
            }

        } catch (Exception e) {
            System.err.println("Creating client error: " + e.getMessage());
        }

        if (response == null || response.getStatus() != 201) {
            System.err.println("Couldn't create Keycloak user.");
        } else {
            System.out.println("Keycloak user created.... verify in keycloak!");
        }

    }

    @Override
    public void deleteKCUser(String userId) {

        System.out.println("Start deleting user from Keycloak...");

        UsersResource usersResource = realmResource.users();
        List<UserRepresentation> userRepresentations = usersResource.searchByUsername(userId.toLowerCase(), true);
        if (userRepresentations.isEmpty()){
            System.err.println("No such keycloak user with username: " + userId);
            return;
        }

        UserRepresentation user = userRepresentations.getFirst();

        System.out.println("User found in Keycloak...");
        System.out.println("User email: " + user.getEmail());
        System.out.println("User id: " + user.getId());

        try {
            // Delete the user from Keycloak
            realmResource.users()
                    .delete(user.getId());
            System.out.println("User has been deleted.");

            // Log out the user by revoking sessions
            realmResource.users()
                    .get(user.getId())
                    .logout();
            System.out.println("User has been logged out.");
        } catch (Exception e) {
            System.err.println("Couldn't delete Keycloak user: " + e.getMessage());
        }
    }

    @Override
    public void updateKCUser(User user) {
        System.out.println("Start updating Keycloak user...");
        UsersResource usersResource = realmResource.users();
        UserRepresentation kcUser = usersResource.searchByEmail(user.getEmail(), true).getFirst();
        System.out.println("User found in Keycloak...");
        System.out.println("User email: " + user.getEmail());
        System.out.println("User id: " + user.getId());

        kcUser.setFirstName(user.getFirstName());
        kcUser.setLastName(user.getLastName());

        try {
            realmResource
                    .users()
                    .get(kcUser.getId())
                    .update(kcUser);
        } catch (Exception e) {
            System.err.println("Updating User information failed: \n" + e.getMessage());
        }

        System.out.println("User updated successfully.");
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
            throw new RuntimeException("Finding keycloak user faild");
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