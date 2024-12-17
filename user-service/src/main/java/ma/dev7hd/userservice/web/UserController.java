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
import ma.dev7hd.userservice.services.loadDataFromExcel.ILoadStudentsService;
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
    private final ILoadStudentsService loadStudentsService;

    @GetMapping(value = "/users/students", produces = MediaType.APPLICATION_JSON_VALUE)
    List<InfosStudentDTO> getStudents(){
        return userService.getAllStudents();
    }

    @GetMapping(value = "/users/admins", produces = MediaType.APPLICATION_JSON_VALUE)
    List<InfosAdminDTO> getAdmins(){
        return userService.getAdmins();
    }

    @GetMapping(value = "/users/admins/by-criteria", produces = MediaType.APPLICATION_JSON_VALUE)
    Page<InfosAdminDTO> getAdminsByCriteria(
            @RequestParam(defaultValue = "", required = false) String id,
            @RequestParam(defaultValue = "", required = false) String email,
            @RequestParam(defaultValue = "", required = false) String firstName,
            @RequestParam(defaultValue = "", required = false) String lastName,
            @RequestParam(defaultValue = "", required = false) DepartmentName departmentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return userService.getAdminsByCriteria(id,email,firstName,lastName,departmentName,page,size);
    }

    @GetMapping(value = "/users/students/by-criteria", produces = MediaType.APPLICATION_JSON_VALUE)
    Page<InfosStudentDTO> getStudentsByCriteria(
            @RequestParam(defaultValue = "", required = false) String id,
            @RequestParam(defaultValue = "", required = false) String email,
            @RequestParam(defaultValue = "", required = false) String firstName,
            @RequestParam(defaultValue = "", required = false) String lastName,
            @RequestParam(defaultValue = "", required = false) ProgramID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getStudentsByCriteriaAsAdmin(id, email, firstName, lastName, programId, page, size);
    }

        @GetMapping(value = "/users/id/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<InfosStudentDTO> getUserById(@PathVariable String id){
        return userService.getStudentById(id);
    }

    @GetMapping(value = "/users/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<InfosStudentDTO> getUserByEmail(@PathVariable String email){
        return userService.getStudentByEmail(email);
    }

    @PostMapping(value = "/users/admins/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    User addAdmin(Admin admin, @RequestPart(value = "photo") MultipartFile photo) throws IOException{
        return userService.newAdmin(admin, photo);
    }

    @PostMapping(value = "/users/students/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    User addStudent(Student student, @RequestPart(value = "photo") MultipartFile photo) throws IOException{
        return userService.newStudent(student, photo);
    }

    @DeleteMapping(value = "users/{id}/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> deleteUserById(@PathVariable String id) {
        return userService.deleteUserById(id);
    }

    @PatchMapping(value = "users/admins/update", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UpdateAdminInfoDTO> updateAdmin(UpdateAdminInfoDTO adminDTO){
        return userService.updateAdminInfo(adminDTO);
    }

    @PatchMapping(value = "/users/toggleAccount")
    ResponseEntity<Boolean> toggleUserAccount(String id) throws ChangeSetPersister.NotFoundException {
        boolean isEnabled = userService.toggleUserAccount(id);
        return ResponseEntity.ok(isEnabled);
    }

    @PostMapping(value = "/users/students/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> registerStudent(NewPendingStudentDTO pendingStudentDTO,@RequestBody MultipartFile photo) throws IOException {
        return userService.registerStudent(pendingStudentDTO, photo);
    }

    @PostMapping(value = "/pending-registrations/approve", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    ResponseEntity<?> approvingStudentRegistration(String email) throws IOException {
        return userService.approvingStudentRegistration(email);
    }

    @PostMapping(value = "/pending-registrations/decline", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> declineStudentRegistration(String email) throws IOException {
        return userService.declineStudentRegistration(email);
    }

    @PostMapping(value = "/pending-registrations/ban", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> banStudentRegistration(String email) throws IOException {
        return userService.banStudentRegistration(email);
    }

    @GetMapping(value = "/pending-registrations/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PendingStudent> getPendingStudentByEmail(@PathVariable String email) {
        return userService.getPendingStudentByEmail(email);
    }

    @GetMapping(value = "/pending-registrations", produces = MediaType.APPLICATION_JSON_VALUE)
    Page<PendingStudent> getPendingStudent(
            @RequestParam(defaultValue = "", required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getPendingStudent(email, page, size);
    }

    @PatchMapping(value = "/users/change-pw", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> changePW(ChangePWDTO pwDTO){
        return userService.changePW(pwDTO);
    }

    @PatchMapping(value = "/users/{id}/reset-pw", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> resetPW(@PathVariable(name = "id") String targetUserId){
        return userService.resetPW(targetUserId);
    }

    @PatchMapping(value = "/users/students/update", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<InfosStudentDTO> updateStudentInfo(@RequestBody UpdateStudentInfoDTO infosStudentDTO) {
        return userService.updateStudentInfo(infosStudentDTO);
    }

    @PutMapping(value = "/users/photos/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> updateStudentPhoto(@RequestPart(value = "photo")MultipartFile photo) throws IOException {
        return userService.updateStudentPhoto(photo);
    }

    @PostMapping(value = "/pending-registrations/approve-multiple", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> approveMultipleRegistrations(@RequestBody List<String> emails) {
        return userService.approveMultipleRegistrations(emails);
    }

    @PostMapping(value = "/pending-registrations/ban-multiple", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> banMultipleRegistrations(@RequestBody List<String> emails) {
        return userService.banMultipleRegistrations(emails);
    }

    @PostMapping(value = "/pending-registrations/decline-multiple")
    void declineMultipleRegistrations(@RequestBody List<String> emails) {
        userService.declineMultipleRegistrations(emails);
    }

    @DeleteMapping(value = "users/multiple-delete", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> deleteMultipleUsers(@RequestBody List<String> ids) {
        return userService.deleteMultipleUsers(ids);
    }

    @PostMapping(value = "/users/multiple-reset-pw", produces = MediaType.TEXT_PLAIN_VALUE)
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

    @PostMapping(value = "/users/students/add-from-excel", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> addStudentsFromExcelFile(@RequestPart(value = "excelFile") MultipartFile excelFile) throws Exception {
        return loadStudentsService.uploadStudentFile(excelFile);
    }

}
