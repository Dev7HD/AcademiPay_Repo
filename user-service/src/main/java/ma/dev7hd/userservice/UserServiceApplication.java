package ma.dev7hd.userservice;

import ma.dev7hd.userservice.repositories.users.AdminRepository;
import ma.dev7hd.userservice.repositories.users.StudentRepository;
import ma.dev7hd.userservice.services.IUserService;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }

    @Bean
    CommandLineRunner commandLineRunner(IUserService userService, StudentRepository studentRepository, AdminRepository adminRepository){
        return args -> {

            userService.countAdmins();
            userService.countStudentsByProgramId();

            /*StringBuilder studentCode = new StringBuilder("STU");
            studentCode.append(Year.now().getValue() % 100);
            Long serialNumber = Student.programIDCounter.get(ProgramID.SMA) + 1;
            studentCode.append(String.format("%08d", serialNumber));
            System.out.println("Counter: " + studentCode);

            Student student = new Student();
            student.setEmail("user2@student.ma");
            student.setId(studentCode.toString());
            student.setFirstName("studentFirstName");
            student.setLastName("studentLastName");
            student.setProgramId(ProgramID.SMA);
            studentRepository.save(student);*/

            /*Admin admin = new Admin();
            admin.setEmail("test@email.ma");
            admin.setDepartmentName(DepartmentName.CHEMISTRY);
            admin.setId("AB" + Admin.serialCounter);
            admin.setFirstName("fname");
            admin.setLastName("lname");
            userRepository.save(admin);*/
        };
    }

}
