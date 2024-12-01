package ma.dev7hd.userservice.dtos.infoDTOs;


import lombok.Getter;
import lombok.Setter;
import ma.dev7hd.userservice.enums.ProgramID;

@Getter
@Setter
public class InfosStudentDTO extends InfosUserDTO {
    private ProgramID programId;
}
