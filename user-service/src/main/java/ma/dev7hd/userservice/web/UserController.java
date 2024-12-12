package ma.dev7hd.userservice.web;

import lombok.AllArgsConstructor;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosAdminDTO;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.NewPendingStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.UpdateAdminInfoDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.UpdateStudentInfoDTO;
import ma.dev7hd.userservice.dtos.otherDTOs.ChangePWDTO;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.entities.User;
import ma.dev7hd.userservice.entities.registrations.PendingStudent;
import ma.dev7hd.userservice.enums.DepartmentName;
import ma.dev7hd.userservice.enums.ProgramID;
import ma.dev7hd.userservice.services.UserService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/users/students")
    List<InfosStudentDTO> getStudents(){
        return userService.getAllStudents();
    }

    @GetMapping("/users/admins")
    List<InfosAdminDTO> getAdmins(){
        return userService.getAdmins();
    }

    @GetMapping("/users/admins/by-criteria")
    Page<InfosAdminDTO> getAdminsByCriteria(
            @RequestParam(defaultValue = "") String id,
            @RequestParam(defaultValue = "") String email,
            @RequestParam(defaultValue = "") String firstName,
            @RequestParam(defaultValue = "") String lastName,
            @RequestParam(defaultValue = "") DepartmentName departmentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return userService.getAdminsByCriteria(id,email,firstName,lastName,departmentName,page,size);
    }

    @GetMapping("/users/students/by-criteria")
    Page<InfosStudentDTO> getStudentsByCriteria(
            @RequestParam(defaultValue = "") String id,
            @RequestParam(defaultValue = "") String email,
            @RequestParam(defaultValue = "") String firstName,
            @RequestParam(defaultValue = "") String lastName,
            @RequestParam(defaultValue = "") ProgramID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getStudentsByCriteriaAsAdmin(id, email, firstName, lastName, programId, page, size);
    }

        @GetMapping("/users/id/{id}")
    ResponseEntity<InfosStudentDTO> getUserById(@PathVariable String id){
        return userService.getStudentById(id);
    }

    @GetMapping("/users/email/{email}")
    ResponseEntity<InfosStudentDTO> getUserByEmail(@PathVariable String email){
        return userService.getStudentByEmail(email);
    }

    @PostMapping(value = "/users/admins/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    User addAdmin(Admin admin, @RequestPart(value = "photo") MultipartFile photo) throws IOException{
        return userService.newAdmin(admin, photo);
    }

    @PostMapping(value = "/users/students/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    User addStudent(Student student, @RequestPart(value = "photo") MultipartFile photo) throws IOException{
        return userService.newStudent(student, photo);
    }

    @DeleteMapping("users/{id}/delete")
    ResponseEntity<String> deleteUserById(@PathVariable String id) {
        return userService.deleteUserById(id);
    }

    @PatchMapping("users/update")
    UpdateAdminInfoDTO updateAdmin(UpdateAdminInfoDTO adminDTO){
        return userService.updateAdmin(adminDTO);
    }

    @PatchMapping("/users/toggleAccount")
    boolean toggleUserAccount(String email) throws ChangeSetPersister.NotFoundException {
        return userService.toggleUserAccount(email);
    }

    @PostMapping(value = "/users/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> registerStudent(NewPendingStudentDTO pendingStudentDTO,@RequestBody MultipartFile photo) throws IOException {
        return userService.registerStudent(pendingStudentDTO, photo);
    }

    @PostMapping("/pending-registrations/approve")
    ResponseEntity<?> approvingStudentRegistration(String email) throws IOException {
        return userService.approvingStudentRegistration(email);
    }

    @PostMapping("/pending-registrations/decline")
    ResponseEntity<String> declineStudentRegistration(String email) throws IOException {
        return userService.declineStudentRegistration(email);
    }

    @PostMapping("/pending-registrations/ban")
    ResponseEntity<String> banStudentRegistration(String email) throws IOException {
        return userService.banStudentRegistration(email);
    }

    @GetMapping("/pending-registrations/{email}")
    ResponseEntity<PendingStudent> getPendingStudentByEmail(@PathVariable String email) {
        return userService.getPendingStudentByEmail(email);
    }

    @GetMapping("/pending-registrations")
    Page<PendingStudent> getPendingStudent(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getPendingStudent(email, page, size);
    }

    @PatchMapping("/users/change-pw")
    ResponseEntity<String> changePW(ChangePWDTO pwDTO){
        return userService.changePW(pwDTO);
    }

    @PatchMapping("/users/{id}/reset-pw")
    ResponseEntity<String> resetPW(@PathVariable(name = "id") String targetUserId){
        return userService.resetPW(targetUserId);
    }

    @PatchMapping("/users/students/update")
    ResponseEntity<InfosStudentDTO> updateStudentInfo(@RequestBody UpdateStudentInfoDTO infosStudentDTO) {
        return userService.updateStudentInfo(infosStudentDTO);
    }

    @PutMapping(value = "/users/photos/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> updateStudentPhoto(@RequestPart(value = "photo")MultipartFile photo) throws IOException {
        return userService.updateStudentPhoto(photo);
    }

    @PostMapping("/pending-registrations/approve-multiple")
    ResponseEntity<String> approveMultipleRegistrations(@RequestBody List<String> emails) {
        return userService.approveMultipleRegistrations(emails);
    }

    @PostMapping("/pending-registrations/ban-multiple")
    ResponseEntity<String> banMultipleRegistrations(@RequestBody List<String> emails) {
        return userService.banMultipleRegistrations(emails);
    }

    @PostMapping("/pending-registrations/decline-multiple")
    void declineMultipleRegistrations(@RequestBody List<String> emails) {
        userService.declineMultipleRegistrations(emails);
    }

    @DeleteMapping("users/multiple-delete")
    ResponseEntity<String> deleteMultipleUsers(@RequestBody List<String> ids) {
        return userService.deleteMultipleUsers(ids);
    }

    @PostMapping("/users/multiple-reset-pw")
    ResponseEntity<String> resetPasswordToMultipleUsers(@RequestBody List<String> ids) {
        return userService.resetPasswordToMultipleUsers(ids);
    }

    @PostMapping("/users/toggle-accounts/multiple")
    void toggleUsersAccounts(@RequestBody List<String> ids){
        userService.toggleUsersAccounts(ids);
    }

    @GetMapping(value = "/users/photo", produces = MediaType.IMAGE_JPEG_VALUE)
    ResponseEntity<byte[]> getProfilePicture(String id){
        return userService.getProfilePicture(id);
    }



}
