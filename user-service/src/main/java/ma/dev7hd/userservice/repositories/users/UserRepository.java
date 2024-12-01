package ma.dev7hd.userservice.repositories.users;

import ma.dev7hd.userservice.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsById(String id);

    boolean existsByEmail(String email);
}
