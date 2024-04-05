package pl.rodzon.endpoints.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.rodzon.endpoints.chat.model.MessageDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends
        JpaRepository<MessageDAO, UUID>,
        JpaSpecificationExecutor<MessageDAO> {

    void deleteByMessageTime(LocalDateTime time);
    void deleteAllByMessageTime(LocalDateTime time);

    @Query
    Optional<List<MessageDAO>> findAllByRoomIDAndPublicKey(String roomID, String publicKey);

    @Query(value = "SELECT DISTINCT m.roomID FROM MessageDAO m WHERE m.roomID LIKE %?1%")
    List<String> findAllRoomID(UUID roomID);
    Optional<MessageDAO> findFirstByRoomIDAndPublicKeyAndImageNotNullOrderByMessageTimeDesc(String roomID, String publicKey);
}
