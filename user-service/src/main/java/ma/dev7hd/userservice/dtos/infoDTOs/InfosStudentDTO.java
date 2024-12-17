package ma.dev7hd.userservice.dtos.infoDTOs;


import lombok.*;
import ma.dev7hd.userservice.enums.ProgramID;

@Getter
@Setter
@AllArgsConstructor @NoArgsConstructor
public class InfosStudentDTO extends InfosUserDTO {
    private ProgramID programId;
}
