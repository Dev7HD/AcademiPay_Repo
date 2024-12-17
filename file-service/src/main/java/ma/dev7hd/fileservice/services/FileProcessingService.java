package ma.dev7hd.fileservice.services;

import lombok.AllArgsConstructor;
import ma.dev7hd.fileservice.business.IFileProcessingBusiness;
import ma.dev7hd.fileservice.dtos.PhotoDTO;
import ma.dev7hd.fileservice.entities.PaymentInvoice;
import ma.dev7hd.fileservice.entities.UserPhoto;
import ma.dev7hd.fileservice.models.Payment;
import ma.dev7hd.fileservice.dtos.InvoiceDTO;
import ma.dev7hd.fileservice.dtos.ProfileDTO;
import ma.dev7hd.fileservice.models.Student;
import ma.dev7hd.fileservice.repositories.PaymentInvoiceRepository;
import ma.dev7hd.fileservice.repositories.UserPhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
@AllArgsConstructor
public class FileProcessingService implements IFileProcessingService{
    private final UserPhotoRepository userPhotoRepository;
    private final PaymentInvoiceRepository paymentInvoiceRepository;
    private final SpringTemplateEngine templateEngine;
    private final IFileProcessingBusiness fileProcessingBusiness;

    private final Path TEMP_PATH = Paths.get(System.getProperty("user.home"), "data", "temp");

    @Override
    public InvoiceDTO generatePaymentReceipt(Payment payment) throws IOException {
        System.out.println(payment);
        if (payment != null){
            boolean isDuplicata = true;
            Optional<PaymentInvoice> optionalPaymentInvoice = paymentInvoiceRepository.findByPaymentId(payment.getId());
            PaymentInvoice paymentInvoice = new PaymentInvoice();
            if (optionalPaymentInvoice.isEmpty()) {
                isDuplicata = false;
                String generatedInvoiceNumber = fileProcessingBusiness.generateInvoiceNumber(payment.getId());
                paymentInvoice.setPaymentId(payment.getId());
                paymentInvoice.setCreateAt(LocalDateTime.now());
                paymentInvoice.setFileName(generatedInvoiceNumber);
                paymentInvoiceRepository.save(paymentInvoice);
            } else {
                paymentInvoice = optionalPaymentInvoice.get();
            }

            // 64 base logo
            String logoPNG = fileProcessingBusiness.get64BaseImg("static/img/wallet.png");

            //64 base stamp
            String stamp = fileProcessingBusiness.get64BaseImg("static/img/stamp.png");

            String invoiceCreatedAt = paymentInvoice.getCreateAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:s"));

            // Prepare the Thymeleaf context with variables
            Context context = new Context();
            context.setVariable("invoiceNumber", paymentInvoice.getFileName());
            context.setVariable("isDuplicata", isDuplicata);
            context.setVariable("invoiceDate", invoiceCreatedAt);
            context.setVariable("studentCode", payment.getStudent().getId());
            context.setVariable("paymentId", payment.getId());
            context.setVariable("amount", payment.getAmount());
            context.setVariable("paymentDate", payment.getDate());
            context.setVariable("downloadDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:s")));
            context.setVariable("studentFName", payment.getStudent().getFirstName());
            context.setVariable("studentLName", payment.getStudent().getLastName());
            context.setVariable("studentProgram", payment.getStudent().getProgramId());
            context.setVariable("paymentMethod", payment.getType());
            context.setVariable("logo", logoPNG);
            context.setVariable("stamp", stamp);


            // Render the HTML template as a String
            String htmlContent = templateEngine.process("receipt", context);

            // Generate PDF from the HTML content
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(htmlContent);
                renderer.layout();
                renderer.createPDF(outputStream);

                ByteArrayInputStream invoiceByteArrayInputStream = new ByteArrayInputStream(outputStream.toByteArray());

                InvoiceDTO invoiceDTO = new InvoiceDTO();
                invoiceDTO.setStream(invoiceByteArrayInputStream);
                invoiceDTO.setNumber(paymentInvoice.getFileName());

                return invoiceDTO;

            } catch (Exception e) {
                throw new RuntimeException("Failed to generate PDF", e);
            }
        }
        return null;
    }



    @Override
    public ProfileDTO generateProfile(Student student) throws IOException {

        if (student != null) {
            Optional<UserPhoto> optionalUserPhoto = userPhotoRepository.findByUserId(student.getId());
            String photoUri;
            String studentPhoto = null;
            if (optionalUserPhoto.isPresent()){
                photoUri = optionalUserPhoto.get().getFileUri();
                byte[] photo = Files.readAllBytes(Paths.get(URI.create(photoUri)));

                // 64 base logo
                studentPhoto = fileProcessingBusiness.get64BaseImg(photo);
            }

            double total = 0;

            List<Payment> payments = student.getPayments();

            List<Payment> validatedPayments = payments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.VALIDATED).toList();

            for (Payment payment : validatedPayments) {
                total += payment.getAmount();
            }

            // Prepare the Thymeleaf context with variables
            Context context = new Context();
            context.setVariable("studentCode", student.getId());
            context.setVariable("payments", validatedPayments);
            context.setVariable("email", student.getEmail());
            context.setVariable("studentFName", student.getFirstName());
            context.setVariable("studentLName", student.getLastName());
            context.setVariable("studentProgram", student.getProgramId());
            context.setVariable("photo", studentPhoto);
            context.setVariable("total", total);


            // Render the HTML template as a String
            String htmlContent = templateEngine.process("profile", context);
            if (!Files.exists(TEMP_PATH)) {
                Files.createDirectories(TEMP_PATH);
            }

            String htmlName = UUID.randomUUID().toString();
            Path htmlFilePath = TEMP_PATH.resolve(htmlName + ".html");

            try (FileWriter fileWriter = new FileWriter(htmlFilePath.toString())) {
                fileWriter.write(htmlContent);
                System.out.println("HTML file saved at: " + htmlFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Generate PDF from the HTML content
            try {
                Path outputPdfPath = TEMP_PATH.resolve(htmlName + ".pdf");
                ProcessBuilder processBuilder = new ProcessBuilder();
                String htmlUri = htmlFilePath.toUri().toString();
                processBuilder.command("wkhtmltopdf", htmlUri, outputPdfPath.toString());
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    System.out.println("PDF generated successfully: " + outputPdfPath);
                } else {
                    System.err.println("Error during PDF generation.");
                    throw new RuntimeException("Failed to generate PDF");
                }

                byte[] pdfFile = Files.readAllBytes(outputPdfPath);
                try {
                    Files.deleteIfExists(htmlFilePath);
                    Files.deleteIfExists(outputPdfPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ProfileDTO profileDTO = new ProfileDTO();
                profileDTO.setStream(new ByteArrayInputStream(pdfFile));
                profileDTO.setPdfName(student.getId() + "_Profile.pdf");

                return profileDTO;

            } catch (Exception e) {
                throw new RuntimeException("Failed to generate PDF", e);
            }
        }
        return null;
    }

    @Transactional
    @Override
    public UUID resizeImageWithAspectRatio(byte[] originalImageData, String userId) throws IOException {

        Optional<UserPhoto> optionalUserPhoto = userPhotoRepository.findByUserId(userId);
        UserPhoto userPhoto = new UserPhoto();
        if(optionalUserPhoto.isPresent()){
            userPhoto = optionalUserPhoto.get();
        }
        // Convert byte[] to BufferedImage
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalImageData);
        BufferedImage originalImage = ImageIO.read(byteArrayInputStream);

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate the aspect ratio of the original image
        double aspectRatio = (double) originalWidth / originalHeight;

        int newWidth;
        int newHeight;

        // Adjust width and height based on the aspect ratio
        int maxHeight = 600;
        int maxWidth = 600;

        if (maxWidth / (double) maxHeight > aspectRatio) {
            // Width is too large, adjust it
            newWidth = (int) (maxHeight * aspectRatio);
            newHeight = maxHeight;
        } else {
            // Height is too large, adjust it
            newWidth = maxWidth;
            newHeight = (int) (maxWidth / aspectRatio);
        }

        // Resize the image while keeping the aspect ratio
        Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

        // Create a new BufferedImage without metadata
        BufferedImage resizedBufferedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        // Draw the resized image into the new BufferedImage
        Graphics2D graphics = resizedBufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(resizedImage, 0, 0, null);
        graphics.dispose();

        // Convert the BufferedImage to a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedBufferedImage, "jpg", byteArrayOutputStream);// You can choose the format: "jpg", "png", etc.

        String photoUri = fileProcessingBusiness.savePhoto(byteArrayOutputStream.toByteArray(), userId);


        userPhoto.setUserId(userId);
        userPhoto.setFileName(userId + ".jpg");
        userPhoto.setCreateAt(LocalDateTime.now());
        userPhoto.setFileUri(photoUri);

        UserPhoto saved = userPhotoRepository.save(userPhoto);

        return saved.getId();
    }



    @Override
    public void deleteUsersPhotos(List<String> usersIds){
        List<UserPhoto> usersPhotos = userPhotoRepository.findAllByUserId(usersIds);
        if(!usersPhotos.isEmpty()){
            usersPhotos.parallelStream().forEach(userPhoto -> {
                try {
                    Files.deleteIfExists(Path.of(URI.create(userPhoto.getFileUri())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public PhotoDTO getUserPhoto(UUID photoId){
        Optional<UserPhoto> optionalUserPhoto = userPhotoRepository.findById(photoId);
        PhotoDTO photoDTO = null;
        if(optionalUserPhoto.isPresent()) {
            UserPhoto userPhoto = optionalUserPhoto.get();
            try {
                photoDTO = PhotoDTO.builder()
                        .photoBytes(Files.readAllBytes(Paths.get(URI.create(userPhoto.getFileUri()))))
                        .fileName(userPhoto.getFileName())
                        .build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return photoDTO;

    }

    @Override
    public UUID getDefaultPhotoId(){
        UserPhoto userPhoto = userPhotoRepository.findByFileName("default.jpg").orElseThrow(() -> new RuntimeException("Default photo not found"));
        return userPhoto.getId();
    }
}
