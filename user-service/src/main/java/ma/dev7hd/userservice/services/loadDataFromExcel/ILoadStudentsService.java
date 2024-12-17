package ma.dev7hd.userservice.services.loadDataFromExcel;

import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ILoadStudentsService {
    @Transactional
    ResponseEntity<String> uploadStudentFile(MultipartFile file) throws Exception;
}
