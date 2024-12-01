package ma.dev7hd.userservice.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class LoggedUser {
    private String email;
    private String firstName;
    private String lastName;
}
