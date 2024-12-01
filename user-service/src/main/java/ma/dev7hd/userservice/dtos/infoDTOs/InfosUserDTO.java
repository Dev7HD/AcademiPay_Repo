package ma.dev7hd.userservice.dtos.infoDTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InfosUserDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private String photo;
}
