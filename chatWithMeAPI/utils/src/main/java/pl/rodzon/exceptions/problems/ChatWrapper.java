package pl.rodzon.exceptions.problems;

import lombok.Getter;

@Getter
public class ChatWrapper extends Exception {
    private final Exception exception;

    public ChatWrapper(Exception exception) {
        this.exception = exception;
    }
}