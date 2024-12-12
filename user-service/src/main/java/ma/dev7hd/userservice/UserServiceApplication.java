package ma.dev7hd.userservice;

import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.enums.DepartmentName;
import ma.dev7hd.userservice.enums.ProgramID;
import ma.dev7hd.userservice.repositories.users.AdminRepository;
import ma.dev7hd.userservice.repositories.users.StudentRepository;
import ma.dev7hd.userservice.repositories.users.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.time.Year;
import java.util.Arrays;
import java.util.List;

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
    CommandLineRunner commandLineRunner(UserRepository userRepository, StudentRepository studentRepository, AdminRepository adminRepository){
        return args -> {

            List<String> adminIds = adminRepository.findAllAdminMle();
            Admin.serialCounter = adminIds.stream()
                    .map(id -> Integer.parseInt(id.substring(4))) // Extract and convert the numeric part
                    .max(Integer::compareTo)
                    .orElse(100000);

            for (ProgramID programID : ProgramID.values()) {
                Long counter = studentRepository.countByProgramId(programID);
                Student.programIDCounter.put(programID, counter);
            }

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
