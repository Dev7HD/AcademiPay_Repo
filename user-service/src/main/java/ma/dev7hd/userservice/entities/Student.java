package ma.dev7hd.userservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.dev7hd.userservice.enums.ProgramID;

import java.util.*;

@Entity
@DiscriminatorValue("STUDENT")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Student extends User {

    @Enumerated(EnumType.STRING)
    private ProgramID programId;

    public static Map<ProgramID, Long> programIDCounter = new EnumMap<>(ProgramID.class);

    public static void updateProgramCountsFromDB(ProgramID programID, Long differenceValue) {

        Long counter = programIDCounter.getOrDefault(programID, 0L );

        counter += differenceValue;

        programIDCounter.put(programID, counter);

    }
}
