package de.plixo.atic.exceptions.validation;

public class TooMuchArgumentsException extends RuntimeException {
    public TooMuchArgumentsException(String message) {
        super(message);
    }
}
