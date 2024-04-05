package pl.rodzon.endpoints.chat.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UsersWithMessagesDTO {
    private List<String> usersIDs;
}
