package ma.dev7hd.userservice.dtos.infoDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InfosUserDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean isEnabled;
    private UUID photoId;
    private Date registerDate;
}
