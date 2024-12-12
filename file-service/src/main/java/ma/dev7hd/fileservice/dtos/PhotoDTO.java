package ma.dev7hd.fileservice.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter @Builder
public class PhotoDTO {
    private byte[] photoBytes;
    private String fileName;
}
