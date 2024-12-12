package ma.dev7hd.fileservice.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Student {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private ProgramID programId;
    private String photo;
    private List<Payment> payments;

    public enum ProgramID {
        SMP,
        SMC,
        SMA,
        SMI,
        SVT
    }
}
