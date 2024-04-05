package pl.rodzon.endpoints.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.rodzon.endpoints.users.model.DeleteUsersRequest;
import pl.rodzon.endpoints.users.model.UserDTO;
import pl.rodzon.endpoints.users.repository.UsersFilter;
import pl.rodzon.endpoints.users.service.UsersService;
import pl.rodzon.response.GoodResponse;

@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users controller")
@AllArgsConstructor
public class UsersController {
    private final UsersService usersService;


    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<Object> getUsers(@Parameter(hidden = true) Pageable pageable, Boolean isPageableEnabled, @ParameterObject UsersFilter filter) {
        return new ResponseEntity<>(this.usersService.getUsers(pageable, isPageableEnabled, filter), HttpStatus.OK);
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get user by username")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable(value = "username") String username) {
        log.info("Getting user by username: {}", username);
        return new ResponseEntity<>(this.usersService.getUserByUsername(username), HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user by userId")
    public ResponseEntity<GoodResponse> deleteUserByUserId(@PathVariable(value = "userId") String userId) {
        log.info("Deleting user by userId: {}", userId);
        return new ResponseEntity<>(this.usersService.deleteUserByUserId(userId), HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Operation(summary = "Delete users by ids")
    public ResponseEntity<GoodResponse> deleteUsersByIds(@ParameterObject DeleteUsersRequest request) {
        return new ResponseEntity<>(this.usersService.deleteUsersByIds(request), HttpStatus.NO_CONTENT);
    }
}
