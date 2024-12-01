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
    @Email(regexp = "[a-z]+[a-z0-9._%+-]+@[a-z]{2,3}[a-z0-9.-]+.[a-z]{2,3}",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @Column(nullable = false, updatable = false)
    @Pattern(regexp = "^[A-Za-z]+(?:['-][A-Za-z]+)*$")
    private String firstName;

    @Column(nullable = false, updatable = false)
    @Pattern(regexp = "^[A-Za-z]+(?:['-][A-Za-z]+)*$")
    private String lastName;

    @Column(nullable = false, updatable = false)
    private Date registerDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ProgramID programID;

    private String photo;
}
