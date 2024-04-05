package pl.rodzon.endpoints.users.model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeleteUsersRequest {
    private List<UUID> usersIDs;
}
