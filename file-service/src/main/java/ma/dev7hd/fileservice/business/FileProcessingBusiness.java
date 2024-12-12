package ma.dev7hd.fileservice.business;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.Base64;
import java.util.UUID;

@Component
public class FileProcessingBusiness implements IFileProcessingBusiness {
    public final Path PATH_TO_PHOTOS = Paths.get(System.getProperty("user.home"), "data", "photos");
    @Override
    public String get64BaseImg(String classpath) throws IOException {
        ClassPathResource imgFile = new ClassPathResource(classpath);
        Path imagePath = Paths.get(imgFile.getURI());
        byte[] imageBytes = Files.readAllBytes(imagePath);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    @Override
    public String get64BaseImg(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    @Override
    public String generateInvoiceNumber(UUID paymentId) {
        // For example: use current year + payment ID hash as the invoice number
        return "INV-" + Year.now().getValue() + "-" + paymentId.toString().substring(0, 8);
    }

    @Override
    public String savePhoto(byte[] photo, String studentCode){
        String fileName = studentCode + ".jpg";
        Path filePath = PATH_TO_PHOTOS.resolve(fileName);
        try {
            if (!Files.exists(PATH_TO_PHOTOS)) {
                Files.createDirectories(PATH_TO_PHOTOS);
            }
            if(Files.exists(filePath)) deletePhoto(filePath.toUri().toString());
            Files.write(filePath, photo);
        } catch (IOException e) {
            return null;
        }
        File file = new File(filePath.toString());
        return file.toURI().toString();
    }

    private void deletePhoto(String photoUri) throws IOException {
        Files.deleteIfExists(Path.of(URI.create(photoUri)));
    }
}
