package de.plixo.atic.exceptions.validation;

public class MissingArgumentsException extends RuntimeException {
    public MissingArgumentsException(String message) {
        super(message);
    }
}
