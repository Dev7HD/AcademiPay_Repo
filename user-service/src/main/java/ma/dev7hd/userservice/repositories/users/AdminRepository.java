package ma.dev7hd.userservice.repositories.users;

import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.enums.DepartmentName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminRepository extends JpaRepository<Admin, String> {
    @Query("SELECT a FROM Admin a WHERE " +
            "(:id IS NULL OR a.id LIKE :id%) AND " +
            "(:email IS NULL OR a.email LIKE %:email%) AND " +
            "(:firstName IS NULL OR a.firstName LIKE %:firstName%) AND " +
            "(:lastName IS NULL OR a.lastName LIKE %:lastName%) AND " +
            "(:departmentName IS NULL OR a.departmentName = :departmentName)")
    Page<Admin> findByFilter(
            @Param("id") String id,
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("departmentName") DepartmentName departmentName,
            Pageable pageable);

    @Query("SELECT a.id FROM Admin a")
    List<String> findAllAdminMle();
}
