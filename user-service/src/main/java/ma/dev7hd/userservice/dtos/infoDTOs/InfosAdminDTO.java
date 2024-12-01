package ma.dev7hd.userservice.dtos.infoDTOs;

import lombok.Getter;
import lombok.Setter;
import ma.dev7hd.userservice.enums.DepartmentName;

@Getter
@Setter
public class InfosAdminDTO extends InfosUserDTO {
    private DepartmentName departmentName;
}
