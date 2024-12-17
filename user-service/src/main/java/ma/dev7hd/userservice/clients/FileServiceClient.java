package ma.dev7hd.userservice.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ma.dev7hd.userservice.config.FeignClientConfig;
import ma.dev7hd.userservice.entities.Student;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "file-service", configuration = FeignClientConfig.class)
public interface FileServiceClient {

    @PostMapping(value = "/profiles/student-profile")
    @CircuitBreaker(name = "profileProcessClient", fallbackMethod = "defaultProfile")
    ResponseEntity<?> getProfile(@RequestBody Student student)  throws IOException;

    @PostMapping(value = "/photos/process-photo/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CircuitBreaker(name = "photoProcessClient", fallbackMethod = "defaultPhoto")
    UUID processUserPhoto(@RequestPart(value = "photo") MultipartFile photo,@PathVariable String userId) throws IOException;

    @DeleteMapping("/photos/delete")
    @CircuitBreaker(name = "photoDeleteProcessClient", fallbackMethod = "defaultPhotoDelete")
    void deletePhotos(@RequestBody List<String> usersIds);

    @GetMapping(value = "/photos/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    @CircuitBreaker(name = "photoGetProcessClient", fallbackMethod = "defaultUserPhoto")
    ResponseEntity<byte[]> getUserPhoto(@PathVariable(name = "id") UUID photoId);

    @GetMapping("/photos/default")
    @CircuitBreaker(name = "photoGetProcessClient", fallbackMethod = "defaultUserPhotoId")
    UUID getDefaultPhotoId();

    default UUID defaultUserPhotoId(Exception e) {
        return null;
    }

    default void defaultPhotoDelete(List<String> usersIds, Exception e){
        System.err.println("File service not available.");
    }

    default ResponseEntity<?> defaultProfile(Student student, Exception e) throws IOException {
        return null;
    }

    default UUID defaultPhoto(MultipartFile photo, String userId, Exception e) {
        return null;
    }


    default ResponseEntity<byte[]> defaultUserPhoto(String userId, Exception e) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=default-user-avatar");

        byte[] defaultUserAvatar;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("static/img/default-user-avatar.png")) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: static/img/default-user-avatar.png");
            }
            defaultUserAvatar = inputStream.readAllBytes();
        }

        return new ResponseEntity<>(defaultUserAvatar, headers, HttpStatus.OK);
    }
}
