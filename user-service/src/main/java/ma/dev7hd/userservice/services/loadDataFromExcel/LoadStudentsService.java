package ma.dev7hd.userservice.services.loadDataFromExcel;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import ma.dev7hd.userservice.clients.FileServiceClient;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.enums.ProgramID;
import ma.dev7hd.userservice.repositories.users.StudentRepository;
import ma.dev7hd.userservice.services.IUserService;
import ma.dev7hd.userservice.services.KeycloakUserService;
import ma.dev7hd.userservice.services.global.IUserDataProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.keycloak.admin.client.Keycloak;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
public class LoadStudentsService implements ILoadStudentsService {
    private final StudentRepository studentRepository;
    private final IUserDataProvider userDataProvider;
    private final FileServiceClient fileServiceClient;
    private final KeycloakUserService keycloakUserService ;
    private final IUserService userService;
    private final Keycloak keycloak;

    private final List<String> DOC_HEADER = List.of("email", "firstName", "lastName", "programId");
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * Handles the upload of a student file and processes its contents.
     * The file must be in Excel format (xls or xlsx) with valid headers.
     * Valid rows are saved as student records, and invalid rows are logged.
     *
     * @param file the uploaded MultipartFile containing student data
     * @return ResponseEntity with a status message detailing the operation results
     * @throws Exception if an error occurs during file processing
     */
    @Transactional
    @Override
    public ResponseEntity<String> uploadStudentFile(MultipartFile file) throws Exception {
        Instant start = Instant.now();

        UUID defaultPhotoId = fileServiceClient.getDefaultPhotoId();
        System.out.println("defaultPhotoId: " + defaultPhotoId);
        String fileType = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));

        if (!isValidFileType(fileType)) {
            return ResponseEntity.badRequest().body("File type not accepted.");
        }

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        if (!validateHeaders(sheet.getRow(0))) {
            return ResponseEntity.badRequest().body("Invalid table headers.");
        }

        List<Student> students = Collections.synchronizedList(new ArrayList<>());
        List<Integer> errorRowIndexes = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = IntStream.range(1, sheet.getLastRowNum() + 1)
                .mapToObj(rowIndex -> CompletableFuture.runAsync(() -> {
                    Row row = sheet.getRow(rowIndex);
                    if (!validateRow(row)) {
                        errorRowIndexes.add(rowIndex);
                        System.out.println("Invalid row data:" + rowIndex);
                    } else {
                        Student student = processExcelRow(row, defaultPhotoId);
                        if (student != null) {
                            students.add(student);
                        }
                    }
                }, executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (students.isEmpty()) {
            return ResponseEntity.badRequest().body("No students found.");
        }

        Map<String, Integer> statistics = saveStudentsToDatabaseParallel(students);

        String responseMessage = buildResponseMessage(statistics, errorRowIndexes, start);

        return ResponseEntity.ok(responseMessage);
    }

    /**
     * Validates the file type to ensure it is either "xls" or "xlsx".
     *
     * @param fileType the file extension to check
     * @return true if the file type is valid, false otherwise
     */
    private boolean isValidFileType(String fileType) {
        return "xls".equalsIgnoreCase(fileType) || "xlsx".equalsIgnoreCase(fileType);
    }

    /**
     * Builds a detailed response message based on the processing results.
     *
     * @param statistics a map containing the counts of saved and existing records
     * @param errorRowIndexes a list of row indices with errors
     * @param start the start time of the processing
     * @return a formatted response message string
     */
    private String buildResponseMessage(Map<String, Integer> statistics, List<Integer> errorRowIndexes, Instant start) {
        String savedMessage = switch (statistics.get("saved")) {
            case 0 -> "No student records were saved.";
            case 1 -> "Successfully saved one student record in";
            default -> "Successfully saved " + statistics.get("saved") + " student records in";
        };
        String notRegisteredMessage = "";
        if (statistics.get("notRegistered") == 1){
            notRegisteredMessage = "One student account couldn't be created";
        } else if (statistics.get("notRegistered") > 1){
            notRegisteredMessage = statistics.get("notRegistered") + " students accounts couldn't be created";
        }

        String existedMessage = statistics.get("existed") > 0 ?
                statistics.get("existed") + (statistics.get("existed") == 1 ? " email already existed." : " emails already existed.") : "";

        String rowErrorMessage = getRowErrorMessage(errorRowIndexes);

        String timeSpent = statistics.get("saved") > 0 ?
                getTimeSpent(Duration.between(start, Instant.now())) : "";

        return String.join("\n", List.of(savedMessage + " " + timeSpent, existedMessage, notRegisteredMessage, rowErrorMessage));
    }

    /**
     * Saves a list of students to the database in parallel and integrates them with Keycloak.
     * This method performs the following steps:
     * 1. Removes duplicate emails from the input list to ensure each email is processed only once.
     * 2. Fetches all existing emails from the database and partitions the students into:
     *    - Existing students: those whose emails are already in the database.
     *    - New students: those whose emails are not found in the database.
     * 3. Asynchronously creates and updates student entities for new students in parallel.
     * 4. Registers the newly created students with Keycloak in parallel.
     * 5. Batch-saves the new student entities to the database.
     * 6. Returns a summary of the operation, including counts of saved and existing students.
     *
     * @param students the list of students to be processed.
     *                 Duplicates in the input list will be ignored, keeping only the first occurrence of each email.
     * @return a map containing statistics about the operation:
     *         - "saved": number of new students successfully saved to the database.
     *         - "existed": number of students that already existed in the database.
     * @throws RuntimeException if there is an error during student entity creation or Keycloak registration.
     */
    private Map<String, Integer> saveStudentsToDatabaseParallel(List<Student> students) {
        // Remove duplicate emails from input list
        Map<String, Student> uniqueStudentsByEmail = students.stream()
                .collect(Collectors.toMap(
                        Student::getEmail,
                        student -> student,
                        (existing, duplicate) -> existing // Keep the first occurrence
                ));

        // Fetch all existing emails from the database
        Set<String> allEmails = studentRepository.findAllEmails();

        // Partition unique students into new and existing based on their emails
        Map<Boolean, List<Student>> partitionedStudents = uniqueStudentsByEmail.values().stream()
                .collect(Collectors.partitioningBy(student -> allEmails.contains(student.getEmail())));

        // Extract new students for processing
        List<Student> newStudents = partitionedStudents.get(false);

        // Asynchronously create Student entities for new students
        List<CompletableFuture<Student>> creationTasks = newStudents.stream()
                .map(student -> CompletableFuture.supplyAsync(() -> createStudentEntity(student), executorService))
                .toList();

        // Wait for all tasks to finish and collect the results
        List<Student> studentEntities;
        try {
            studentEntities = creationTasks.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating student entities: " + e.getMessage(), e);
        }

        List<Student> registeredStudents = new ArrayList<>();
        List<Student> notRegisteredStudents = new ArrayList<>();

        // Track successfully registered students for rollback in case of an exception
        try {
            // Register students with Keycloak in parallel
            studentEntities.forEach(student -> {
                Response response = keycloakUserService.registerUserWithKeycloak(student);
                if (response.getStatus() == 201){
                    registeredStudents.add(student);
                } else {
                    notRegisteredStudents.add(student);
                }

                if (notRegisteredStudents.size() == 9){
                    System.out.println("Renewing admin session...");
                    renewSession();
                }
            });
        } catch (Exception e) {
            System.out.println("Registered students to rollback: " + registeredStudents.size());
            // Rollback Keycloak registrations
            if (!registeredStudents.isEmpty() || registeredStudents != null){
                registeredStudents.forEach(rollbackStudent -> {
                    try {
                        keycloakUserService.deleteKCUser(rollbackStudent.getId());
                    } catch (Exception rollbackException) {
                        // Log rollback failure (not rethrowing to ensure rollback attempts for all)
                        System.err.println("Failed to rollback Keycloak user: " + rollbackStudent.getEmail() +
                                ". Error: " + rollbackException.getMessage());
                    }
                });
            }

            // Recount statistics after rollback
            userService.countStudentsByProgramId();
            userService.countAdmins();

            throw new RuntimeException("Error occurred while registering students to Keycloak: " + e.getMessage(), e);
        }

        // Batch save new students to the database
        if (!registeredStudents.isEmpty()) {
            studentRepository.saveAll(registeredStudents);
        }

        // Prepare and return statistics
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("saved", registeredStudents.size());
        statistics.put("existed", partitionedStudents.get(true).size());
        statistics.put("notRegistered", notRegisteredStudents != null ? notRegisteredStudents.size() : 0);

        return statistics;
    }

    private void renewSession() {
        try {
            // Refresh the token
            keycloak.tokenManager().refreshToken();

            // Access the refreshed token if needed
            String newAccessToken = keycloak.tokenManager().getAccessTokenString();
            System.out.println("New Access Token: " + newAccessToken);

        } catch (Exception e) {
            // Handle exceptions (e.g., token expired, refresh token invalid, etc.)
            System.err.println("Error refreshing token: " + e.getMessage());
            throw new RuntimeException("Failed to renew Keycloak session", e);
        }
    }

    // Helper method to create a Student entity
    private Student createStudentEntity(Student dto) {
        Student student = new Student();

        // Generate new student ID
        String studentId = userDataProvider.generateStudentId(dto.getProgramId());

        student.setEmail(dto.getEmail());
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setProgramId(dto.getProgramId());
        student.setEnabled(true);
        student.setId(studentId);

        // Update program counts
        Student.updateProgramCountsFromDB(student.getProgramId(), 1L);

        return student;
    }

    private static String getTimeSpent(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        if (hours > 0) {
            return String.format("%02d hours %02d minutes %02d seconds and %03d milliseconds", hours, minutes, seconds, millis);
        } else if (minutes > 0) {
            return String.format("%02d minutes %02d seconds and %03d milliseconds", minutes, seconds, millis);
        } else {
            return String.format("%02d seconds and %03d milliseconds", seconds, millis);
        }
    }

    /**
     * Processes an Excel row to create an InfosStudentDTO object.
     * @param row The Excel row to process.
     * @param defaultPhotoId The user photo id.
     * @return InfosStudentDTO containing the student's information.
     * @throws IllegalArgumentException if the row data is invalid.
     */
    private Student processExcelRow(Row row, UUID defaultPhotoId) {
        try {
            String email = row.getCell(0).getStringCellValue().trim();
            String firstName = row.getCell(1).getStringCellValue().trim();
            String lastName = row.getCell(2).getStringCellValue().trim();
            ProgramID programId = ProgramID.valueOf(row.getCell(3).getStringCellValue().trim());

            Student student = new Student();
            student.setEmail(email);
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setProgramId(programId);
            student.setEnabled(true);
            student.setPhotoId(defaultPhotoId);

            return student;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid row data: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the headers of an Excel sheet against the expected format.
     * @param headerRow The header row of the sheet.
     * @return True if headers match the expected format, false otherwise.
     */
    private boolean validateHeaders(Row headerRow) {
        if (headerRow == null || headerRow.getLastCellNum() < DOC_HEADER.size()) return false;

        for (int i = 0; i < DOC_HEADER.size(); i++) {
            if (!DOC_HEADER.get(i).equalsIgnoreCase(headerRow.getCell(i).getStringCellValue().trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates a single row from the Excel sheet for correct data format.
     * @param row The Excel row to validate.
     * @return True if the row is valid, false otherwise.
     */
    private boolean validateRow(Row row) {
        try {
            if (row == null || row.getLastCellNum() < 4) return false;

            String email = row.getCell(0).getStringCellValue().trim();
            if (!isValidEmail(email)) return false;

            String firstName = row.getCell(1).getStringCellValue().trim();
            if (isInvalidName(firstName)) return false;

            String lastName = row.getCell(2).getStringCellValue().trim();
            if (isInvalidName(lastName)) return false;

            String programIdStr = row.getCell(3).getStringCellValue().trim();

            if (ProgramID.valueOf(programIdStr) == null) return false;

        } catch (Exception e) {
            System.err.println("Row validation error: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Validates an email address using a regex pattern.
     * @param email The email address to validate.
     * @return True if the email is valid, false otherwise.
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^(?=.{1,254}$)(?=.{1,64}@)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,63}$";
        return email != null && email.matches(emailRegex);
    }

    /**
     * Validates student's firstname and lastname using a regex pattern.
     * @param name The name to validate.
     * @return True if the firstname/lastname is valid, false otherwise.
     */
    private boolean isInvalidName(String name) {
        String nameRegex = "^(?=.{3,30}$)[A-Za-z]+(?:['-][A-Za-z]+)*(?:\\s[A-Za-z]+(?:['-][A-Za-z]+)*)*$";
        return name == null || !name.matches(nameRegex);
    }

    /**
     * Generates a user-friendly error message for rows with invalid data.
     * @param errorRowIndexes A list of row indexes with invalid data.
     * @return A formatted error message.
     */
    private String getRowErrorMessage(List<Integer> errorRowIndexes) {
        if (errorRowIndexes.isEmpty()) return "";

        int errorCount = errorRowIndexes.size();
        if (errorCount > 20) {
            return errorCount + " Excel rows have invalid data and weren't saved.";
        } else {
            String rows = errorRowIndexes.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            String recordWord = errorCount == 1 ? "record" : "records";
            return "Due to invalid data, " + errorCount + " " + recordWord + " weren't saved. Rows: " + rows;
        }
    }

    /**
     * Extracts the file extension from a filename.
     * @param filename The name of the file.
     * @return The file extension in lowercase, or an empty string if none exists.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

}
