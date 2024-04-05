package pl.rodzon.endpoints.auth.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pl.rodzon.endpoints.auth.model.RegistrationRequest;
import pl.rodzon.endpoints.users.model.UserDAO;
import pl.rodzon.endpoints.users.model.UserDTO;
import pl.rodzon.endpoints.users.service.UsersService;
import pl.rodzon.response.GoodResponse;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class AuthService {
    private final UsersService usersService;
    private final ModelMapper modelMapper;

    public UserDTO login(String username, String password) {
        log.info("Trying to logging in... with credentials: " + username + " " + password);
        return this.modelMapper.map(this.usersService.findUserDAOByUsernameAndPassword(username, password), UserDTO.class);
    }

    public UserDTO registration(RegistrationRequest registrationRequest) {
        log.info("Trying to register {}", registrationRequest.getUsername());
        return this.usersService.insertUser(registrationRequest);
    }
}
