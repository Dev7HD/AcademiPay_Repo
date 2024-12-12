package ma.dev7hd.fileservice.repositories;

import ma.dev7hd.fileservice.entities.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserFileRepository extends JpaRepository<UserFile, UUID> {
}
