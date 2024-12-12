package ma.dev7hd.fileservice.business;

import java.io.IOException;
import java.util.UUID;

public interface IFileProcessingBusiness {
    String get64BaseImg(String classpath) throws IOException;

    String get64BaseImg(byte[] imageBytes);

    String generateInvoiceNumber(UUID paymentId);

    String savePhoto(byte[] photo, String studentCode);

}
