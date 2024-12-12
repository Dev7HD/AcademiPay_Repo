package ma.dev7hd.userservice.services.global;

import lombok.AllArgsConstructor;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.entities.User;
import ma.dev7hd.userservice.enums.ProgramID;
import ma.dev7hd.userservice.repositories.users.AdminRepository;
import ma.dev7hd.userservice.repositories.users.StudentRepository;
import ma.dev7hd.userservice.repositories.users.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Optional;
import java.util.Random;

@Component
@AllArgsConstructor
public class UserDataProvider implements IUserDataProvider {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;

    @Override
    public Optional<Admin> getCurrentAdmin(){
        String userId = getCurrentUserId();
        System.out.println("userId: " + userId);
        return adminRepository.findById(userId.toUpperCase());
    }

    @Override
    public String getCurrentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getClaims().get("preferred_username").toString();
        System.out.println(userId);
        return userId;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findById(email).orElse(null);
    }

    @Override
    public Optional<User> getCurrentUser() {
        String userId = getCurrentUserId();
        return userRepository.findById(userId.toUpperCase());
    }

    @Override
    public Optional<Student> getCurrentStudent(){
        String currentUserEmail = getCurrentUserId();
        return studentRepository.findById(currentUserEmail);
    }

    @Override
    public Optional<Student> getStudentByEmail(String email){
        return studentRepository.findById(email);
    }

    @Override
    public Optional<Admin> getAdminByEmail(String email){
        return adminRepository.findById(email);
    }

    @Override
    public String generateStudentId(ProgramID program) {
        StringBuilder studentCode = new StringBuilder("STU");

        // Append the last two digits of the current year
        int year = Year.now().getValue() % 100;
        studentCode.append(year);

        // Append the program-specific code
        switch (program) {
            case SMP:
                studentCode.append("13");
                break;
            case SMC:
                studentCode.append("12");
                break;
            case SMA:
                studentCode.append("10");
                break;
            case SMI:
                studentCode.append("11");
                break;
            case SVT:
                studentCode.append("14");
                break;
            default:
                throw new IllegalArgumentException("Invalid Program");
        }

        // Append a unique 5-digit number
        Student.updateProgramCountsFromDB(program, 1L);
        Long serialNumber = Student.programIDCounter.get(program) + 1;
        studentCode.append(String.format("%05d", serialNumber));

        System.out.println("studentCode ====> "+studentCode);

        return studentCode.toString();
    }

    @Override
    public String generateAdminId(String lastName) {
        if (lastName == null || lastName.length() < 2) {
            throw new IllegalArgumentException("Last name must contain at least two characters.");
        }

        // Extract the first two characters from the last name and convert to uppercase
        String initials = lastName.substring(0, 2).toUpperCase();

        // Append the last two digits of the current year
        int year = Year.now().getValue() % 100;

        // Get the current serial number and increment it for the next use
        String serialNumber = String.format("%06d", Admin.serialCounter++);

        System.out.println("inits: " + initials);
        System.out.println("year: " + year);
        System.out.println("serialNumber: " + serialNumber);

        System.out.println("Generated id: ************************ \n"+initials + year + serialNumber);
        // Combine initials, year and serial number to create the ID
        return initials + year + serialNumber;
    }
}
