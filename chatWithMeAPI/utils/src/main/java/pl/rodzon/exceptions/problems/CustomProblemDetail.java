package pl.rodzon.exceptions.problems;

import lombok.Getter;
import org.springframework.http.ProblemDetail;
import pl.rodzon.exceptions.model.ChatProblemMessage;
import pl.rodzon.exceptions.model.ChatViolation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Getter
public class CustomProblemDetail extends ProblemDetail {
    private final String exception;
    private final String timestamp;
    private final UUID userID;
    private final List<ChatViolation> violations;

    public CustomProblemDetail(ChatProblemMessage message, UUID userID, List<ChatViolation> violations) {
        super(message.getStatus().value());
        setTitle(message.getStatus().getReasonPhrase());
        setDetail(message.getDetail());

        this.exception = message.getException();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.userID = userID;
        this.violations = violations;
    }
}
