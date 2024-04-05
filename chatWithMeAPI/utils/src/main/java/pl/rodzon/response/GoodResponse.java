package pl.rodzon.response;

import lombok.Data;
import pl.rodzon.exceptions.service.ChatMessage;

@Data
public class GoodResponse {
    private String message;

    public GoodResponse(String message, Object... detailParams) {
        String mess = ChatMessage.get().get(message);
        this.message = String.format(mess, detailParams);
    }
}
