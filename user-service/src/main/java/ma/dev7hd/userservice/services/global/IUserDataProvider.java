package ma.dev7hd.userservice.services.global;


import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.User;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.enums.ProgramID;

import java.util.Optional;

public interface IUserDataProvider {
    Optional<Admin> getCurrentAdmin();

    String getCurrentUserId();

    User getUserByEmail(String email);

    Optional<User> getCurrentUser();

    Optional<Student> getCurrentStudent();

    Optional<Student> getStudentByEmail(String email);

    Optional<Admin> getAdminByEmail(String email);

    String generateStudentId(ProgramID program);

    String generateAdminId(String lastName);
}
