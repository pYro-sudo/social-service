package by.losik.commentlikeservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }

    public DuplicateResourceException(String resource, String identifier) {
        super(String.format("%s already exists with identifier: %s", resource, identifier),
                HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}