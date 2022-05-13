package de.plixo.atic.compiler.semantics.n.exceptions;

public class UnexpectedTypeException extends RegionException {
    public UnexpectedTypeException(String message, int position) {
        super(message, position);
    }
}
