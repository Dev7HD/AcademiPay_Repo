package ma.dev7hd.fileservice;

import ma.dev7hd.fileservice.entities.UserPhoto;
import ma.dev7hd.fileservice.repositories.UserPhotoRepository;
import ma.dev7hd.fileservice.services.IFileProcessingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@SpringBootApplication
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(UserPhotoRepository userPhotoRepository, IFileProcessingService fileProcessingService) {
        return args -> {
            UserPhoto userPhoto = userPhotoRepository.findByFileName("default.jpg").orElse(null);

            if(userPhoto == null) {
                byte[] defaultUserAvatar;
                try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("static/img/default-user-avatar.png")) {
                    if (inputStream == null) {
                        throw new IllegalArgumentException("File not found: static/img/default-user-avatar.png");
                    }
                    defaultUserAvatar = inputStream.readAllBytes();
                } catch (IOException e) {
                    throw new IllegalArgumentException("File not found: static/img/default-user-avatar.png");
                }

                UUID photoId = fileProcessingService.resizeImageWithAspectRatio(defaultUserAvatar, "default");
                System.out.println("Image id: " + photoId);
            }
        };
    }

}
