package pl.rodzon.exceptions.handler;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.rodzon.exceptions.model.ChatProblemMessage;
import pl.rodzon.exceptions.model.ChatViolation;
import pl.rodzon.exceptions.problems.ChatProblem;

import java.time.DateTimeException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        return this.handleConstraintViolation(ex);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handlePropertyReferenceException(PropertyReferenceException ex) {
        return (ProblemDetail) this.handleConstraintViolation(ex).getBody();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        return (ProblemDetail) this.handleConstraintViolation(ex).getBody();
    }

    @ExceptionHandler(DateTimeException.class)
    ProblemDetail handleDateTimeException(DateTimeException ex) {
        ChatProblemMessage chatProblemMessage = new ChatProblemMessage("ex.other.422.date");
        return new ChatProblem(chatProblemMessage, List.of(new ChatViolation(ex.getClass().getSimpleName(), ex.getMessage()))).toProblemDetail();
    }

    @ExceptionHandler(ChatProblem.class)
    ProblemDetail handleFluffyProblem(ChatProblem chatProblem) {
        return chatProblem.toProblemDetail();
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    ProblemDetail handleException(Exception ex) {
        ChatProblemMessage chatProblemMessage = new ChatProblemMessage("ex.other.500.unknown");
        return new ChatProblem(chatProblemMessage, ex).toProblemDetail();
    }


    private ResponseEntity<Object> handleConstraintViolation(Exception exception) {
        ChatProblemMessage chatProblemMessage = new ChatProblemMessage("ex.other.400.constraint.violation");
        ProblemDetail problemDetail = new ChatProblem(chatProblemMessage, exception).toProblemDetail();
        return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
    }
}

