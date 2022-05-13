package de.plixo.atic.compiler.semantics.n.exceptions;

public class UnresolvedAutoException extends RegionException {
    public UnresolvedAutoException(String message, int position) {
        super(message, position);
    }
}
