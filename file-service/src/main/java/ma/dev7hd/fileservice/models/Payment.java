package ma.dev7hd.fileservice.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.UUID;

@Getter @Setter @ToString
public class Payment {

    private UUID id;

    private Date date;

    private Date registerDate;

    private double amount;

    private PaymentType type;

    private PaymentStatus status;

    private Student student;

    public enum PaymentStatus {
        CREATED,
        VALIDATED,
        REJECTED
    }

    public enum PaymentType {
        CASH,
        CHECK,
        TRANSFER,
        DEPOSIT
    }

}


