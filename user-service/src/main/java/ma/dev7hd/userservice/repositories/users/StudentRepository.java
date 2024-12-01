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

    boolean existsByEmail( String email);

    Optional<Student> findByEmail(String email);

    boolean existsById( String id);

    @Query("SELECT s FROM Student s WHERE " +
            "(:email IS NULL OR s.email LIKE %:email%) AND " +
            "(:id IS NULL OR s.id LIKE :id%) AND " +
            "(:firstName IS NULL OR s.firstName LIKE %:firstName%) AND " +
            "(:lastName IS NULL OR s.lastName LIKE %:lastName%) AND " +
            "(:id IS NULL OR :id = '' OR s.id like :id%) AND " +
            "(:programId IS NULL OR s.programId = :programId)")
    Page<Student> findByFilter(
            @Param("email") String email,
            @Param("id") String id,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("programId") ProgramID programID,
            Pageable pageable);

    Integer countByProgramId(ProgramID programId);

    @Query("SELECT s.email FROM Student s")
    Set<String> findAllEmails();

    @Query("SELECT MAX(SUBSTRING(s.id, LENGTH(s.id) - 3)) FROM Student s ")
    Optional<Integer> findLastIdentifier();
}

