package ma.dev7hd.userservice;

import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.enums.DepartmentName;
import ma.dev7hd.userservice.repositories.users.UserRepository;
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
    CommandLineRunner commandLineRunner(UserRepository userRepository){
        return args -> {
            Admin admin = new Admin();
            admin.setId("stu1234567");
            admin.setEmail("test@email.ma");
            admin.setDepartmentName(DepartmentName.CHEMISTRY);
            admin.setUserName("test_user");
            admin.setFirstName("fname");
            admin.setLastName("lname");
            admin.setPhoto("photo");

            userRepository.save(admin);

        };
    }

}
