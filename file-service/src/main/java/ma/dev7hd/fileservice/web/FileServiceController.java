package ma.dev7hd.fileservice.web;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import ma.dev7hd.fileservice.dtos.InvoiceDTO;
import ma.dev7hd.fileservice.dtos.PhotoDTO;
import ma.dev7hd.fileservice.dtos.ProfileDTO;
import ma.dev7hd.fileservice.entities.UserPhoto;
import ma.dev7hd.fileservice.models.Payment;
import ma.dev7hd.fileservice.models.Student;
import ma.dev7hd.fileservice.repositories.UserPhotoRepository;
import ma.dev7hd.fileservice.services.IFileProcessingService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class FileServiceController {
    private final IFileProcessingService fileProcessingService;
    private final UserPhotoRepository userPhotoRepository;

    @GetMapping("/photos/all")
    public List<UserPhoto> getUsersPhotos(){
        return userPhotoRepository.findAll();
    }

    @GetMapping("/invoices/payment-invoice")
    public ResponseEntity<?> getPaymentInvoice(@RequestBody Payment payment) throws IOException {
        InvoiceDTO invoiceDTO = fileProcessingService.generatePaymentReceipt(payment);

        if (invoiceDTO != null) {
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=" + invoiceDTO.getNumber() +".pdf");

            return new ResponseEntity<>(invoiceDTO.getStream().readAllBytes(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.badRequest().body("The payment must be 'VALIDATED' to be downloaded");
        }
    }

    @PostMapping("/profiles/student-profile")
    public ResponseEntity<?> getProfile(@RequestBody Student student) throws IOException {
        ProfileDTO profileDTO = fileProcessingService.generateProfile(student);

        if (profileDTO != null) {
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=" + profileDTO.getPdfName());

            return new ResponseEntity<>(profileDTO.getStream().readAllBytes(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.badRequest().body("Error generating profile.");
        }
    }

    @PostMapping(value = "/photos/process-photo/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UUID processUserPhoto(@Parameter(description = "Photo to upload") @RequestPart(value = "photo") MultipartFile photo,@PathVariable String userId) throws IOException {
        return fileProcessingService.resizeImageWithAspectRatio(photo.getBytes(), userId);

    }

    @DeleteMapping("/photos/delete")
    public void deleteUsersPhotos(@RequestBody List<String> usersIds){
        fileProcessingService.deleteUsersPhotos(usersIds);
    }

    @GetMapping(value = "/photos/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    ResponseEntity<byte[]> getUserPhoto(@PathVariable(name = "id") String userId){
        PhotoDTO photoDTO = fileProcessingService.getUserPhoto(userId);
        return generateUserPhotoResponse(photoDTO);
    }

    @NotNull
    private ResponseEntity<byte[]> generateUserPhotoResponse(PhotoDTO photoDTO) {
        if( photoDTO != null){
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=" + photoDTO.getFileName());

            return new ResponseEntity<>(photoDTO.getPhotoBytes(), headers, HttpStatus.OK);
        } else {
            throw new RuntimeException("Error processing photo!!");
        }
    }
}
