package ma.dev7hd.userservice.services;

import ma.dev7hd.userservice.entities.User;

public interface IClientService {
    void saveClientAndRegisterWithKeycloak(User newUser);

    void deleteKCUser(String email);

    void updateKCUser(User user);

    boolean toggleKCUserAccount(String email);
}
