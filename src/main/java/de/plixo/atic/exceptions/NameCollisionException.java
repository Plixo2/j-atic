package de.plixo.atic.exceptions;

public class NameCollisionException extends RuntimeException {
    public NameCollisionException(String message) {
        super(message);
    }
}
