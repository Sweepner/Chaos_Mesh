package pl.rodzon.endpoints.chat.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class CreateImageMessageRequest {
    private MultipartFile image;
    private String roomID;
    private String publicKey;
    private String username;
    private String sender;
    private String time;
}
