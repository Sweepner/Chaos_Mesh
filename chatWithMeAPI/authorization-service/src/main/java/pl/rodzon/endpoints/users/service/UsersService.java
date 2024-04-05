package pl.rodzon.endpoints.users.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.rodzon.endpoints.auth.model.RegistrationRequest;
import pl.rodzon.endpoints.users.model.DeleteUsersRequest;
import pl.rodzon.endpoints.users.model.UserDAO;
import pl.rodzon.endpoints.users.model.UserDTO;
import pl.rodzon.endpoints.users.model.UsersDTO;
import pl.rodzon.endpoints.users.repository.UsersFilter;
import pl.rodzon.endpoints.users.repository.UsersRepository;
import pl.rodzon.endpoints.users.repository.UsersSpec;
import pl.rodzon.exceptions.model.ChatProblemMessage;
import pl.rodzon.exceptions.problems.ChatProblem;
import pl.rodzon.response.GoodResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@AllArgsConstructor
@Slf4j
public class UsersService {
    private final UsersRepository usersRepository;
    private final ModelMapper mapper;


    public UserDTO insertUser(RegistrationRequest registrationRequest) {
        this.checkIfUsernameExists(registrationRequest.getUsername());
        UserDAO createdUser = this.createUserFromRequest(registrationRequest);
        this.usersRepository.save(createdUser);
        return this.mapper.map(createdUser, UserDTO.class);
    }

    public Object getUsers(Pageable pageable, Boolean isPageableEnabled, UsersFilter filter) {
        if (isPageableEnabled) {
            return this.getUsersPageable(pageable, filter);
        } else {
            return this.getAllUsers(filter);
        }
    }

    public Page<UserDTO> getUsersPageable(Pageable pageable, UsersFilter filter) {
        UsersSpec spec = new UsersSpec(filter);
        return this.usersRepository.findAll(spec.getSpecification(), pageable).map(x -> mapper.map(x, UserDTO.class));
    }

    public UsersDTO getAllUsers(UsersFilter filter) {
        log.info("Getting all users not pageable with filters: {}", filter.toString());
        UsersSpec spec = new UsersSpec(filter);
        return UsersDTO.builder()
                .users(this.usersRepository.findAll(spec.getSpecification()).stream().map(x -> mapper.map(x, UserDTO.class)).toList())
                .build();
    }

    public GoodResponse deleteUserByUserId(String userId) {
        log.info("Deleting user by userId: {}", userId);
        this.usersRepository.deleteById(UUID.fromString(userId));
        return new GoodResponse("good.user.delete");
    }

    public GoodResponse deleteUsersByIds(DeleteUsersRequest request) {
        log.info("Deleting users by ids: {}", request.getUsersIDs());
        List<UserDAO> users = this.usersRepository.findAllById(request.getUsersIDs());
        this.usersRepository.deleteAll(users);
        return new GoodResponse("good.user.delete");
    }

    public UUID findUserIdByUsernameAndPassword(String username, String password) {
        Optional<UUID> userId = this.usersRepository.findUserIdByUsernameAndPassword(username, password);
        if (userId.isPresent()) {
            return userId.get();
        } else {
            throw new ChatProblem(new ChatProblemMessage("ex.auth.401.wrong.credential"));
        }
    }

    public UserDAO findUserDAOByUsernameAndPassword(String username, String password) {
        Optional<UserDAO> userDAO = this.usersRepository.findByUsernameAndPassword(username, password);
        if (userDAO.isPresent()) {
            return userDAO.get();
        } else {
            throw new ChatProblem(new ChatProblemMessage("ex.auth.401.wrong.credential"));
        }
    }

    public UserDTO getUserByUsername(String username) {
        return this.mapper.map(this.usersRepository.findByUsername(username).orElseThrow(() -> new ChatProblem(new ChatProblemMessage("ex.user.404.not.found", username))), UserDTO.class);
    }

    private UserDAO createUserFromRequest(RegistrationRequest registrationRequest) {
        return UserDAO.builder()
                .userID(UUID.randomUUID())
                .username(registrationRequest.getUsername())
                .password(registrationRequest.getPassword())
                .publicKey(registrationRequest.getPublicKey())
                .build();
    }

    private void checkIfUsernameExists(String username) {
        if (this.usersRepository.checkIfUsernameExists(username).isPresent()) {
            throw new ChatProblem(new ChatProblemMessage("ex.user.409.username.already.exists"));
        }
    }
}
