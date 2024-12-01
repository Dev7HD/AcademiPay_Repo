package ma.dev7hd.userservice.services;

import ma.dev7hd.userservice.dtos.infoDTOs.InfosAdminDTO;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.*;
import ma.dev7hd.userservice.entities.User;
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
    User newUser(User newUser);

    @Transactional
    ResponseEntity<String> deleteUserById(String id);

    @Transactional
    UpdateAdminInfoDTO updateAdmin(UpdateAdminInfoDTO adminDTO);

    @Transactional
    boolean toggleUserAccount(String id) throws ChangeSetPersister.NotFoundException;

    List<InfosStudentDTO> getAllStudents();

    ResponseEntity<InfosStudentDTO> getStudentByCode(String id);

    ResponseEntity<InfosStudentDTO> getStudentByEmail(String email);

    List<InfosStudentDTO> getStudentByProgram(ProgramID programID);

    @Transactional
    ResponseEntity<NewAdminDTO> saveAdmin(NewAdminDTO newAdminDTO);

    @Transactional
    ResponseEntity<InfosStudentDTO> saveStudent(NewStudentDTO studentDTO, MultipartFile photo) throws IOException;

    List<InfosAdminDTO> getAdmins();

    Page<InfosAdminDTO> getAdminsByCriteria(String id, String email, String firstName, String lastName, DepartmentName departmentName, int page, int size);

    Page<InfosStudentDTO> getStudentsByCriteriaAsAdmin(String id, String email, String firstName, String lastName, ProgramID programID, int page, int size);

    @Transactional
    ResponseEntity<String> registerStudent(NewPendingStudentDTO pendingStudentDTO, MultipartFile photo) throws IOException;

    Page<PendingStudent> getPendingStudent(String email, int page, int size);

    ResponseEntity<PendingStudent> getPendingStudentByEmail(String email);

    @Transactional
    ResponseEntity<String> declineStudentRegistration(String email) throws IOException;

    @Transactional
    ResponseEntity<String> banStudentRegistration(String email) throws IOException;

    @Transactional
    ResponseEntity<InfosStudentDTO> updateStudentInfo(UpdateStudentInfoDTO studentDTO);

    @Transactional
    void declineMultipleRegistrations(List<String> emails);

    void toggleUsersAccounts(List<String> ids);
}
