package ma.dev7hd.userservice.entities.registrations;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import ma.dev7hd.userservice.enums.ProgramID;

import java.util.Date;

@Entity
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
@Builder
public class PendingStudent {
    @Id
    @Email(regexp = "^(?=.{1,254}$)(?=.{1,64}@)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,63}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @Column(nullable = false, updatable = false)
    @Pattern(regexp = "^(?=.{3,30}$)[A-Za-z]+(?:['-][A-Za-z]+)*(?:\\s[A-Za-z]+(?:['-][A-Za-z]+)*)*$")
    private String firstName;

    @Column(nullable = false, updatable = false)
    @Pattern(regexp = "^(?=.{3,30}$)[A-Za-z]+(?:['-][A-Za-z]+)*(?:\\s[A-Za-z]+(?:['-][A-Za-z]+)*)*$")
    private String lastName;

    @Column(nullable = false, updatable = false)
    private Date registerDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ProgramID programID;

    private String photo;
}
