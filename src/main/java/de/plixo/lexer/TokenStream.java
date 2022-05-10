package de.plixo.lexer;

import lombok.Setter;

import java.util.List;

public class TokenStream<T> {
    private final List<T> list;
    @Setter
    protected int index = 0;

    public TokenStream(List<T> list) {
        this.list = list;
    }

    public T current() {
        return list.get(index);
    }

    public int size() {
        return list.size();
    }

    public boolean hasEntriesLeft() {
        return index < list.size();
    }

    public void consume() {
        index += 1;
    }

    public int index() {
        return index;
    }

}
