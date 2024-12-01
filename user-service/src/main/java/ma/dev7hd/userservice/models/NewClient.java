package ma.dev7hd.userservice.models;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NewClient {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String roles;
}
