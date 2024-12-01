package ma.dev7hd.userservice.entities.registrations;

import jakarta.persistence.*;
import lombok.*;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.enums.ProgramID;

import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BanedRegistration {
    @Id
    private String email;

    @Column(nullable = false, updatable = false)
    private String firstName;

    @Column(nullable = false, updatable = false)
    private String lastName;

    @Column(nullable = false, updatable = false)
    private Date registerDate;

    @Column(nullable = false, updatable = false)
    private Date banDate;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ProgramID programID;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false, updatable = false)
    private Admin adminBanner;
}
