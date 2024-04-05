package pl.rodzon.endpoints.chat.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageDTO {
    private UUID id;
    private String text;
    private byte[] image;
    private LocalDateTime messageTime;
    private String roomID;
    private String username;
}
