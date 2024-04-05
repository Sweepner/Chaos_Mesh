package pl.rodzon.endpoints.auth.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrationRequest {
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String publicKey;
}
