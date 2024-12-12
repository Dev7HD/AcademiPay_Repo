package ma.dev7hd.userservice.services;

import lombok.AllArgsConstructor;
import ma.dev7hd.userservice.clients.NotificationClient;
import ma.dev7hd.userservice.clients.FileServiceClient;
import ma.dev7hd.userservice.converters.ByteArrayMultipartFile;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosAdminDTO;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.*;
import ma.dev7hd.userservice.dtos.otherDTOs.ChangePWDTO;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.entities.User;
import ma.dev7hd.userservice.entities.registrations.BanedRegistration;
import ma.dev7hd.userservice.entities.registrations.PendingStudent;
import ma.dev7hd.userservice.enums.DepartmentName;
import ma.dev7hd.userservice.enums.ProgramID;
import ma.dev7hd.userservice.mappers.IUserMapper;
import ma.dev7hd.userservice.repositories.registrations.BanedRegistrationRepository;
import ma.dev7hd.userservice.repositories.registrations.PendingStudentRepository;
import ma.dev7hd.userservice.repositories.users.AdminRepository;
import ma.dev7hd.userservice.repositories.users.StudentRepository;
import ma.dev7hd.userservice.repositories.users.UserRepository;
import ma.dev7hd.userservice.services.global.IUserDataProvider;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;
    private final PendingStudentRepository pendingStudentRepository;
    private final BanedRegistrationRepository banedRegistrationRepository;
    private final IUserMapper userMapper;
    private final FileServiceClient photoServiceClient;
    private final NotificationClient notificationClient;
    private final IUserDataProvider iUserDataProvider;
    private final IClientService clientService;
    private final FileServiceClient fileServiceClient;

    private final Path PATH_TO_PHOTOS = Paths.get(System.getProperty("user.home"), "data", "photos");
    private final Path PATH_TO_REGISTRATION_PHOTOS = Paths.get(System.getProperty("user.home"), "data", "registrations_photos");
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Transactional
    @Override
    public Admin newAdmin(Admin admin, MultipartFile photo) throws IOException {
        Optional<User> user = userRepository.findByEmail(admin.getEmail());
        if (user.isPresent()) {
            throw new RuntimeException("User already exists");
        }

        if(photo == null){
            throw new IOException("User photo mustn't be null");
        }

        String adminId = iUserDataProvider.generateAdminId(admin.getLastName());

        System.out.println("ADMIN INIT...");

        admin.setId(adminId);

        UUID photoId = fileServiceClient.processUserPhoto(photo, adminId);
        admin.setPhotoId(photoId);

        admin.setEnabled(true);

        clientService.registerUserWithKeycloak(admin);

        Admin savedAdmin = userRepository.save(admin);
        System.out.println(savedAdmin);

        return savedAdmin;

    }

    @Transactional
    @Override
    public Student newStudent(Student student, MultipartFile photo) throws IOException {

        Optional<User> user = userRepository.findByEmail(student.getEmail());
        if (user.isPresent()) {
            throw new RuntimeException("User already exists");
        }

        if(photo == null){
            throw new IOException("User photo mustn't be null");
        }

        System.out.println("STUDENT INIT...");
        String studentId = iUserDataProvider.generateStudentId(student.getProgramId());

        student.setId(studentId);
        UUID photoId = fileServiceClient.processUserPhoto(photo, studentId);
        student.setPhotoId(photoId);

        student.setEnabled(true);

        System.out.println("*******************");
        System.out.println("photo id "+photoId);
        System.out.println("student photo id "+student.getPhotoId());
        System.out.println("*******************");

        clientService.registerUserWithKeycloak(student);

        Student savedStudent = userRepository.save(student);
        System.out.println(savedStudent);


        return savedStudent;
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteUserById(String id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            clientService.deleteKCUser(user.getId());
            userRepository.delete(user);
            return ResponseEntity.ok().body("User deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    @Transactional
    @Override
    public UpdateAdminInfoDTO updateAdmin(UpdateAdminInfoDTO adminDTO){
        return adminRepository.findById(adminDTO.getId())
                .map(admin -> {
                    clientService.updateKCUser(admin);
                    if(adminDTO.getEmail() != null && !adminDTO.getEmail().isBlank() && !adminDTO.getEmail().isEmpty()) admin.setEmail(adminDTO.getEmail());
                    if(adminDTO.getFirstName() != null && !adminDTO.getFirstName().isBlank() && !adminDTO.getFirstName().isEmpty()) admin.setFirstName(adminDTO.getFirstName());
                    if(adminDTO.getLastName() != null && !adminDTO.getLastName().isBlank() && !adminDTO.getLastName().isEmpty()) admin.setLastName(adminDTO.getLastName());
                    if(adminDTO.getDepartmentName() != null) admin.setDepartmentName(adminDTO.getDepartmentName());
                    Admin saved = adminRepository.save(admin);
                    return userMapper.convertUpdatedAdminToDto(saved);
                })
                .orElse(null);
    }

    @Transactional
    @Override
    public boolean toggleUserAccount(String email) throws ChangeSetPersister.NotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> {
                    boolean isEnabled = clientService.toggleKCUserAccount(user.getEmail());
                    user.setEnabled(isEnabled);
                    userRepository.save(user);
                    return isEnabled;
                })
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
    }

    @Override
    public List<InfosStudentDTO> getAllStudents(){
        List<Student> students = studentRepository.findAll();
        return students.stream()
                .map(userMapper::convertStudentToDto)
                .toList();
    }

    @Override
    public ResponseEntity<InfosStudentDTO> getStudentById(String id) {
        Optional<Student> optionalStudent = studentRepository.findById(id);
        if (optionalStudent.isPresent()) {
            Student student = optionalStudent.get();
            return ResponseEntity.ok(userMapper.convertStudentToDto(student));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<InfosStudentDTO> getStudentByEmail(String email) {
        Optional<Student> optionalStudent = studentRepository.findByEmail(email);
        if (optionalStudent.isPresent()) {
            Student student = optionalStudent.get();
            return ResponseEntity.ok(userMapper.convertStudentToDto(student));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> changePW(ChangePWDTO pwDTO){
        User loggedUser = iUserDataProvider.getCurrentUser().orElse(null);
        if (loggedUser != null) {
            boolean isOldPWCorrect = clientService.verifyPassword(loggedUser.getEmail(), pwDTO.getOldPassword());
            if(isOldPWCorrect){
                return processPasswordChange(loggedUser, pwDTO.getNewPassword());
            }
            return ResponseEntity.badRequest().body("Old password not correct.");
        } else {
            return ResponseEntity.status(401).body("Not authenticated/User not found.");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> resetPW(String targetUserId){
        Optional<User> optionalTargetUser = userRepository.findById(targetUserId);
        if (optionalTargetUser.isPresent()) {
            User targetUser = optionalTargetUser.get();
            return processPasswordReset(targetUser);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public List<InfosAdminDTO> getAdmins(){
        List<Admin> admins = adminRepository.findAll();
        return admins.stream().map(userMapper::convertAdminToDto).toList();
    }

    @Override
    public Page<InfosAdminDTO> getAdminsByCriteria(String id, String email, String firstName, String lastName, DepartmentName departmentName, int page, int size){
        Page<Admin> admins = adminRepository.findByFilter(id.toLowerCase(), email.toLowerCase(), firstName.toLowerCase(), lastName.toLowerCase(), departmentName, PageRequest.of(page, size));
        return userMapper.convertPageableAdminToDTO(admins);
    }

    @Override
    public Page<InfosStudentDTO> getStudentsByCriteriaAsAdmin(String id, String email, String firstName, String lastName, ProgramID programID, int page, int size){
        Page<Student> students = studentRepository.findByFilter(id.toLowerCase(), email.toLowerCase(), firstName.toLowerCase(), lastName.toLowerCase(), programID, PageRequest.of(page, size));
        return userMapper.convertPageableStudentToDTO(students);
    }

    @Transactional
    @Override
    public ResponseEntity<String> registerStudent(NewPendingStudentDTO pendingStudentDTO, MultipartFile photo) throws IOException {
        boolean userIsExistByEmail = userRepository.existsByEmail(pendingStudentDTO.getEmail());
        System.out.println("userIsExistByEmail: " + userIsExistByEmail);
        boolean pendingStudentExistByEmail = pendingStudentRepository.existsById(pendingStudentDTO.getEmail());
        System.out.println("pendingStudentExistByEmail: " + pendingStudentExistByEmail);
        boolean bannedExistById = banedRegistrationRepository.existsById(pendingStudentDTO.getEmail());
        System.out.println("bannedExistById: " + bannedExistById);

        if (userIsExistByEmail || pendingStudentExistByEmail || bannedExistById) {
            return ResponseEntity.badRequest().body("Email or Code already in use or banned");
        }

        if (photo == null){
            return ResponseEntity.badRequest().body("You must provide your profile picture");
        }
        String fileName = UUID.randomUUID().toString();
        String extension = FilenameUtils.getExtension(photo.getOriginalFilename());
        Path filePath = PATH_TO_REGISTRATION_PHOTOS.resolve(fileName + "." + extension);
        if (!Files.exists(PATH_TO_REGISTRATION_PHOTOS)) {
            Files.createDirectories(PATH_TO_REGISTRATION_PHOTOS);
        }
        try {
            Files.copy(photo.getInputStream(), filePath);
        } catch (IOException e) {
            return null;
        }

        File file = new File(filePath.toString());
        String photoUri = file.toURI().toString();

        PendingStudent pendingStudent = userMapper.convertPendingStudentToDto(pendingStudentDTO);

        pendingStudent.setPhoto(photoUri);
        pendingStudent.setRegisterDate(new Date());

        PendingStudent savedPendingStudent = pendingStudentRepository.save(pendingStudent);
//        notificationClient.pushStudentRegistration(savedPendingStudent);

        return ResponseEntity.ok().body("The registration was successful. Please wait to be approved.");
    }

    @Override
    public Page<PendingStudent> getPendingStudent(String email, int page, int size){
        Page<PendingStudent> pendingStudents = pendingStudentRepository.findByPendingStudentsByFilter(email, PageRequest.of(page, size));
        /*if (!pendingStudents.getContent().isEmpty()){
            notificationClient.adminNotificationSeen(pendingStudents.getContent().getFirst().getEmail());
        }*/
        return pendingStudents;
    }

    @Override
    public ResponseEntity<PendingStudent> getPendingStudentByEmail(String email){
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findById(email);
        if (optionalPendingStudent.isPresent()) {
            PendingStudent pendingStudent = optionalPendingStudent.get();
//            notificationClient.adminNotificationSeen(pendingStudent.getEmail());
            return ResponseEntity.ok(pendingStudent);
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> approvingStudentRegistration(String email) throws IOException {
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findById(email);
        if (optionalPendingStudent.isPresent()) {
            if (!studentRepository.existsByEmail(optionalPendingStudent.get().getEmail())){
                PendingStudent pendingStudent = optionalPendingStudent.get();

                Student studentToBeApproved = newStudentProcessing(userMapper.convertPendingStudentToStudent(pendingStudent));

                UUID photoId = photoProcessingForApprovedStudent(pendingStudent.getPhoto(), studentToBeApproved.getId());

                studentToBeApproved.setPhotoId(photoId);

                Student savedStudent = studentRepository.save(studentToBeApproved);
                pendingStudentRepository.delete(pendingStudent);
                return ResponseEntity.ok().body(userMapper.convertStudentToDto(savedStudent));
            }
            return ResponseEntity.badRequest().body("Student already registered.");
        } else {
            return ResponseEntity.badRequest().body("Email is not correct.");
        }
    }

    private UUID photoProcessingForApprovedStudent(String pendingStudentPhotoUri , String studentId) throws IOException {
        byte[] photo = Files.readAllBytes(Path.of(URI.create(pendingStudentPhotoUri)));
        MultipartFile multiPartPhoto = new ByteArrayMultipartFile(photo, "photo", "Original photo", "image/jpeg");

        UUID photoId = fileServiceClient.processUserPhoto(multiPartPhoto, studentId);
        if(photoId != null){
            deletePhoto(pendingStudentPhotoUri);
            return photoId;
        } else {
            throw new IOException("Processing photo error.");
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> declineStudentRegistration(String email) throws IOException {
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findById(email);
        if (optionalPendingStudent.isPresent()) {
            PendingStudent pendingStudent = optionalPendingStudent.get();

            deletePhoto(pendingStudent.getPhoto());

            pendingStudentRepository.delete(pendingStudent);

            return ResponseEntity.ok().body("The registration was declined successfully.");
        }
        return ResponseEntity.badRequest().body("Email is not correct.");
    }

    private void deletePhoto(String photoUri) throws IOException {
        Files.deleteIfExists(Path.of(URI.create(photoUri)));
    }

    @Transactional
    @Override
    public ResponseEntity<String> banStudentRegistration(String email) throws IOException {
        Optional<Admin> optionalUser = iUserDataProvider.getCurrentAdmin();
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findById(email);
        if (optionalPendingStudent.isPresent() && optionalUser.isPresent() && optionalUser.get() instanceof Admin admin) {
            PendingStudent pendingStudent = optionalPendingStudent.get();

            BanedRegistration banedRegistration = userMapper.convertPendingStudentToBanedRegistration(pendingStudent);
            banedRegistration.setBanDate(new Date());
            banedRegistration.setAdminBanner(admin);

            banedRegistrationRepository.save(banedRegistration);

            deletePhoto(pendingStudent.getPhoto());

            pendingStudentRepository.delete(pendingStudent);

            return ResponseEntity.ok().body("The registration was banned successfully.");
        }
        return ResponseEntity.badRequest().build();
    }

    @Transactional
    @Override
    public ResponseEntity<InfosStudentDTO> updateStudentInfo(UpdateStudentInfoDTO studentDTO){
        Optional<Student> optionalStudent = studentRepository.findById(studentDTO.getId());
        if (optionalStudent.isPresent()) {
            Student student = optionalStudent.get();
            if (studentDTO.getEmail() != null && !studentDTO.getEmail().isEmpty() && !studentDTO.getEmail().isBlank()) student.setEmail(studentDTO.getEmail());
            if(studentDTO.getFirstName() != null && !studentDTO.getFirstName().isEmpty() && !studentDTO.getFirstName().isBlank()) student.setFirstName(studentDTO.getFirstName());
            if(studentDTO.getLastName() != null && !studentDTO.getLastName().isEmpty() && !studentDTO.getLastName().isBlank()) student.setLastName(studentDTO.getLastName());
            if(studentDTO.getProgramId() != null) student.setProgramId(studentDTO.getProgramId());
            Student savedStudentInfo = studentRepository.save(student);

            if (studentDTO.getProgramId() != null){
                Student.updateProgramCountsFromDB(student.getProgramId(), -1L);
                Student.updateProgramCountsFromDB(studentDTO.getProgramId(), 1L);
            }
            return ResponseEntity.ok(userMapper.convertStudentToDto(savedStudentInfo));
        }
        return ResponseEntity.badRequest().build();
    }

    @Override
    public ResponseEntity<String> updateStudentPhoto(MultipartFile photo) throws IOException {
        User user = iUserDataProvider.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId();

        if (userId == null) {
            throw new IllegalStateException("User ID is null");
        }

        // Resize the photo
        UUID photoId = fileServiceClient.processUserPhoto(photo, userId);

        if(photoId != null){
            return ResponseEntity.ok("User photo successfully updated.");
        } else {
            return ResponseEntity.badRequest().body("User photo update failed");
        }
    }


    @Override
    @Transactional
    public ResponseEntity<String> approveMultipleRegistrations(List<String> emails) {
        try {
            List<PendingStudent> allPendingStudents = pendingStudentRepository.findAllById(emails);
            StringBuilder message = new StringBuilder();
            if (allPendingStudents.isEmpty()) {
                message.append("No registration was approved. Registrations provided doesn't exist!");
                return ResponseEntity.ok(message.toString());
            }

            Set<String> allEmails = studentRepository.findAllEmails();

            // Parallel processing for approving registrations
            CompletableFuture<List<Student>> approveFuture = CompletableFuture.supplyAsync(() -> allPendingStudents.parallelStream()
                    .filter(registration ->
                            !allEmails.contains(registration.getEmail())
                    ).map(registration -> {
                        Student approvedStudent ;
                        try {
                            approvedStudent = newStudentProcessing(userMapper.convertPendingStudentToStudent(registration));
                            UUID photoId = photoProcessingForApprovedStudent(registration.getPhoto(), approvedStudent.getId());
                            approvedStudent.setPhotoId(photoId);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        return approvedStudent;
                    }).collect(Collectors.toList()), executorService);

            List<Student> studentsToApprove = approveFuture.join(); // Wait for completion

            studentRepository.saveAll(studentsToApprove);

            List<String> registrationsEmails = studentsToApprove.stream()
                    .map(Student::getEmail)
                    .collect(Collectors.toList());

            pendingStudentRepository.deleteAllById(registrationsEmails);

            int approvedCount = studentsToApprove.size();
            int foundCount = allPendingStudents.size();
            int totalProvided = emails.size();

            if (approvedCount == 0) {
                message.append("No registration was approved!");
            } else if (approvedCount < foundCount && foundCount < totalProvided) {
                message.append(approvedCount).append(" registration(s) approved, ")
                        .append(foundCount - approvedCount).append(" cannot be approved, ")
                        .append(totalProvided - foundCount).append(" not found.");
            } else if (approvedCount < foundCount && foundCount == totalProvided) {
                message.append(approvedCount).append(" registration(s) approved, ")
                        .append(foundCount - approvedCount).append(" cannot be approved.");
            } else if (approvedCount == foundCount && approvedCount < totalProvided) {
                message.append(approvedCount).append(" registration(s) approved, ")
                        .append(totalProvided - approvedCount).append(" not found.");
            } else {
                message.append("All selected registrations were approved.");
            }

            return ResponseEntity.ok(message.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing registrations: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> banMultipleRegistrations(List<String> emails) {
        try {
            List<PendingStudent> pendingStudents = pendingStudentRepository.findAllById(emails);
            if (pendingStudents.isEmpty()) return ResponseEntity.badRequest().body("No registration was found!");

            Optional<Admin> currentAdmin = iUserDataProvider.getCurrentAdmin();
            if (currentAdmin.isPresent()) {
                Admin admin = currentAdmin.get();

                // Parallel processing for banning registrations
                CompletableFuture<Void> banFuture = CompletableFuture.runAsync(() -> {
                    try {
                        List<BanedRegistration> registrationsToBan = pendingStudents.parallelStream()
                                .map(pendingStudent -> BanedRegistration.builder()
                                        .banDate(new Date())
                                        .registerDate(pendingStudent.getRegisterDate())
                                        .email(pendingStudent.getEmail())
                                        .firstName(pendingStudent.getFirstName())
                                        .lastName(pendingStudent.getLastName())
                                        .programID(pendingStudent.getProgramID())
                                        .adminBanner(admin)
                                        .build())
                                .collect(Collectors.toList());

                        pendingStudents.forEach(registration -> {
                            try {
                                deletePhoto(registration.getPhoto());
                            } catch (IOException e) {
                                System.out.println("Failed to delete photo for " + registration.getEmail());
                            }
                        });

                        banedRegistrationRepository.saveAll(registrationsToBan);
                        pendingStudentRepository.deleteAll(pendingStudents);
                    } catch (Exception e) {
                        System.out.println("Error occurred during banning registrations");
                    }
                }, executorService);

                banFuture.join();

                return ResponseEntity.ok(pendingStudents.size() + " registration(s) are banned.");
            }
            return ResponseEntity.badRequest().body("The request must be done by a valid admin.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ban registrations error: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void declineMultipleRegistrations(List<String> emails) {
        List<PendingStudent> pendingStudentList = pendingStudentRepository.findAllById(emails);

        // Handle potential photo deletion errors
        pendingStudentList.forEach(p -> {
            try {
                deletePhoto(p.getPhoto());
            } catch (IOException e) {
                System.err.println("Failed to delete photo: " + p.getPhoto() + ". Error: " + e.getMessage());
            }
        });

        // Proceed to delete registrations after attempting photo deletion
        pendingStudentRepository.deleteAll(pendingStudentList);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMultipleUsers(List<String> ids) {
        List<User> users = userRepository.findAllByIdIn(ids);
        if (users.isEmpty()) {
            return ResponseEntity.badRequest().body("Emails provided are not valid.");
        }

        // Parallel processing of photo deletion for Student users
        deletePhotos(ids);

        users.parallelStream().forEach(user -> {
            clientService.deleteKCUser(user.getId());
        });

        userRepository.deleteAll(users);

        // Construct response message
        int providedCount = ids.size();
        int usersFoundCount = users.size();
        String message = providedCount == usersFoundCount
                ? "All provided users were successfully deleted."
                : String.format("%d users were deleted, but %d emails were not found.", usersFoundCount, providedCount - usersFoundCount);

        return ResponseEntity.ok(message);
    }

    private void deletePhotos(List<String> usersIds){
        fileServiceClient.deletePhotos(usersIds);
    }


    @Override
    public ResponseEntity<String> resetPasswordToMultipleUsers(List<String> ids) {
        List<User> users = userRepository.findAllById(ids);
        users.forEach(user -> {
            clientService.changeUserPassword(user.getId());
        });
        userRepository.saveAll(users);
        return ResponseEntity.ok(users.size() + " password(s) has been reset successfully.");
    }

    @Override
    public void toggleUsersAccounts(List<String> ids){
        List<User> users = userRepository.findAllById(ids);
        if (!users.isEmpty()) {
            users.parallelStream().forEach(user -> {
                try {
                    toggleUserAccount(user.getEmail());
                } catch (ChangeSetPersister.NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public ResponseEntity<byte[]> getProfilePicture(String id) {
        Optional<User> optionalUser = userRepository.findById(id);
        return optionalUser.map(student -> fileServiceClient.getUserPhoto(student.getId())).orElse(null);
    }

    //PRIVATE METHODS

    private Student newStudentProcessing(Student student) throws IOException {
        String studentId = iUserDataProvider.generateStudentId(student.getProgramId());
        student.setId(studentId);
        student.setEnabled(true);
        clientService.registerUserWithKeycloak(student);
        Student.updateProgramCountsFromDB(student.getProgramId(),1L);
        return student;
    }

    private String savePhoto(byte[] photo, String studentCode){
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

    private ResponseEntity<String> processPasswordChange(User user, String newPW) {
        clientService.changeUserPassword(user.getId(), newPW);
        System.out.println("Password updated successfully for user ID: " + user.getId());
        return ResponseEntity.ok("Password updated successfully for user ID: " + user.getId());
    }

    private ResponseEntity<String> processPasswordReset(User user) {
        clientService.changeUserPassword(user.getId());
        System.out.println("Password has benn reset successfully for user ID: " + user.getId());
        return ResponseEntity.ok("Password has benn reset successfully for user ID: " + user.getId());
    }

}
