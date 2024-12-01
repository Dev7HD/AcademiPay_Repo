package ma.dev7hd.userservice.dtos.newObjectDTOs;

import lombok.Getter;
import lombok.Setter;
import ma.dev7hd.userservice.enums.ProgramID;

@Getter
@Setter
public class NewStudentDTO extends NewUserDTO {
    private ProgramID programId;
}
