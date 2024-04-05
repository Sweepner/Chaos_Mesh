package pl.rodzon.endpoints.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "message")
public class MessageDAO {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "text")
    private String text;
    @Column(name = "image")
    private byte[] image;
    @Column(name = "message_time")
    private LocalDateTime messageTime;
    @Column(name = "room_id")
    private String roomID;
    @Column(name = "public_key", length = 2000)
    private String publicKey;
}
