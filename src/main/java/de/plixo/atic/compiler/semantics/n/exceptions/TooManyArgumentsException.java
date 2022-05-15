package de.plixo.atic.compiler.semantics.n.exceptions;

public class TooManyArgumentsException extends RegionException{
    public TooManyArgumentsException(int position) {
        super("Too many arguments", position);
    }
}
