package de.plixo.lexer.exceptions;

public class FailedTokenCaptureException extends RuntimeException {
    public FailedTokenCaptureException(String message) {
        super(message);
    }
}
