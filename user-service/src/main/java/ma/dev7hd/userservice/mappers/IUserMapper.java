package ma.dev7hd.userservice.mappers;

import ma.dev7hd.userservice.dtos.infoDTOs.InfosAdminDTO;
import ma.dev7hd.userservice.dtos.infoDTOs.InfosStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.NewAdminDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.NewPendingStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.NewStudentDTO;
import ma.dev7hd.userservice.dtos.newObjectDTOs.UpdateAdminInfoDTO;
import ma.dev7hd.userservice.entities.Admin;
import ma.dev7hd.userservice.entities.Student;
import ma.dev7hd.userservice.entities.registrations.BanedRegistration;
import ma.dev7hd.userservice.entities.registrations.PendingStudent;
import org.springframework.data.domain.Page;

public interface IUserMapper {
    UpdateAdminInfoDTO convertUpdatedAdminToDto(Admin admin);

    Page<InfosAdminDTO> convertPageableAdminToDTO(Page<Admin> admins);

    BanedRegistration convertPendingStudentToBanedRegistration(PendingStudent pendingStudent);

    PendingStudent convertPendingStudentToDto(NewPendingStudentDTO pendingStudentDTO);

    Student convertPendingStudentToStudent(PendingStudent pendingStudent);

    Page<InfosStudentDTO> convertPageableStudentToDTO(Page<Student> students);

    InfosStudentDTO convertStudentToDto(Student student);

    InfosAdminDTO convertAdminToDto(Admin admin);

    Admin convertNewAdminDtoToAdmin(NewAdminDTO newAdminDTO);

    Student convertStudentDtoToStudent(NewStudentDTO studentDTO);
}
