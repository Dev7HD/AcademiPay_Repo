package ma.dev7hd.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.dev7hd.userservice.enums.DepartmentName;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Entity
@DiscriminatorValue("ADMIN")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Admin extends User {

    public static int serialCounter;

    @Enumerated(EnumType.STRING)
    private DepartmentName departmentName;


}
