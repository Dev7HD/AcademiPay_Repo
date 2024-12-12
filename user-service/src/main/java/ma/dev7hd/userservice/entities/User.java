package ma.dev7hd.userservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ROLE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "users-table")
public class User {
    @Id
    @Pattern(regexp = "[a-z]{2,3}[0-9]{8,9}",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Code invalid format"
    )
    private String id;

    @Email(regexp = "[a-z]+[a-z0-9._%+-]+@[a-z]{2,3}[a-z0-9.-]+.[a-z]{2,3}",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    //@Column(unique = true)
    private String email;

    //@Column(nullable = false)
    @Pattern(regexp = "^[A-Za-z]+(?:['-][A-Za-z]+)*$")
    private String firstName;

    //@Column(nullable = false)
    @Pattern(regexp = "^[A-Za-z]+(?:['-][A-Za-z]+)*$")
    private String lastName;

    private UUID photoId;

    private Date registerDate;

    private boolean isEnabled;

    @PrePersist
    void initRegisterDate(){
        if (registerDate == null){
            registerDate = new Date();
        }
    }

}