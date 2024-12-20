package ma.dev7hd.userservice.repositories.registrations;

import ma.dev7hd.userservice.entities.registrations.PendingStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PendingStudentRepository extends JpaRepository<PendingStudent, String> {

    @Query("SELECT a FROM PendingStudent a WHERE " +
            "(:email IS NULL OR a.email like :email%) ")
    Page<PendingStudent> findByPendingStudentsByFilter(
            @Param("email") String email,
            Pageable pageable);

    Optional<PendingStudent> findByEmailIgnoreCase(String email);

}
