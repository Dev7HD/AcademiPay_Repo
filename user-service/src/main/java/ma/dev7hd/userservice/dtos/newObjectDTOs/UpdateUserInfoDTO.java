package ma.dev7hd.userservice.dtos.newObjectDTOs;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateUserInfoDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
}
