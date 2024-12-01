package ma.dev7hd.userservice.repositories.registrations;

import ma.dev7hd.userservice.entities.registrations.BanedRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BanedRegistrationRepository extends JpaRepository<BanedRegistration, String> {
    boolean existsById(String email);
}
