package de.plixo.atic.compiler.semantics.n.exceptions;

public class NameCollisionException extends RegionException {
    final String name;
    public NameCollisionException(String name, int position) {
        super(name + " is already in use", position);
        this.name = name;
    }
}
