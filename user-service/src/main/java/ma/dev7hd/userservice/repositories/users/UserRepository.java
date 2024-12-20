package ma.dev7hd.userservice.repositories.users;

import ma.dev7hd.userservice.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByIdIgnoreCase(String s);

    boolean existsById(String id);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByIdInIgnoreCase(List<String> ids);
}
