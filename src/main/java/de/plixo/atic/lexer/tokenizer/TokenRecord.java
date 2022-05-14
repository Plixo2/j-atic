package de.plixo.atic.lexer.tokenizer;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@AllArgsConstructor
public class TokenRecord<T> {
    public final T token;
    public final String data;
    public final int from;
    public final int to;

    @Override
    public String toString() {
        return "TokenRecord{" +
                "token=" + token +
                ", data='" + data + '\'' +
                ", from=" + from +
                ", to=" + to +
                '}';
    }

}
