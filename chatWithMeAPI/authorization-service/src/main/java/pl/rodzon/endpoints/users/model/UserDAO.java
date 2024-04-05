package pl.rodzon.endpoints.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Data
@Entity
@Table(name = "user_table")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDAO {
    @Id
    @Column(name = "id")
    private UUID userID;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "public_key", length = 2000)
    private String publicKey;
    @Column(name = "picture")
    private byte[] picture;
}
