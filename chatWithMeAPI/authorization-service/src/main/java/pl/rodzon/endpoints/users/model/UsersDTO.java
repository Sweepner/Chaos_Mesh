package pl.rodzon.endpoints.users.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UsersDTO {
    private List<UserDTO> users;
}
