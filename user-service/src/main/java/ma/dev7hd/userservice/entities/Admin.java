package ma.dev7hd.userservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.dev7hd.userservice.enums.DepartmentName;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Entity
@DiscriminatorValue("ADMIN")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Admin extends User {

    public static int serialCounter;

    @Enumerated(EnumType.STRING)
    private DepartmentName departmentName;


    /*@Transient
    private List<PaymentStatusChange> paymentStatusChanges;

    @OneToMany(mappedBy = "adminBanner", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<BanedRegistration> banedRegistrations;

    @ManyToMany(mappedBy = "adminRemover")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<Notification> deletedNotifications;*/

    /*@Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_STUDENT"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }*/

}
