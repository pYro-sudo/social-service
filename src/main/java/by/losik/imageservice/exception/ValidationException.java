package by.losik.imageservice.exception;

import java.util.Set;

public class ValidationException extends RuntimeException {
    private final Set<String> errors;

    public ValidationException(String message, Set<String> errors) {
        super(message);
        this.errors = errors;
    }

    public Set<String> getErrors() {
        return errors;
    }
}