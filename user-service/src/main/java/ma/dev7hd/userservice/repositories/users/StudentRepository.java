package ma.dev7hd.userservice.repositories.users;

import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.enums.ProgramID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StudentRepository extends JpaRepository<Student,String> {
    List<Student> findStudentByProgramId(ProgramID programId);

    boolean existsByEmailIgnoreCase( String email);

    Optional<Student> findByEmailIgnoreCase(String email);

    Optional<Student> findByIdIgnoreCase(String id);

    boolean existsById(String id);

    @Query("SELECT s FROM Student s WHERE " +
            "(:id IS NULL OR :id = '' OR LOWER(s.id) LIKE :id%) AND " +
            "(:email IS NULL OR LOWER(s.email) LIKE %:email%) AND " +
            "(:firstName IS NULL OR LOWER(s.firstName) LIKE %:firstName%) AND " +
            "(:lastName IS NULL OR LOWER(s.lastName) LIKE %:lastName%) AND " +
            "(:programId IS NULL OR s.programId = :programId)")
    Page<Student> findByFilter(
            @Param("id") String id,
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("programId") ProgramID programID,
            Pageable pageable);

    Long countByProgramId(ProgramID programId);

    @Query("SELECT s.email FROM Student s")
    Set<String> findAllEmails();

}

