package ma.dev7hd.userservice.dtos.otherDTOs;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangePWDTO {
    private String oldPassword;
    private String newPassword;
}
