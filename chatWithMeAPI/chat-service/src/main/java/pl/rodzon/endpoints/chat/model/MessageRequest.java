package pl.rodzon.endpoints.chat.model;

import lombok.Data;

import jakarta.validation.constraints.NotNull;


@Data
public class MessageRequest {
    @NotNull
    private String roomID;
    private String text;
    private String time;
    @NotNull
    private String publicKey;
    private String username;
}
