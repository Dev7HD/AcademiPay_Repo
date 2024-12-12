package ma.dev7hd.userservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import ma.dev7hd.userservice.entities.User;
import ma.dev7hd.userservice.repositories.users.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

//@Service
//@AllArgsConstructor
public class UserSynchronizationService {

    /*private final UserRepository userRepository;

    //@EventListener(AuthenticationSuccessEvent.class)
    public void onAuthenticationSuccessEvent(final AuthenticationSuccessEvent event) throws JsonProcessingException {
        Jwt jwt = (Jwt) event.getAuthentication().getCredentials();

        // Check if user exists in the application database
        User user = userRepository.findByEmail(jwt.getClaim("email")).orElse(null);
        if (user == null) {
            // Log out the user if they are not found in the database
            SecurityContextHolder.clearContext(); // Clears the current authentication context
            throw new RuntimeException("User not found in the database. Logging out."); // Throws exception to terminate processing
        }

        // Check user roles
        if (event.getAuthentication().getAuthorities().toString().contains("ADMIN")) {
            System.out.println("The logged-in user has 'ADMIN' role");
        } else if (event.getAuthentication().getAuthorities().toString().contains("STUDENT")) {
            System.out.println("The logged-in user has 'STUDENT' role");
        }
    }*/


}
