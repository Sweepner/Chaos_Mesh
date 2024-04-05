package pl.rodzon.endpoints.users.repository;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class UsersFilter {
    @Parameter(description = "Filter: Searching by username.")
    private String username;
}
