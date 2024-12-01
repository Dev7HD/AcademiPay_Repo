package ma.dev7hd.userservice.services;

import lombok.AllArgsConstructor;
import ma.dev7hd.userservice.clients.NotificationClient;
import ma.dev7hd.userservice.clients.PhotoServiceClient;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosAdminDTO;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.*;
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
import org.apache.commons.io.FilenameUtils;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
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
    private final PhotoServiceClient photoServiceClient;
    private final NotificationClient notificationClient;

    private final Path PATH_TO_PHOTOS = Paths.get(System.getProperty("user.home"), "data", "photos");
    private final Path PATH_TO_REGISTRATION_PHOTOS = Paths.get(System.getProperty("user.home"), "data", "registrations_photos");

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final IClientService clientService;

    @Transactional
    @Override
    public User newUser(User newUser) {

        User savedUser = null;

        Optional<User> user = userRepository.findByEmail(newUser.getEmail());
        if (user.isPresent()) {
            throw new RuntimeException("User already exists");
        }

        // Save client to the database
        if (newUser instanceof Admin admin) {
            admin.setId(admin.getLastName().toUpperCase().substring(0,2) + (int)(Math.random() * 100000 + 100000));
            admin.setFirstName(admin.getFirstName());
            admin.setLastName(admin.getLastName());
            admin.setEmail(admin.getEmail());
            admin.setUserName(admin.getFirstName() + "." + newUser.getLastName());
            admin.setDepartmentName(admin.getDepartmentName());
            savedUser = userRepository.save(admin);
        } else if (newUser instanceof Student student) {
            // "123456" Must be changed with an auto increment value.
            student.setId(student.getLastName().toUpperCase().substring(0,2) + "123456");
            student.setFirstName(student.getFirstName());
            student.setLastName(student.getLastName());
            student.setEmail(student.getEmail());
            student.setUserName(student.getFirstName() + "." + student.getLastName());
            student.setProgramId(student.getProgramId());
            savedUser = userRepository.save(student);
        }
        clientService.saveClientAndRegisterWithKeycloak(savedUser);
        return savedUser;
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteUserById(String id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            clientService.deleteKCUser(user.getEmail());
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
                    admin.setFirstName(adminDTO.getFirstName());
                    admin.setLastName(adminDTO.getLastName());
                    admin.setDepartmentName(adminDTO.getDepartmentName());
                    Admin saved = adminRepository.save(admin);
                    return userMapper.convertUpdatedAdminToDto(saved);
                })
                .orElse(null);
    }



    @Transactional
    @Override
    public boolean toggleUserAccount(String email) throws ChangeSetPersister.NotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> clientService.toggleKCUserAccount(user.getEmail()))
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
    public ResponseEntity<InfosStudentDTO> getStudentByCode(String id) {
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
    public List<InfosStudentDTO> getStudentByProgram(ProgramID programID){
        List<Student> students = studentRepository.findStudentByProgramId(programID);
        if(students.isEmpty()){
            return List.of();
        } else {
            return students.stream()
                    .map(userMapper::convertStudentToDto)
                    .toList();
        }
    }

    @Transactional
    @Override
    public ResponseEntity<NewAdminDTO> saveAdmin(NewAdminDTO newAdminDTO) {
        Admin admin = newAdminProcessing(userMapper.convertNewAdminDtoToAdmin(newAdminDTO));
        clientService.saveClientAndRegisterWithKeycloak(admin);
        userRepository.save(admin);
        return ResponseEntity.ok().body(newAdminDTO);
    }

    @Transactional
    @Override
    public ResponseEntity<InfosStudentDTO> saveStudent(NewStudentDTO studentDTO, MultipartFile photo) throws IOException {
        Student student = newStudentProcessing(userMapper.convertStudentDtoToStudent(studentDTO));
        byte[] resizedPhoto = photoServiceClient.processPhoto(photo);
        String savePhotoUri = savePhoto(resizedPhoto, student.getId());
        student.setPhoto(savePhotoUri);
        Student saved = userRepository.save(student);
        return ResponseEntity.ok().body(userMapper.convertStudentToDto(saved));
    }

    /*@Override
    @Transactional
    public ResponseEntity<String> changePW(ChangePWDTO pwDTO){
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return processPasswordChange(user, pwDTO);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }*/

    /*@Override
    @Transactional
    public ResponseEntity<String> resetPW(String targetUserId){
        Optional<User> optionalTargetUser = userRepository.findById(targetUserId);
        String loggedInUserId = iUserDataProvider.getCurrentUserId();
        if (optionalTargetUser.isPresent()) {
            User targetUser = optionalTargetUser.get();
            return processPasswordReset(targetUser, loggedInUserId);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }*/

    @Override
    public List<InfosAdminDTO> getAdmins(){
        List<Admin> admins = adminRepository.findAll();
        return admins.stream().map(userMapper::convertAdminToDto).toList();
    }

    @Override
    public Page<InfosAdminDTO> getAdminsByCriteria(String id, String email, String firstName, String lastName, DepartmentName departmentName, int page, int size){
        Page<Admin> admins = adminRepository.findByFilter(id, email, firstName, lastName, departmentName, PageRequest.of(page, size));
        return userMapper.convertPageableAdminToDTO(admins);
    }

    @Override
    public Page<InfosStudentDTO> getStudentsByCriteriaAsAdmin(String id, String email, String firstName, String lastName, ProgramID programID, int page, int size){
        Page<Student> students = studentRepository.findByFilter(id, email, firstName, lastName, programID, PageRequest.of(page, size));
        return userMapper.convertPageableStudentToDTO(students);
    }

    @Transactional
    @Override
    public ResponseEntity<String> registerStudent(NewPendingStudentDTO pendingStudentDTO, MultipartFile photo) throws IOException {
        boolean userIsExistByEmail = userRepository.existsByEmail(pendingStudentDTO.getEmail());
        boolean pendingStudentExistByEmail = pendingStudentRepository.existsById(pendingStudentDTO.getEmail());
        boolean bannedExistById = banedRegistrationRepository.existsById(pendingStudentDTO.getEmail());

        if (userIsExistByEmail || pendingStudentExistByEmail || bannedExistById) {
            return ResponseEntity.badRequest().body("Email or Code already in use or banned");
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
        notificationClient.pushStudentRegistration(savedPendingStudent);

        return ResponseEntity.ok().body("The registration was successful.");
    }

    @Override
    public Page<PendingStudent> getPendingStudent(String email, int page, int size){
        Page<PendingStudent> pendingStudents = pendingStudentRepository.findByPendingStudentsByFilter(email, PageRequest.of(page, size));
        if (!pendingStudents.getContent().isEmpty()){
            notificationClient.adminNotificationSeen(pendingStudents.getContent().getFirst().getEmail());
        }
        return pendingStudents;
    }

    @Override
    public ResponseEntity<PendingStudent> getPendingStudentByEmail(String email){
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findById(email);
        if (optionalPendingStudent.isPresent()) {
            PendingStudent pendingStudent = optionalPendingStudent.get();
            notificationClient.adminNotificationSeen(pendingStudent.getEmail());
            return ResponseEntity.ok(pendingStudent);
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> approvingStudentRegistration( String email) throws IOException {
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findById(email);
        if (optionalPendingStudent.isPresent()) {
            if (!studentRepository.existsByEmail(optionalPendingStudent.get().getEmail())){
                PendingStudent pendingStudent = optionalPendingStudent.get();

                Student approvedStudent = newStudentProcessing(convertPendingStudentToStudent(pendingStudent));

                String photo = photoProcessingForApprovedStudent(pendingStudent.getPhoto(), approvedStudent.getId());

                approvedStudent.setPhoto(photo);

                Files.deleteIfExists(Path.of(URI.create(pendingStudent.getPhoto())));
                Student savedStudent = studentRepository.save(approvedStudent);
                pendingStudentRepository.delete(pendingStudent);
                return ResponseEntity.ok().body(convertStudentToDto(savedStudent));
            }
            return ResponseEntity.badRequest().body("Student already registered.");
        } else {
            return ResponseEntity.badRequest().body("Email is not correct.");
        }
    }

    private String photoProcessingForApprovedStudent(String pendingStudentPhotoUri , String studentCode) throws IOException {
        byte[] photo = Files.readAllBytes(Path.of(URI.create(pendingStudentPhotoUri)));
        byte[] resizedPhoto = imageService.resizeImageWithAspectRatio(photo);
        String savedPhoto = savePhoto(resizedPhoto, studentCode);
        deletePhoto(pendingStudentPhotoUri);
        return savedPhoto;
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
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findById(email);
        Optional<User> optionalUser = userRepository.findById(userEmail);
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
            student.setFirstName(studentDTO.getFirstName());
            student.setLastName(studentDTO.getLastName());
            student.setProgramId(studentDTO.getProgramId());
            Student savedStudentInfo = studentRepository.save(student);
            Student.updateProgramCountsFromDB(student.getProgramId(), -1.0);
            Student.updateProgramCountsFromDB(studentDTO.getProgramId(), 1.0);
            return ResponseEntity.ok(userMapper.convertStudentToDto(savedStudentInfo));
        }
        return ResponseEntity.badRequest().build();
    }

    @Override
    public byte[] updateStudentPhoto(MultipartFile photo) throws IOException {
        User user = iUserDataProvider.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId();

        if (userId == null) {
            throw new IllegalStateException("User ID is null");
        }

        // Resize the photo
        byte[] resizedPhoto = imageService.resizeImageWithAspectRatio(photo.getBytes());

        // Save photo and update user's photo path
        String savedPhotoPath = savePhoto(resizedPhoto, userId);
        if (savedPhotoPath == null) {
            throw new IOException("Failed to save photo");
        }

        user.setPhoto(savedPhotoPath);
        userRepository.save(user);

        // Read and return the saved photo bytes
        return Files.readAllBytes(Path.of(URI.create(savedPhotoPath)));
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
                            approvedStudent = newStudentProcessing(convertPendingStudentToStudent(registration));
                            String photo = photoProcessingForApprovedStudent(registration.getPhoto(), approvedStudent.getId());
                            approvedStudent.setPhoto(photo);
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
        List<String> allPhotos = pendingStudentRepository.findAllPhotosById(emails);

        // Handle potential photo deletion errors
        allPhotos.forEach(photo -> {
            try {
                deletePhoto(photo);
            } catch (IOException e) {
                System.out.println("Failed to delete photo: " + photo + ". Error: " + e.getMessage());
            }
        });

        // Proceed to delete registrations after attempting photo deletion
        pendingStudentRepository.deleteAllById(emails);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMultipleUsers(List<String> ids) {
        List<User> users = userRepository.findAllById(ids);
        if (users.isEmpty()) {
            return ResponseEntity.badRequest().body("Emails provided are not valid.");
        }

        // Parallel processing of photo deletion for Student users
        users.stream()
                .filter(user -> user instanceof Student && user.getPhoto() != null)
                .forEach(user -> {
                    try {
                        deletePhoto(user.getPhoto());
                    } catch (IOException e) {
                        System.out.println("Failed to delete photo for user: " + user.getEmail());
                    }
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


    /*@Override
    public ResponseEntity<String> resetPasswordToMultipleUsers(List<String> ids) {
        List<User> users = userRepository.findAllById(ids);
        users.forEach(user -> {

        });
        userRepository.saveAll(users);
        return ResponseEntity.ok(users.size() + " password(s) has been reset successfully.");
    }*/

    @Override
    public void toggleUsersAccounts(List<String> ids){
        List<User> users = userRepository.findAllById(ids);
        if (!users.isEmpty()) {
            users.forEach(user -> {
                try {
                    toggleUserAccount(user.getEmail());
                } catch (ChangeSetPersister.NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public PictureDTO getProfilePicture(String id) throws IOException {
        Optional<Student> optionalStudent = studentRepository.findById(id);
        if (optionalStudent.isPresent()) {
            Student student = optionalStudent.get();
            PictureDTO pictureDTO = new PictureDTO();
            pictureDTO.setPicture(Files.readAllBytes(Path.of(URI.create(student.getPhoto()))));
            pictureDTO.setPictureName(student.getId());
            return pictureDTO;
        }
        return null;
    }

    //PRIVATE METHODS

    private Student newStudentProcessing(Student student) throws IOException {
        String studentId = iUserDataProvider.generateStudentId(student.getProgramId(), null);

        student.setPasswordChanged(false);
        student.setEnabled(true);
        student.setId(studentId);
        student.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        Student.updateProgramCountsFromDB(student.getProgramId(),1.0);
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

    private Admin newAdminProcessing(Admin admin){
        String adminId = iUserDataProvider.generateAdminId(admin.getLastName());
        admin.setId(adminId);
        return admin;
    }



    private ResponseEntity<String> processPasswordChange(User user, String newPW) {

        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        // Retrieve the user by username
        String username = user.getEmail();
        String userId = usersResource.searchByEmail(username, true).get(0).getId(); // Ensure the username exists

        // Create the new password representation
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPW);
        credential.setTemporary(false); // Set to true if you want the user to change it on next login

        // Set the new password for the user
        usersResource.get(userId).resetPassword(credential);

        System.out.println("Password updated successfully for user: " + username);
        return ResponseEntity.ok("Password updated successfully for user: " + username);
    }

    private ResponseEntity<String> processPasswordReset(User targetUser, String loggedInUserId) {
            processPasswordChange(targetUser, DEFAULT_PASSWORD);
            return ResponseEntity.ok("Password has been reset");
    }


}
