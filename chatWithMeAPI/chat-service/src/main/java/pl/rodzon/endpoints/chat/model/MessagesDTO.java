package pl.rodzon.endpoints.chat.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessagesDTO {
    private List<MessageDTO> messages;
}
