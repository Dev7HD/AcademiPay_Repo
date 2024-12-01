package ma.dev7hd.userservice.services;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ma.dev7hd.userservice.config.KeycloakConfig;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;


@Service
public class ClientService implements IClientService {

    private final RealmResource realmResource;

    public ClientService(Keycloak keycloak) {
        this.realmResource = keycloak.realm(KeycloakConfig.realm);
    }

    @Override
    @Transactional
    public void saveClientAndRegisterWithKeycloak(User savedUser) {

        // Register client as Keycloak user
        UserRepresentation keycloakUser = new UserRepresentation();
        assert savedUser != null;
        keycloakUser.setId(savedUser.getId());
        keycloakUser.setUsername(savedUser.getEmail());
        keycloakUser.setEmailVerified(true);
        keycloakUser.setEmail(savedUser.getEmail());
        keycloakUser.setFirstName(savedUser.getFirstName());
        keycloakUser.setLastName(savedUser.getLastName());
        keycloakUser.setEnabled(true);

        RoleRepresentation adminRole = realmResource.roles().get("ADMIN").toRepresentation();
        RoleRepresentation studentRole = realmResource.roles().get("STUDENT").toRepresentation();

        // Password and other attributes can be set here
        CredentialRepresentation passwordCredentials = createPasswordCredentials();
        keycloakUser.setCredentials(Collections.singletonList(passwordCredentials));


        // Add user to Keycloak
        Response response = null;

        try {
            response = realmResource.users().create(keycloakUser);

            UsersResource usersResource = realmResource.users();
            UserRepresentation userRepresentation = usersResource.searchByEmail(savedUser.getEmail(), true).getFirst(); // Replace with appropriate search
            String userId = userRepresentation.getId();

            if(savedUser instanceof Admin){
                usersResource.get(userId).roles().realmLevel().add(List.of(adminRole, studentRole));
            } else {
                usersResource.get(userId).roles().realmLevel().add(List.of(studentRole));
                //List<RoleRepresentation> assignedRoles = usersResource.get(userId).roles().realmLevel().listAll();
            }

        } catch(Exception e) {
            System.err.println("Creating client error: " + e.getMessage());
        }

        if (response==null || response.getStatus() != 201) {
            System.err.println("Couldn't create Keycloak user.");
        }else{
            System.out.println("Keycloak user created.... verify in keycloak!");
        }

    }

    @Override
    public void deleteKCUser(String email){

        System.out.println("Start deleting user from Keycloak...");

        UsersResource usersResource = realmResource.users();
        UserRepresentation user = usersResource.searchByEmail(email, true).getFirst();
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
    public void updateKCUser(User user){
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
        } catch (Exception e){
            System.err.println("Updating User information failed: \n" + e.getMessage());
        }

        System.out.println("User updated successfully.");
    }

    @Override
    public boolean toggleKCUserAccount(String email){
        UsersResource usersResource = realmResource.users();
        UserRepresentation kcUser = usersResource.searchByEmail(email, true).getFirst();
        kcUser.setEnabled(!kcUser.isEnabled());
        try {
            realmResource
                    .users()
                    .get(kcUser.getId())
                    .update(kcUser);
        } catch (Exception e){
            System.err.println("Toggling user account failed: \n" + e.getMessage());
        }
        return kcUser.isEnabled();
    }

    private CredentialRepresentation createPasswordCredentials() {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        String defaultPassword = "123456";
        credential.setValue(defaultPassword);
        credential.setTemporary(false);
        return credential;
    }
}
