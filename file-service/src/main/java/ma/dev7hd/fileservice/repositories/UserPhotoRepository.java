package ma.dev7hd.fileservice.repositories;

import ma.dev7hd.fileservice.entities.UserPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserPhotoRepository extends JpaRepository<UserPhoto, UUID> {
    Optional<UserPhoto> findByUserId(String userId);

    @Query("SELECT p FROM UserPhoto p WHERE p.userId IN :usersIds")
    List<UserPhoto> findAllByUserId(List<String> usersIds);
}
