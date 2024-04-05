package pl.rodzon.exceptions.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import pl.rodzon.exceptions.service.ChatMessage;

@Getter
public class ChatProblemMessage {
    private final HttpStatus status;
    private final String exception;
    private final String detail;

    public ChatProblemMessage(String messageKey, Object... detailParams) {
        this.status = this.prepareStatus(this.prepareStatusKey(messageKey));
        this.exception = ChatMessage.get().get(this.prepareExceptionKey(messageKey));
        String detail = ChatMessage.get().get(messageKey);
        this.detail = String.format(detail, detailParams);
    }

    public ChatProblemMessage(Integer status, String exception, String detail, Object... detailsParam) {
        this.status = this.prepareStatus(status);
        this.exception = ChatMessage.get().get(exception);
        this.detail = String.format(detail, detailsParam);
    }

    private HttpStatus prepareStatus(Integer status) {
        try {
            return HttpStatus.valueOf(status);
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private Integer prepareStatusKey(String messageKey) {
        try {
            int index = this.getIndexOfSecondDot(messageKey);
            String status = messageKey.substring(index + 1, index + 4);
            return Integer.valueOf(status);
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }

    private String prepareExceptionKey(String messageKey) {
        try {
            int index = this.getIndexOfSecondDot(messageKey);
            return messageKey.substring(0, index);
        } catch (Exception e) {
            return "ex.unknown";
        }
    }

    private int getIndexOfSecondDot(String messageKey) {
        return messageKey.indexOf(".", messageKey.indexOf(".") + 1);
    }
}
