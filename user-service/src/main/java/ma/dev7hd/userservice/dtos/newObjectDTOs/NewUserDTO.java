package ma.dev7hd.userservice.dtos.newObjectDTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewUserDTO {
    @Email(regexp = "^(?=.{1,254}$)(?=.{1,64}@)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,63}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @Pattern(regexp = "^(?=.{3,30}$)[A-Za-z]+(?:['-][A-Za-z]+)*(?:\\s[A-Za-z]+(?:['-][A-Za-z]+)*)*$")
    private String firstName;

    @Pattern(regexp = "^(?=.{3,30}$)[A-Za-z]+(?:['-][A-Za-z]+)*(?:\\s[A-Za-z]+(?:['-][A-Za-z]+)*)*$")
    private String lastName;
}
