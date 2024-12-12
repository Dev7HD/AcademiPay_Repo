package ma.dev7hd.userservice.dtos.infoDTOs;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class InfosUserDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean isEnabled;
    private String photoId;
    private Date registerDate;
}
