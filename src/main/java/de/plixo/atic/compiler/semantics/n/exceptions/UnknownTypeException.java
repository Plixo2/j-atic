package de.plixo.atic.compiler.semantics.n.exceptions;

public class UnknownTypeException extends RegionException {
    final String name;
    public UnknownTypeException(String name, int position) {
        super(name + " is not a known type", position);
        this.name = name;
    }
}
