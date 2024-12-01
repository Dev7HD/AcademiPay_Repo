package ma.dev7hd.userservice.dtos.newObjectDTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewUserDTO {
    @Email(regexp = "[a-z]+[a-z0-9._%+-]+@[a-z]{2,3}[a-z0-9.-]+.[a-z]{2,3}",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    private String lastName;

    private String firstName;
}
