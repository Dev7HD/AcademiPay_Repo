package ma.dev7hd.userservice.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;

@FeignClient(name = "file-service")
public interface FileServiceClient {
    @PostMapping(value = "/process-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CircuitBreaker(name = "ProcessPhotoClient", fallbackMethod = "defaultProcessedPhoto")
    byte[] processPhoto(@RequestPart("file") MultipartFile file);

    default byte[] defaultProcessedPhoto(MultipartFile file, Exception e){
        return null;
    }
}
