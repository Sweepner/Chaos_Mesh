package pl.rodzon.endpoints.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.rodzon.endpoints.users.model.UserDAO;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsersRepository extends
        JpaRepository<UserDAO, UUID>,
        JpaSpecificationExecutor<UserDAO> {

    @Query
    Optional<UserDAO> findByUsername(String username);

    @Query
    Optional<UserDAO> findByUsernameAndPassword(String username, String password);

    @Query(value = "SELECT u.userID FROM UserDAO u WHERE u.username = ?1 AND u.password = ?2")
    Optional<UUID> findUserIdByUsernameAndPassword(String username, String password);

    @Query(value = "SELECT u.username FROM UserDAO u WHERE u.username = ?1")
    Optional<String> checkIfUsernameExists(String username);
}
