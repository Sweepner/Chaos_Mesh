package pl.rodzon.endpoints.chat.repository;

import org.springframework.data.jpa.domain.Specification;
import pl.rodzon.endpoints.chat.model.MessageDAO;

public class ChatSpec {
    private final Specification<MessageDAO> specification;

    public ChatSpec(String roomID, String publicKey) {
        Specification<MessageDAO> searchByUserID = (r, q, b) -> {
            if(roomID != null) {
                return b.like(r.get("roomID").as(String.class), roomID);
            }
            return null;
        };

        Specification<MessageDAO> searchPublicKey = (r, q, b) -> {
            if(publicKey != null) {
                return b.like(r.get("publicKey"), publicKey);
            }
            return null;
        };
        this.specification = Specification
                .where(searchByUserID)
                .and(searchPublicKey);
    }
    public Specification<MessageDAO> getSpecification() {
        return this.specification;
    }
}
