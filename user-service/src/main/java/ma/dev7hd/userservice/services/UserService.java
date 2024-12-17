package ma.dev7hd.userservice.services;

import lombok.AllArgsConstructor;
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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
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
import java.util.function.Consumer;
import java.util.function.Function;
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
    private final IUserDataProvider iUserDataProvider;
    private final IClientService clientService;
    private final FileServiceClient fileServiceClient;

    private final Path PATH_TO_PHOTOS = Paths.get(System.getProperty("user.home"), "data", "photos");
    private final Path PATH_TO_REGISTRATION_PHOTOS = Paths.get(System.getProperty("user.home"), "data", "registrations_photos");
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Transactional
    @Override
    public Admin newAdmin(Admin admin, MultipartFile photo) throws IOException {

        checkProvidedUserData(admin.getEmail(), photo);

        //Generate new Admin ID
        String adminId = iUserDataProvider.generateAdminId(admin.getLastName());
        admin.setId(adminId);

        //Convert, compress and save provided user picture
        UUID photoId = fileServiceClient.processUserPhoto(photo, adminId);
        admin.setPhotoId(photoId);

        //Make user account enabled in database and keycloak
        admin.setEnabled(true);

        //Create new user account in Keycloak
        clientService.registerUserWithKeycloak(admin);

        //Save user in database
        return userRepository.save(admin);

    }

    @Transactional
    @Override
    public Student newStudent(Student student, MultipartFile photo) throws IOException {

        checkProvidedUserData(student.getEmail(), photo);

        //Generate new Student ID
        String studentId = iUserDataProvider.generateStudentId(student.getProgramId());
        student.setId(studentId);

        //Convert, compress and save provided user picture
        UUID photoId = fileServiceClient.processUserPhoto(photo, studentId);
        student.setPhotoId(photoId);

        //Make user account enabled in database and keycloak
        student.setEnabled(true);

        //Create new user account in Keycloak
        clientService.registerUserWithKeycloak(student);

        //Save user in database
        Student savedStudent = userRepository.save(student);
        System.out.println(savedStudent);


        return savedStudent;
    }

    private void checkProvidedUserData(String email, MultipartFile photo) {

        //Check if provided email exist in database
        boolean isExist = userRepository.existsByEmailIgnoreCase(email);
        if (isExist) {
            throw new RuntimeException("Email already exists");
        }

        // Check if provided photo is null
        if(photo == null || photo.isEmpty()){
            throw new IllegalArgumentException("User photo mustn't be null");
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteUserById(String id) {
        Optional<User> optionalUser = userRepository.findByIdIgnoreCase(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            int statusCode = clientService.deleteKCUser(user.getId());
            System.out.println("Status code: " + statusCode);
            userRepository.delete(user);
            return ResponseEntity.ok().body("User deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    @Transactional
    @Override
    public ResponseEntity<UpdateAdminInfoDTO> updateAdminInfo(UpdateAdminInfoDTO adminDTO) {
        return updateEntity(adminDTO, adminRepository, admin -> {
            if (adminDTO.getDepartmentName() != null) {
                admin.setDepartmentName(adminDTO.getDepartmentName());
            }
            clientService.updateKCUser(admin);
        }, userMapper::convertUpdatedAdminToDto);
    }

    @Transactional
    @Override
    public ResponseEntity<InfosStudentDTO> updateStudentInfo(UpdateStudentInfoDTO studentDTO) {
        return updateEntity(studentDTO, studentRepository, student -> {
            if (studentDTO.getProgramId() != null) {
                Student.updateProgramCountsFromDB(student.getProgramId(), -1L);
                Student.updateProgramCountsFromDB(studentDTO.getProgramId(), 1L);
                student.setProgramId(studentDTO.getProgramId());
            }
            clientService.updateKCUser(student);
        }, userMapper::convertStudentToDto);
    }

    private <T extends User, D, R> ResponseEntity<R> updateEntity(D dto, JpaRepository<T, String> repository,
                                                                  Consumer<T> additionalUpdates,
                                                                  Function<T, R> toDtoMapper) {
        return repository.findById(getId(dto).toUpperCase())
                .map(entity -> {
                    updateUserFields(dto, entity);
                    additionalUpdates.accept(entity);
                    T savedEntity = repository.save(entity);
                    return ResponseEntity.ok(toDtoMapper.apply(savedEntity));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private <T extends User, D> void updateUserFields(D dto, T user) {
        if (dto instanceof UpdateAdminInfoDTO adminDTO) {
            updateUserInfo(user, adminDTO.getEmail(), adminDTO.getFirstName(), adminDTO.getLastName());
        } else if (dto instanceof UpdateStudentInfoDTO studentDTO) {
            updateUserInfo(user, studentDTO.getEmail(), studentDTO.getFirstName(), studentDTO.getLastName());
        }
    }

    private <T extends User> void updateUserInfo(T user, String email, String firstName, String lastName) {
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName);
        }
    }

    private <D> String getId(D dto) {
        if (dto instanceof UpdateAdminInfoDTO adminDTO) {
            return adminDTO.getId();
        } else if (dto instanceof UpdateStudentInfoDTO studentDTO) {
            return studentDTO.getId();
        }
        throw new IllegalArgumentException("Unsupported DTO type");
    }


    @Transactional
    @Override
    public boolean toggleUserAccount(String id) throws ChangeSetPersister.NotFoundException {
        return userRepository.findByIdIgnoreCase(id)
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
        if (id == null || id.isBlank()){
            throw new RuntimeException("Program ID mustn't be null");
        }
        Optional<Student> optionalStudent = studentRepository.findByIdIgnoreCase(id);
        if (optionalStudent.isPresent()) {
            Student student = optionalStudent.get();
            return ResponseEntity.ok(userMapper.convertStudentToDto(student));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<InfosStudentDTO> getStudentByEmail(String email) {
        Optional<Student> optionalStudent = studentRepository.findByEmailIgnoreCase(email);
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
        Optional<User> optionalTargetUser = userRepository.findByIdIgnoreCase(targetUserId);
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
        boolean userIsExistByEmail = userRepository.existsByEmailIgnoreCase(pendingStudentDTO.getEmail());
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
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findByEmailIgnoreCase(email);
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
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findByEmailIgnoreCase(email);
        if (optionalPendingStudent.isPresent()) {
            if (!studentRepository.existsByEmailIgnoreCase(optionalPendingStudent.get().getEmail())){
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
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findByEmailIgnoreCase(email);
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
        Optional<Admin> optionalAdmin = iUserDataProvider.getCurrentAdmin();
        Optional<PendingStudent> optionalPendingStudent = pendingStudentRepository.findByEmailIgnoreCase(email);
        if (optionalPendingStudent.isPresent() && optionalAdmin.isPresent()) {
            PendingStudent pendingStudent = optionalPendingStudent.get();
            Admin admin = optionalAdmin.get();

            BanedRegistration banedRegistration = userMapper.convertPendingStudentToBanedRegistration(pendingStudent);
            banedRegistration.setBanDate(new Date());
            banedRegistration.setAdminBanner(admin);

            banedRegistrationRepository.save(banedRegistration);

            deletePhoto(pendingStudent.getPhoto());

            pendingStudentRepository.delete(pendingStudent);

            return ResponseEntity.ok().body("The registration was banned successfully.");
        }
        return ResponseEntity.badRequest().body("Operation not possible! try again.");
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
            user.setPhotoId(photoId);
            userRepository.save(user);
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
        List<User> users = userRepository.findAllByIdInIgnoreCase(ids);
        if (users.isEmpty()) {
            return ResponseEntity.badRequest().body("Emails provided are not valid.");
        }

        // Parallel processing of photo deletion for Student users
        deletePhotos(ids);

        users.parallelStream().forEach(user -> clientService.deleteKCUser(user.getId()));

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
        users.forEach(user -> clientService.changeUserPassword(user.getId()));
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
        Optional<User> optionalUser = userRepository.findByIdIgnoreCase(id);
        return optionalUser.map(student -> fileServiceClient.getUserPhoto(student.getPhotoId())).orElse(null);
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

    @Override
    public void countStudentsByProgramId(){
        for (ProgramID programID : ProgramID.values()) {
            Long counter = studentRepository.countByProgramId(programID);
            Student.programIDCounter.put(programID, counter);
        }
    }

    @Override
    public void countAdmins(){
        List<String> adminIds = adminRepository.findAllAdminMle();
        Admin.serialCounter = adminIds.stream()
                .map(id -> Integer.parseInt(id.substring(4))) // Extract and convert the numeric part
                .max(Integer::compareTo)
                .orElse(100000);
    }

}
