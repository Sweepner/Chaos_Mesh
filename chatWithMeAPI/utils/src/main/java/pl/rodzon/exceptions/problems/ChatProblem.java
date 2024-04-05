package pl.rodzon.exceptions.problems;

import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import pl.rodzon.application_context.ChatContext;
import pl.rodzon.exceptions.model.ChatProblemMessage;
import pl.rodzon.exceptions.model.ChatViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatProblem extends RuntimeException {
    private final ProblemDetail problemDetail;

    public ChatProblem(ChatProblemMessage message) {
        this(message, ChatContext.getOptionalUserId(), null);
    }

    public ChatProblem(ChatProblemMessage message, Exception e) {
        this(message, ChatContext.getOptionalUserId(), extractViolations(e));
    }

    public ChatProblem(ChatProblemMessage message, List<ChatViolation> violations) {
        this(message, ChatContext.getOptionalUserId(), violations);
    }

    public ProblemDetail toProblemDetail() {
        return problemDetail;
    }

    private ChatProblem(ChatProblemMessage message, UUID userID, List<ChatViolation> violations) {
        this.problemDetail = new CustomProblemDetail(message, userID, violations);
    }

    private static List<ChatViolation> extractViolations(Exception e) {
        if (e instanceof ChatWrapper wrapper) {
            e = wrapper.getException();
        }

        List<ChatViolation> violations = new ArrayList<>();
        if (e instanceof BindException ex) {
            for (FieldError fieldError : ex.getFieldErrors()) {
                String rejectedValue = null;
                Object rejected = fieldError.getRejectedValue();
                if (rejected != null) {
                    rejectedValue = rejected.toString();
                }
                violations.add(new ChatViolation(fieldError.getField(), rejectedValue));
            }
        }
        if (violations.isEmpty()) {
            violations.add(new ChatViolation("Cause " + e.getClass().getSimpleName(), e.getMessage()));
        }
        return violations;
    }
}
