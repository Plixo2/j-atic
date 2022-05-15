package de.plixo.atic.compiler.semantics.n.exceptions;

public class MissingArgumentsException extends RegionException{
    public MissingArgumentsException(int position) {
        super("Missing arguments", position);
    }
}
