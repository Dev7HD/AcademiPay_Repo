package ma.dev7hd.userservice.dtos.newObjectDTOs;

import lombok.Getter;
import lombok.Setter;
import ma.dev7hd.userservice.enums.DepartmentName;

@Getter
@Setter
public class UpdateAdminInfoDTO extends UpdateUserInfoDTO {
    private DepartmentName departmentName;
}
