package ma.dev7hd.userservice.mappers;

import lombok.AllArgsConstructor;
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
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserMapper implements IUserMapper{
    private final ModelMapper modelMapper;

    @Override
    public UpdateAdminInfoDTO convertUpdatedAdminToDto(Admin admin){
        return modelMapper.map(admin, UpdateAdminInfoDTO.class);
    }

    @Override
    public Page<InfosAdminDTO> convertPageableAdminToDTO(Page<Admin> admins){
        return admins.map(admin -> modelMapper.map(admin, InfosAdminDTO.class));
    }

    @Override
    public BanedRegistration convertPendingStudentToBanedRegistration(PendingStudent pendingStudent){
        return modelMapper.map(pendingStudent, BanedRegistration.class);
    }

    @Override
    public PendingStudent convertPendingStudentToDto(NewPendingStudentDTO pendingStudentDTO) {
        return modelMapper.map(pendingStudentDTO, PendingStudent.class);
    }

    @Override
    public Student convertPendingStudentToStudent(PendingStudent pendingStudent) {
        return modelMapper.map(pendingStudent, Student.class);
    }

    @Override
    public Page<InfosStudentDTO> convertPageableStudentToDTO(Page<Student> students){
        return students.map(student -> modelMapper.map(student, InfosStudentDTO.class));
    }

    @Override
    public InfosStudentDTO convertStudentToDto(Student student) {
        return modelMapper.map(student, InfosStudentDTO.class);
    }

    @Override
    public InfosAdminDTO convertAdminToDto(Admin admin) {
        return modelMapper.map(admin, InfosAdminDTO.class);
    }

    @Override
    public Admin convertNewAdminDtoToAdmin(NewAdminDTO newAdminDTO){
        return modelMapper.map(newAdminDTO, Admin.class);
    }

    @Override
    public Student convertStudentDtoToStudent(NewStudentDTO studentDTO){
        return modelMapper.map(studentDTO, Student.class);
    }
}
