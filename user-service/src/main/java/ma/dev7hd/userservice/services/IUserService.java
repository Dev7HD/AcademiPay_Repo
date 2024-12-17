package ma.dev7hd.userservice.services;

import ma.dev7hd.userservice.dtos.infoDTOs.InfosAdminDTO;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.*;
import ma.dev7hd.userservice.dtos.otherDTOs.ChangePWDTO;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.entities.registrations.PendingStudent;
import ma.dev7hd.userservice.enums.DepartmentName;
import ma.dev7hd.userservice.enums.ProgramID;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IUserService {
    @Transactional
    Admin newAdmin(Admin admin, MultipartFile photo) throws IOException;

    @Transactional
    Student newStudent(Student student, MultipartFile photo) throws IOException;

    @Transactional
    ResponseEntity<String> deleteUserById(String id);

    @Transactional
    ResponseEntity<UpdateAdminInfoDTO> updateAdminInfo(UpdateAdminInfoDTO adminDTO);

    @Transactional
    boolean toggleUserAccount(String id) throws ChangeSetPersister.NotFoundException;

    List<InfosStudentDTO> getAllStudents();

    ResponseEntity<InfosStudentDTO> getStudentById(String id);

    ResponseEntity<InfosStudentDTO> getStudentByEmail(String email);

    @Transactional
    ResponseEntity<String> changePW(ChangePWDTO pwDTO);

    @Transactional
    ResponseEntity<String> resetPW(String targetUserId);

    List<InfosAdminDTO> getAdmins();

    Page<InfosAdminDTO> getAdminsByCriteria(String id, String email, String firstName, String lastName, DepartmentName departmentName, int page, int size);

    Page<InfosStudentDTO> getStudentsByCriteriaAsAdmin(String id, String email, String firstName, String lastName, ProgramID programID, int page, int size);

    @Transactional
    ResponseEntity<String> registerStudent(NewPendingStudentDTO pendingStudentDTO, MultipartFile photo) throws IOException;

    Page<PendingStudent> getPendingStudent(String email, int page, int size);

    ResponseEntity<PendingStudent> getPendingStudentByEmail(String email);

    @Transactional
    ResponseEntity<?> approvingStudentRegistration(String email) throws IOException;

    @Transactional
    ResponseEntity<String> declineStudentRegistration(String email) throws IOException;

    @Transactional
    ResponseEntity<String> banStudentRegistration(String email) throws IOException;

    @Transactional
    ResponseEntity<InfosStudentDTO> updateStudentInfo(UpdateStudentInfoDTO studentDTO);

    ResponseEntity<String> updateStudentPhoto(MultipartFile photo) throws IOException;

    @Transactional
    ResponseEntity<String> approveMultipleRegistrations(List<String> emails);

    ResponseEntity<String> banMultipleRegistrations(List<String> emails);

    @Transactional
    void declineMultipleRegistrations(List<String> emails);

    @Transactional
    ResponseEntity<String> deleteMultipleUsers(List<String> ids);

    ResponseEntity<String> resetPasswordToMultipleUsers(List<String> ids);

    void toggleUsersAccounts(List<String> ids);

    ResponseEntity<?> getProfilePicture(String id) throws IOException;

    void countStudentsByProgramId();

    void countAdmins();
}
