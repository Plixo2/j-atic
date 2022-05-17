package de.plixo.atic.compiler.semantics.n.exceptions;

public abstract class RegionException extends RuntimeException {
    public final int position;

    public RegionException(String message, int position) {
        super(message + " at [" + position + "]");
        this.position = position;
    }
}
