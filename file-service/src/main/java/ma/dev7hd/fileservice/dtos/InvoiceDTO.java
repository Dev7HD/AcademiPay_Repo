package ma.dev7hd.fileservice.dtos;

import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;

@Getter
@Setter
public class InvoiceDTO {
    ByteArrayInputStream stream;
    String number;
}
