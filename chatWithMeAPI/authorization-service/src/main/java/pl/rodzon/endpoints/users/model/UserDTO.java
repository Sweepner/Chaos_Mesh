package pl.rodzon.endpoints.users.model;

import lombok.Data;
import org.springframework.util.ResourceUtils;
import pl.rodzon.exceptions.model.ChatProblemMessage;
import pl.rodzon.exceptions.problems.ChatProblem;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID userID;
    private String username;
    private String publicKey;
    private byte[] picture;


    public void setPicture(byte[] picture) {
        if (picture != null) {
            this.picture = picture;
        } else {
            this.picture = this.getDefaultPicture();
        }
    }

    private byte[] getDefaultPicture() {
        try {
            File path = ResourceUtils.getFile("classpath:static/default_profile_picture.png");
            FileInputStream fileInputStream = new FileInputStream(path);
            byte[] barr = new byte[(int) path.length()];
            fileInputStream.read(barr);
            fileInputStream.close();
            return barr;
        } catch (Exception e) {
            throw new ChatProblem(new ChatProblemMessage("ex.user.500.cannot.load.default.picture"), e);
        }
    }
}
