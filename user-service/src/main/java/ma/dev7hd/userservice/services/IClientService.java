package ma.dev7hd.userservice.services;

import jakarta.ws.rs.core.Response;
import ma.dev7hd.userservice.entities.User;

public interface IClientService {
    Response registerUserWithKeycloak(User newUser);

    int deleteKCUser(String userId);

    void updateKCUser(User user);

    boolean toggleKCUserAccount(String email);

    // RESET PASSWORD TO THE DEFAULT ONE
    void changeUserPassword(String userId);

    //CHANGE PASSWORD TO NEW ONE
    void changeUserPassword(String userEmail, String newPassword);

    boolean verifyPassword(String email, String password);
}
