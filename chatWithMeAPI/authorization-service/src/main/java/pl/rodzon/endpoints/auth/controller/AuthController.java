package pl.rodzon.endpoints.auth.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rodzon.endpoints.auth.model.LoginRequest;
import pl.rodzon.endpoints.auth.model.RegistrationRequest;
import pl.rodzon.endpoints.auth.service.AuthService;
import pl.rodzon.endpoints.users.model.UserDTO;
import pl.rodzon.response.GoodResponse;


@RestController
@Tag(name = "Registration and login")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/login")
    public ResponseEntity<UserDTO> login(@ParameterObject LoginRequest loginRequest) {
        return new ResponseEntity<>(this.authService.login(loginRequest.getUsername(), loginRequest.getPassword()), HttpStatus.OK);
    }

    @PostMapping("/registration")
    public ResponseEntity<UserDTO> registration(@ParameterObject RegistrationRequest registrationRequest) {
        return new ResponseEntity<>(this.authService.registration(registrationRequest), HttpStatus.CREATED);
    }
}
