package de.plixo.lexer.exceptions;

public class UnexpectedTokenException extends RuntimeException {
    public UnexpectedTokenException(String message) {
        super(message);
    }
}
