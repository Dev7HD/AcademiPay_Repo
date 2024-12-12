package ma.dev7hd.fileservice.services;

import ma.dev7hd.fileservice.dtos.InvoiceDTO;
import ma.dev7hd.fileservice.dtos.PhotoDTO;
import ma.dev7hd.fileservice.dtos.ProfileDTO;
import ma.dev7hd.fileservice.models.Payment;
import ma.dev7hd.fileservice.models.Student;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface IFileProcessingService {
    InvoiceDTO generatePaymentReceipt(Payment payment) throws IOException;

    ProfileDTO generateProfile(Student student) throws IOException;

    UUID resizeImageWithAspectRatio(byte[] originalImageData, String userId) throws IOException;

    void deleteUsersPhotos(List<String> usersIds);

    PhotoDTO getUserPhoto(String userId);
}
